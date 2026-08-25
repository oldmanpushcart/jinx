package io.github.oldmanpushcart.jinx.extra.cron.impl;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.jinx.core.detector.FileDetector;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
class CronDetectorImpl extends FileDetector<CronMeta> implements CronDetector {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Agent agent;

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
        entries.values().forEach(IOUtils::closeQuietly);
    }

    @Scheduled(fixedDelay = "10s")
    void scan() {
        detectQuietly("scan");
    }

    @Override
    public String toString() {
        return "jinx://cron/detector";
    }

    // ---- FileDetector钩子实现 ----

    @Override
    protected Path directory() {
        return CRON_DIR;
    }

    @Override
    protected Path pathOf(String name) {
        return CRON_DIR.resolve(name + CRON_FILE_SUFFIX);
    }

    /**
     * 是否是定时任务文件
     *
     * @param path 路径
     * @return TRUE | FALSE
     */
    @Override
    protected boolean isTarget(Path path) {
        return Files.isRegularFile(path)
                && path.getFileName().toString().endsWith(CRON_FILE_SUFFIX);
    }

    @Override
    protected String nameOf(Path path) {
        final var filename = path.getFileName().toString();
        return filename.substring(0, filename.length() - CRON_FILE_SUFFIX.length());
    }

    @Override
    protected CronMeta parse(Path path) throws IOException {
        final var json = Files.readString(path, UTF_8);
        return JacksonJsonUtils.toObject(json, CronMeta.class);
    }

    @Override
    protected String nameOf(CronMeta meta) {
        return meta.name();
    }

    @Override
    protected Instant versionOf(Path path, CronMeta meta) throws IOException {
        return Files.getLastModifiedTime(path).toInstant();
    }

    /**
     * 激活定时任务：启用且有下次触发点时挂单次调度，否则不携带调度。
     * <p>
     * 激活失败（如非法的cron表达式）如实返回失败阶段，
     * 由{@link FileDetector}框架统一降级注册并记录失败原因。
     * </p>
     *
     * @param name    任务名称
     * @param meta    任务元数据
     * @param version 版本指纹
     * @return 资源句柄（取消调度）
     */
    @Override
    protected CompletionStage<AutoCloseable> activate(String name, CronMeta meta, Instant version) {
        if (!meta.enabled()) {
            return CompletableFuture.completedFuture(null);
        }
        final var future = scheduleNext(meta, version, CronExpression.create(meta.cron()), Instant.now());
        return CompletableFuture.completedFuture(cancelable(future));
    }

    // ---- 调度引擎：单次调度 + 自续期 ----

    /**
     * 包装调度句柄：关闭即取消调度
     */
    private static AutoCloseable cancelable(ScheduledFuture<?> future) {
        if (null == future) {
            return null;
        }
        return () -> future.cancel(false);
    }

    private void fire(Entry<CronMeta> snapshot) {

        // 任务已被删除或替换，不再执行与续期（防复活）
        if (!isCurrent(snapshot)) {
            return;
        }

        final var meta = snapshot.item();
        logger.info("{} executing cron task: name={}, sessionId={}", this, meta.name(), meta.sessionId());

        final var cronExpr = CronExpression.create(meta.cron());

        // FIXED 模式：触发后立即按触发时刻续期，不等待执行完成（允许并行）
        if (CronMeta.Mode.FIXED == meta.mode()) {
            renew(snapshot, cronExpr, Instant.now());
        }

        // 触发即走：只关心执行完成与否，不关心输出；调度线程不阻塞等待。
        // DELAY 模式：执行完成（或失败）后，按完成时刻续期下一次触发。
        Flux.from(agent.flow(meta.sessionId(), Message.user(meta.prompt())))
                .timeout(Duration.ofMinutes(5))
                .subscribe(
                        msg -> {
                        },
                        error -> {
                            logger.warn("{} cron task failed: name={}", this, meta.name(), error);
                            renewIfDelay(snapshot, cronExpr);
                        },
                        () -> {
                            logger.info("{} cron task completed: name={}", this, meta.name());
                            renewIfDelay(snapshot, cronExpr);
                        }
                );
    }

    private void renewIfDelay(Entry<CronMeta> snapshot, CronExpression cronExpr) {
        if (CronMeta.Mode.DELAY == snapshot.item().mode()) {
            renew(snapshot, cronExpr, Instant.now());
        }
    }

    /**
     * 防复活校验：任务被替换时必然伴随 version 或 meta 变化（不变式）。
     */
    private boolean isCurrent(Entry<CronMeta> snapshot) {
        final var current = entries.get(snapshot.name());
        return null != current
                && Objects.equals(snapshot.version(), current.version())
                && snapshot.item().equals(current.item());
    }

    /**
     * 原子续期：校验 + 调度 + 替换在 compute 内完成，无中途被替换的竞态。
     */
    private void renew(Entry<CronMeta> snapshot, CronExpression cronExpr, Instant from) {
        entries.compute(snapshot.name(), (name, current) -> {

            // 已被删除或替换，放弃续期
            if (null == current
                    || !Objects.equals(snapshot.version(), current.version())
                    || !snapshot.item().equals(current.item())) {
                return current;
            }

            final var future = scheduleNext(snapshot.item(), snapshot.version(), cronExpr, from);
            if (null == future) {
                return current;   // 永久过期，停止调度；任务保留供查询展示
            }

            IOUtils.closeQuietly(current);   // 旧任务已触发完成，关闭为空操作
            return entryOf(name, snapshot.item(), snapshot.version(), cancelable(future));
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
        return scheduler.schedule(() -> fire(entryOf(meta.name(), meta, version, null)), delay, TimeUnit.MILLISECONDS);
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

}
