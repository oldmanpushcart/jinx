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

    private static final String CRON_FILE_SUFFIX = ".cron.json";

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
        try {
            detect();
        } catch (IOException e) {
            logger.warn("{}/init detect ignored by error!", this, e);
        }
    }

    @PreDestroy
    void destroy() {
        scheduler.shutdownNow();
        tasks.values().forEach(IOUtils::closeQuietly);
    }

    @Scheduled(fixedDelay = "10s")
    void scan() {
        try {
            detect();
        } catch (IOException e) {
            logger.warn("{}/scan detect ignored by error!", this, e);
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
    public CronMeta create(CronMeta meta) {
        try {
            final var json = JacksonJsonUtils.toJson(meta);
            final var path = CRON_DIR.resolve(meta.name() + CRON_FILE_SUFFIX);
            Files.writeString(path, json, UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Write cron file failed: %s".formatted(meta.name()), e);
        }
        return reload(meta.name());
    }

    @Override
    public CronMeta reload(String name) {
        final var path = CRON_DIR.resolve(name + CRON_FILE_SUFFIX);
        if (!Files.exists(path)) {
            return null;
        }
        return reload(path);
    }

    @Override
    public CronMeta remove(String name) {
        final var removed = removeTask(name);
        if (null != removed) {
            // 删除磁盘文件
            try {
                Files.deleteIfExists(CRON_DIR.resolve(name + CRON_FILE_SUFFIX));
            } catch (IOException e) {
                logger.warn("{} delete cron file failed. name={}", this, name, e);
            }
            return removed.meta();
        }
        return null;
    }

    @Override
    public CronMeta pause(String name) {
        final var task = tasks.get(name);
        if (null != task) {
            // 更新文件 enabled=false
            final var updated = new CronMeta(task.meta().name(), task.meta().cron(), task.meta().prompt(), false, task.meta().mode());
            writeMeta(updated);
            putTask(new CronTask(updated, task.version(), null));
            return updated;
        }
        return null;
    }

    @Override
    public CronMeta resume(String name) {
        final var task = tasks.get(name);
        if (null != task) {
            final var updated = new CronMeta(task.meta().name(), task.meta().cron(), task.meta().prompt(), true, task.meta().mode());
            writeMeta(updated);
            return reload(updated.name());
        }
        return null;
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
                            final var meta = reload(cronPath);
                            removes.remove(meta.name());
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

    private CronMeta reload(Path cronPath) {
        final var filename = cronPath.getFileName().toString();
        final var name = filename.substring(0, filename.length() - CRON_FILE_SUFFIX.length());

        try {
            // 版本检查
            final var version = Files.getLastModifiedTime(cronPath).toInstant();
            final var exist = tasks.get(name);
            if (null != exist && Objects.equals(exist.version(), version)) {
                return exist.meta();
            }

            // 解析文件
            final var json = Files.readString(cronPath, UTF_8);
            final var meta = JacksonJsonUtils.toObject(json, CronMeta.class);

            // 名称校验
            if (!Objects.equals(name, meta.name())) {
                throw new RuntimeException("Cron name mismatch! expect: %s, actual: %s".formatted(
                        name,
                        meta.name()
                ));
            }

            // 创建新任务（逐出旧任务时自动关闭），启用则调度
            putTask(createTask(meta, version));
            return meta;

        } catch (IOException e) {
            throw new UncheckedIOException("Reload cron file failed: %s".formatted(cronPath), e);
        }
    }

    // ---- 任务生命周期：tasks 的唯一写入口 ----

    /**
     * 放入任务：被逐出的旧任务一律关闭（已完成则关闭为空操作）。
     */
    private void putTask(CronTask task) {
        final var expired = tasks.put(task.meta().name(), task);
        if (null != expired) {
            IOUtils.closeQuietly(expired);
        }
    }

    /**
     * 逐出任务：逐出即关闭。
     */
    private CronTask removeTask(String name) {
        final var removed = tasks.remove(name);
        if (null != removed) {
            IOUtils.closeQuietly(removed);
        }
        return removed;
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
            final var cronExpr = CronExpression.create(meta.cron());
            final var next = nextTimeAfter(cronExpr, Instant.now());
            if (null == next) {
                logger.info("{} cron task expired permanently, stop scheduling: name={}", this, meta.name());
                return new CronTask(meta, version, null);
            }
            final var delay = Math.max(Duration.between(Instant.now(), next).toMillis(), 0);
            final var future = scheduler.schedule(
                    () -> fire(meta, version, cronExpr),
                    delay,
                    TimeUnit.MILLISECONDS
            );
            logger.debug("{} scheduled cron task: name={}, cron={}, mode={}, nextRun={}",
                    this, meta.name(), meta.cron(), meta.mode(), next);
            return new CronTask(meta, version, future);
        } catch (Exception e) {
            logger.warn("{} failed to schedule cron task: name={}", this, meta.name(), e);
            return new CronTask(meta, version, null);
        }
    }

    private void fire(CronMeta meta, Instant version, CronExpression cronExpr) {

        // 任务已被删除或替换，不再执行与续期（防复活）
        if (!isCurrent(meta, version)) {
            return;
        }

        logger.info("{} executing cron task: name={}", this, meta.name());

        // FIXED 模式：触发后立即按触发时刻续期，不等待执行完成（允许并行）
        if (CronMeta.Mode.FIXED == meta.mode()) {
            renew(meta, version, cronExpr, Instant.now());
        }

        final var sessionId = "cron@%s".formatted(meta.name());
        final var inbound = Message.user(meta.prompt());

        // 触发即走：只关心执行完成与否，不关心输出；调度线程不阻塞等待
        Flux.from(agent.flow(sessionId, inbound))
                .timeout(Duration.ofMinutes(5))
                .subscribe(
                        msg -> {
                        },
                        error -> {
                            logger.warn("{} cron task failed: name={}", this, meta.name(), error);
                            renewIfDelay(meta, version, cronExpr);
                        },
                        () -> {
                            logger.info("{} cron task completed: name={}", this, meta.name());
                            renewIfDelay(meta, version, cronExpr);
                        }
                );
    }

    /**
     * DELAY 模式：执行完成（或失败）后，按完成时刻续期下一次触发。
     */
    private void renewIfDelay(CronMeta meta, Instant version, CronExpression cronExpr) {
        if (CronMeta.Mode.DELAY == meta.mode()) {
            renew(meta, version, cronExpr, Instant.now());
        }
    }

    /**
     * 防复活校验：任务被替换时必然伴随 version 或 meta 变化（不变式）。
     */
    private boolean isCurrent(CronMeta meta, Instant version) {
        final var current = tasks.get(meta.name());
        return null != current
                && Objects.equals(current.version(), version)
                && current.meta().equals(meta);
    }

    /**
     * 原子续期：校验 + 调度 + 替换在 compute 内完成，无中途被替换的竞态。
     */
    private void renew(CronMeta meta, Instant version, CronExpression cronExpr, Instant from) {
        tasks.compute(meta.name(), (name, current) -> {
            if (null == current || !Objects.equals(current.version(), version) || !current.meta().equals(meta)) {
                return current;   // 已被删除或替换，放弃续期
            }
            final var next = nextTimeAfter(cronExpr, from);
            if (null == next) {
                logger.info("{} cron task expired permanently, stop scheduling: name={}", this, meta.name());
                return current;
            }
            final var delay = Math.max(Duration.between(Instant.now(), next).toMillis(), 0);
            final var future = scheduler.schedule(
                    () -> fire(meta, version, cronExpr),
                    delay,
                    TimeUnit.MILLISECONDS
            );
            logger.debug("{} scheduled cron task: name={}, cron={}, mode={}, nextRun={}",
                    this, meta.name(), meta.cron(), meta.mode(), next);
            IOUtils.closeQuietly(current);   // 旧任务已触发完成，关闭为空操作
            return new CronTask(meta, version, future);
        });
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

    private void writeMeta(CronMeta meta) {
        try {
            final var json = JacksonJsonUtils.toJson(meta);
            final var path = CRON_DIR.resolve(meta.name() + CRON_FILE_SUFFIX);
            Files.writeString(path, json, UTF_8);
        } catch (IOException e) {
            logger.warn("{} write cron meta failed: name={}", this, meta.name(), e);
        }
    }

    // ---- 内部记录 ----

    /**
     * 定时任务的运行时调度载体（{@link CronMeta} 为持久化定义）。
     */
    record CronTask(CronMeta meta, Instant version, ScheduledFuture<?> future) implements AutoCloseable {

        @Override
        public void close() {
            if (null != future) {
                future.cancel(false);
            }
        }

    }

}
