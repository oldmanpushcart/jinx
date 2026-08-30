package io.github.oldmanpushcart.jinx.core.detector;

import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件探测器
 * <p>
 * 基于文件检索的{@link Detector}：扫描目标目录{@link #directory()}，
 * 将命中{@link #isTarget(Path)}的来源解析成对象并注册，来源消失的条目会被移除。
 * </p>
 * <p>
 * 重加载流程：解析 → 版本比对（未变化则跳过）→ 名称一致性校验 → 激活注册。
 * </p>
 * <p>
 * 激活失败采用降级注册：条目携带失败原因照常注册，保留可见性；
 * 失败条目在周期扫描中每轮重试（串行阻塞扫描由调度自然限频，不形成风暴），实现自愈；
 * 显式重载{@link #reload(String)}强制立即重新激活并向调用方如实反馈失败原因。
 * </p>
 *
 * @param <T> 探测对象类型
 */
public abstract class FileDetector<T> implements Detector<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 注册表：名称 -> 条目
     */
    protected final Map<String, Entry<T>> entries = new ConcurrentHashMap<>();

    @Override
    public List<T> list() {
        return entries.values()
                .stream()
                .map(Entry::item)
                .toList();
    }

    @Override
    public Optional<T> get(String name) {
        return Optional.ofNullable(entries.get(name))
                .map(Entry::item);
    }

    @Override
    public CompletionStage<T> reload(String name) {
        final var path = pathOf(name);
        if (!Files.exists(path)) {
            return CompletableFuture.failedFuture(new IOException("Detector target not exist! name=%s; path=%s".formatted(
                    name,
                    path
            )));
        }

        /*
         * 显式重载：强制重新激活（绕过版本短路），
         * 直接依据本次激活产生的条目向调用方如实反馈失败原因。
         */
        return reload(path, true)
                .thenCompose(entry -> {
                    if (entry.isFailed()) {
                        return CompletableFuture.failedFuture(entry.failure());
                    }
                    return CompletableFuture.completedFuture(entry.item());
                });
    }

    /**
     * 启动探测：Bean初始化后立即检测一次，由框架统一提供，子类无需重复声明。
     */
    @PostConstruct
    protected void init() {
        detectQuietly("init");
    }

    /**
     * 周期探测：固定10秒扫描一次，承担失效条目移除与降级条目的重试自愈，
     * 由框架统一提供，子类无需重复声明。
     */
    @Scheduled(fixedDelay = "10s")
    @ReflectiveAccess
    protected void scan() {
        detectQuietly("scan");
    }

    /**
     * 检测一次：扫描目录并逐一重加载命中的来源，最后移除已失效的条目
     *
     * @throws IOException 检测失败
     */
    protected synchronized void detect() throws IOException {

        final var directory = directory();
        if (!Files.isDirectory(directory)) {
            logger.warn("{} ignored by directory not exist. path={}", this, directory);
            return;
        }

        final var removes = new ArrayList<>(entries.keySet());

        /*
         * 阻塞遍历目录，找出所有命中的来源，逐一加载。
         */
        try (final var __stream__ = Files.list(directory)) {
            __stream__
                    .filter(this::isTarget)
                    .forEach(path -> {
                        final var name = nameOf(path);
                        try {
                            reload(path)
                                    .thenAccept(_entry -> removes.remove(name))
                                    .toCompletableFuture()
                                    .join();
                        } catch (Exception ex) {
                            final var cause = CompletableFutureUtils.unwrapEx(ex);
                            logger.warn("{} detect ignored. path={}", this, path, cause);
                        }
                    });
        }

        // 移除已失效的条目
        removes.forEach(this::remove);

    }

    /**
     * 检测一次，忽略发生的错误
     *
     * @param action 动作标识（仅用于日志）
     */
    protected final void detectQuietly(String action) {
        try {
            detect();
        } catch (IOException e) {
            logger.warn("{}/{} detect ignored by error!", this, action, e);
        }
    }

    /**
     * 从指定来源重加载对象（周期探测路径：版本短路，失败条目下轮重试）
     *
     * @param path 来源路径
     * @return 重加载后的条目
     */
    protected CompletionStage<Entry<T>> reload(Path path) {
        return reload(path, false);
    }

    /**
     * 从指定来源重加载对象
     *
     * @param path  来源路径
     * @param force TRUE时绕过版本短路强制重新激活（用于显式重载的重试语义）
     * @return 重加载后的条目（含激活结果与失败原因）
     */
    private CompletionStage<Entry<T>> reload(Path path, boolean force) {
        return CompletableFuture.completedFuture(null)
                .thenCompose(_u -> {

                    try {

                        final var name = nameOf(path);
                        final var item = parse(path);
                        final var version = versionOf(path, item);

                        /*
                         * 检查已注册的版本和当前版本是否一致，如果一致就不用重新加载了。
                         * 例外：强制重载，或已注册条目处于激活失败状态（版本短路会让失败永远无法被重试）。
                         */
                        final var exist = entries.get(name);
                        if (!force && null != exist && !exist.isFailed() && Objects.equals(exist.version(), version)) {
                            return CompletableFuture.completedFuture(exist);
                        }

                        /*
                         * 检查名称是否和期望的一致
                         * 要求从来源路径提取的名称必须和从对象提取的名称一致
                         */
                        if (!Objects.equals(name, nameOf(item))) {
                            return CompletableFuture.failedFuture(new RuntimeException("Name mismatch! expect: %s, actual: %s".formatted(
                                    name,
                                    nameOf(item)
                            )));
                        }

                        /*
                         * 激活注册：失败时降级注册（条目携带失败原因照常注册），
                         * 条目保留可见性，由周期扫描每轮重试直至激活成功。
                         * completedFuture(null).thenCompose 将子类同步抛出的异常统一转为失败阶段。
                         */
                        return CompletableFuture.completedFuture(null)
                                .thenCompose(_v -> activate(name, item, version))
                                .handle((resource, ex) -> {
                                    final var cause = null == ex ? null : CompletableFutureUtils.unwrapEx(ex);
                                    final var entry = new Entry<>(name, item, version, null == ex ? resource : null, cause);
                                    final var expired = entries.put(name, entry);
                                    if (null != expired) {
                                        IOUtils.closeQuietly(expired);
                                    }

                                    /*
                                     * 日志按状态转移收敛：失败条目每轮重试，
                                     * 相同失败的重复告警降为debug，避免刷屏。
                                     */
                                    if (entry.isFailed()) {
                                        if (entry.sameFailureAs(expired)) {
                                            logger.debug("{} activate still failed. name={};", this, name, entry.failure());
                                        } else {
                                            logger.warn("{} activate failed, registered as degraded. name={};", this, name, entry.failure());
                                        }
                                    } else if (null != expired && expired.isFailed()) {
                                        logger.info("{} activate recovered from failure. name={};", this, name);
                                    } else {
                                        logger.debug("{} register success by reload. name={};", this, name);
                                    }

                                    return entry;
                                });

                    } catch (Exception ex) {
                        return CompletableFuture.failedFuture(ex);
                    }

                });
    }

    /**
     * 移除指定名称的条目（关闭被逐出的条目）
     *
     * @param name 名称
     * @return 被移除的对象，不存在时返回 null
     */
    protected T remove(String name) {
        final var removed = entries.remove(name);
        if (null != removed) {
            IOUtils.closeQuietly(removed);
            return removed.item();
        }
        return null;
    }

    /**
     * @return 扫描目录
     */
    protected abstract Path directory();

    /**
     * 获取名称对应的来源路径
     *
     * @param name 名称
     * @return 来源路径
     */
    protected abstract Path pathOf(String name);

    /**
     * 判断路径是否是探测目标
     *
     * @param path 路径
     * @return TRUE | FALSE
     */
    protected abstract boolean isTarget(Path path);

    /**
     * 从路径提取名称
     *
     * @param path 路径
     * @return 名称
     */
    protected abstract String nameOf(Path path);

    /**
     * 从路径解析出对象
     *
     * @param path 来源路径
     * @return 对象
     * @throws IOException 解析失败
     */
    protected abstract T parse(Path path) throws IOException;

    /**
     * 从对象提取名称
     *
     * @param item 对象
     * @return 名称
     */
    protected abstract String nameOf(T item);

    /**
     * 计算版本指纹（与已注册版本一致时跳过重加载）
     *
     * @param path 来源路径
     * @param item 对象
     * @return 版本指纹
     * @throws IOException 版本计算失败
     */
    protected abstract Instant versionOf(Path path, T item) throws IOException;

    /**
     * 激活对象：完成资源注册并返回该资源的可关闭句柄
     * <p>
     * 激活失败时返回失败阶段即可，由框架统一降级注册并记录失败原因；
     * 子类若已自行创建资源，需在失败前清理，避免资源泄漏。
     * </p>
     *
     * @param name    名称
     * @param item    对象
     * @param version 版本指纹
     * @return 资源句柄
     */
    protected abstract CompletionStage<AutoCloseable> activate(String name, T item, Instant version);

    /**
     * 构造注册条目（供子类构造入口）
     *
     * @param name     名称
     * @param item     对象
     * @param version  版本指纹
     * @param resource 资源句柄（条目被逐出时关闭）
     * @return 注册条目
     */
    protected static <T> Entry<T> entryOf(String name, T item, Instant version, AutoCloseable resource) {
        return new Entry<>(name, item, version, resource, null);
    }

    /**
     * 注册条目
     *
     * @param name     名称
     * @param item     对象
     * @param version  版本指纹
     * @param resource 资源句柄（条目被逐出时关闭）
     * @param failure  激活失败原因（激活成功时为 null）
     */
    protected record Entry<T>(
            String name,
            T item,
            Instant version,
            AutoCloseable resource,
            Throwable failure
    ) implements AutoCloseable {

        /**
         * @return TRUE|FALSE：激活是否失败
         */
        public boolean isFailed() {
            return null != failure;
        }

        /**
         * 判断与另一条目的激活失败原因是否相同
         *
         * @param other 另一条目（允许为 null）
         * @return TRUE|FALSE
         */
        public boolean sameFailureAs(Entry<?> other) {
            return null != other
                    && Objects.equals(String.valueOf(failure), String.valueOf(other.failure));
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(resource);
        }

    }

}
