package io.github.oldmanpushcart.jinx.extra.cron.impl;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.jinx.extra.cron.CronDetector;
import io.github.oldmanpushcart.jinx.extra.cron.CronMeta;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.scheduling.cron.CronExpression;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
class CronDetectorImpl implements CronDetector {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Agent agent;

    private final Map<String, CronTask> tasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        final var t = new Thread(r, "jinx://cron/scheduler");
        t.setDaemon(true);
        return t;
    });

    public CronDetectorImpl(Agent agent) {
        this.agent = agent;
    }

    @PostConstruct
    void init() {
        detectQuietly("init");
    }

    @PreDestroy
    void destroy() {
        scheduler.shutdownNow();
        tasks.values().forEach(IOUtils::closeQuietly);
    }

    @Scheduled(fixedDelay = "10s")
    void scan() {
        detectQuietly("scan");
    }

    private void detectQuietly(String action) {
        try {
            detect();
        } catch (IOException e) {
            logger.warn("{}/{} detect ignored by error!", this, action, e);
        }
    }

    @Override
    public String toString() {
        return "jinx://cron/detector";
    }

    @Override
    public List<CronMeta> list() {
        return tasks.values().stream()
                .map(CronTask::meta)
                .toList();
    }

    @Override
    public Optional<CronMeta> get(String name) {
        return Optional.ofNullable(tasks.get(name))
                .map(CronTask::meta);
    }

    @Override
    public CronMeta reload(String name) {
        final var path = CRON_DIR.resolve(name + CRON_FILE_SUFFIX);
        if (!Files.exists(path)) {
            throw new RuntimeException("Cron: %s not exist!".formatted(name));
        }
        return reload(path).meta();
    }

    // ---- 文件检测 ----
    private synchronized void detect() throws IOException {

        final var directory = CRON_DIR;
        if (!Files.isDirectory(directory)) {
            return;
        }

        final var removes = new ArrayList<>(tasks.keySet());
        try (final var __stream__ = Files.list(directory)) {
            __stream__
                    .filter(Files::isRegularFile)
                    .filter(CronDetectorImpl::isCronFile)
                    .forEach(cronPath -> {
                        try {
                            removes.remove(reload(cronPath).name());
                        } catch (Exception ex) {
                            logger.warn("{} detect ignored. path={}", this, cronPath, ex);
                        }
                    });
        }

        removes.forEach(this::removeTask);
    }

    private static boolean isCronFile(Path path) {
        return path.getFileName().toString().endsWith(CRON_FILE_SUFFIX);
    }

    private CronTask reload(Path path) {
        final var filename = path.getFileName().toString();
        final var name = filename.substring(0, filename.length() - CRON_FILE_SUFFIX.length());

        try {

            // 版本检查
            final var version = Files.getLastModifiedTime(path).toInstant();
            final var exist = tasks.get(name);
            if (null != exist && Objects.equals(exist.version(), version)) {
                return exist;
            }

            // 解析文件
            final var json = Files.readString(path, UTF_8);
            final var meta = JacksonJsonUtils.toObject(json, CronMeta.class);

            // 名称校验
            if (!Objects.equals(name, meta.name())) {
                throw new RuntimeException("Cron name mismatch! expect: %s, actual: %s".formatted(
                        name,
                        meta.name()
                ));
            }

            // 创建新任务（逐出旧任务时自动关闭），启用则调度
            final var task = createTask(meta, version);
            putTask(task);
            return task;

        } catch (IOException e) {
            throw new UncheckedIOException("Reload cron file failed: %s".formatted(path), e);
        }

    }

    // ---- 任务生命周期：tasks 的唯一写入口 ----

    /**
     * 放入任务：被逐出的旧任务一律关闭（已完成则关闭为空操作）。
     */
    private void putTask(CronTask task) {
        IOUtils.closeQuietly(tasks.put(task.name(), task));
    }

    /**
     * 逐出任务：逐出即关闭。
     */
    private void removeTask(String name) {
        IOUtils.closeQuietly(tasks.remove(name));
    }

    // ---- 调度引擎：单次调度 + 自续期 ----

    /**
     * 创建任务：启用且有下次触发点时挂单次调度，否则不携带调度。
     */
    private CronTask createTask(CronMeta meta, Instant version) {
        if (!meta.enabled()) {
            return new CronTask(meta, version, null);
        }
        try {
            final var future = scheduleNext(meta, version, CronExpression.create(meta.cron()), Instant.now());
            return new CronTask(meta, version, future);
        } catch (Exception e) {
            logger.warn("{} failed to schedule cron task: name={}", this, meta.name(), e);
            return new CronTask(meta, version, null);
        }
    }

    private void fire(CronTask task) {

        // 任务已被删除或替换，不再执行与续期（防复活）
        if (!isCurrent(task)) {
            return;
        }

        final var meta = task.meta();
        logger.info("{} executing cron task: name={}", this, meta.name());

        final var cronExpr = CronExpression.create(meta.cron());

        // FIXED 模式：触发后立即按触发时刻续期，不等待执行完成（允许并行）
        if (CronMeta.Mode.FIXED == meta.mode()) {
            renew(task, cronExpr, Instant.now());
        }

        // 触发即走：只关心执行完成与否，不关心输出；调度线程不阻塞等待。
        // DELAY 模式：执行完成（或失败）后，按完成时刻续期下一次触发。
        Flux.from(agent.flow("cron@%s".formatted(meta.name()), Message.user(meta.prompt())))
                .timeout(Duration.ofMinutes(5))
                .subscribe(
                        msg -> {
                        },
                        error -> {
                            logger.warn("{} cron task failed: name={}", this, meta.name(), error);
                            renewIfDelay(task, cronExpr);
                        },
                        () -> {
                            logger.info("{} cron task completed: name={}", this, meta.name());
                            renewIfDelay(task, cronExpr);
                        }
                );
    }

    private void renewIfDelay(CronTask task, CronExpression cronExpr) {
        if (CronMeta.Mode.DELAY == task.meta().mode()) {
            renew(task, cronExpr, Instant.now());
        }
    }

    /**
     * 防复活校验：任务被替换时必然伴随 version 或 meta 变化（不变式）。
     */
    private boolean isCurrent(CronTask task) {
        return task.isCurrent(tasks.get(task.name()));
    }

    /**
     * 原子续期：校验 + 调度 + 替换在 compute 内完成，无中途被替换的竞态。
     */
    private void renew(CronTask task, CronExpression cronExpr, Instant from) {
        tasks.compute(task.name(), (name, current) -> {

            // 已被删除或替换，放弃续期
            if (!task.isCurrent(current)) {
                return current;
            }
            final var future = scheduleNext(task.meta(), task.version(), cronExpr, from);
            if (null == future) {
                return current;   // 永久过期，停止调度；任务保留供查询展示
            }
            IOUtils.closeQuietly(current);   // 旧任务已触发完成，关闭为空操作
            return new CronTask(task.meta(), task.version(), future);
        });
    }

    /**
     * 挂下一次单次调度；无下次触发点（永久过期）时返回 null。
     */
    private ScheduledFuture<?> scheduleNext(CronMeta meta, Instant version, CronExpression cronExpr, Instant from) {

        // 计算下次执行时间
        final var afterAt = nextTimeAfter(cronExpr, from);
        if (null == afterAt) {
            logger.info("{} cron task expired permanently, stop scheduling: name={}", this, meta.name());
            return null;
        }

        // 计算从现在到下次执行之间的时间间隔，作为调度延迟
        final var delay = Math.max(Duration.between(Instant.now(), afterAt).toMillis(), 0);

        logger.debug("{} scheduled cron task: name={}, cron={}, mode={}, afterAt={}",
                this,
                meta.name(),
                meta.cron(),
                meta.mode(),
                afterAt
        );
        return scheduler.schedule(() -> fire(new CronTask(meta, version, null)), delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 计算 from 之后的下一个触发点；不存在时返回 null（视为永久过期）。
     */
    private static ZonedDateTime nextTimeAfter(CronExpression cronExpr, Instant from) {
        try {
            return cronExpr.nextTimeAfter(ZonedDateTime.ofInstant(from, ZoneId.systemDefault()));
        } catch (IllegalArgumentException e) {
            // 搜索期限内无下次触发点（如一次性已过期表达式），视为无下次触发
            return null;
        }
    }

    // ---- 内部记录 ----

    /**
     * 定时任务的运行时调度载体（{@link CronMeta} 为持久化定义）。
     */
    record CronTask(CronMeta meta, Instant version, ScheduledFuture<?> future) implements AutoCloseable {

        public CronTask(CronMeta meta, Instant version) {
            this(meta, version, null);
        }

        public String name() {
            return meta.name();
        }

        /**
         * 是否当前注册的任务（防复活校验的单一实现）。
         */
        public boolean isCurrent(CronTask other) {
            return null != other
                    && Objects.equals(version, other.version)
                    && meta.equals(other.meta);
        }

        @Override
        public void close() {
            if (null != future) {
                future.cancel(false);
            }
        }

    }

}
