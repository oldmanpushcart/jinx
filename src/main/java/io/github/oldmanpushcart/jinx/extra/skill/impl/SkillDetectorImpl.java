package io.github.oldmanpushcart.jinx.extra.skill.impl;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.ToolSubscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.skill.Skill;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.jinx.extra.skill.SkillDetector;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
class SkillDetectorImpl implements SkillDetector {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Toolbox toolbox;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    SkillDetectorImpl(Toolbox toolbox) {
        this.toolbox = toolbox;
    }

    @PostConstruct
    void init() {
        try {
            detect();
        } catch (IOException e) {
            logger.warn("{}/init detect ignored by error!", this, e);
        }
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
        return "jinx://skill/detector";
    }

    @Override
    public List<Skill> list() {
        return entries.values()
                .stream()
                .map(Entry::skill)
                .toList();
    }

    @Override
    public Optional<Skill> get(String name) {
        return Optional.ofNullable(entries.get(name))
                .map(Entry::skill);
    }

    @Override
    public CompletionStage<Skill> reload(String name) {
        final var home = SKILLS_DIR
                .resolve(name)
                .normalize();
        if (!Files.exists(home)) {
            return CompletableFuture.failedStage(new RuntimeException("SKILL: %s not exist!".formatted(name)));
        }
        return reload(home)
                .thenApply(Entry::skill);
    }

    @Override
    public Skill remove(String name) {
        final var removed = entries.remove(name);
        if (null != removed) {
            IOUtils.closeQuietly(removed);
            return removed.skill();
        } else {
            return null;
        }
    }

    private CompletionStage<Entry> reload(Path home) {
        final var name = home.getFileName().toString();
        return CompletableFuture.completedStage(null)
                .thenCompose(_u -> {

                    try {

                        // 从目录中加载出SKILL
                        final var skill = Skill.of(home);

                        // SKILL名称必须和目录名保持一致
                        if (!Objects.equals(name, skill.header().name())) {
                            throw new RuntimeException("Skill name mismatch! expect: %s, actual: %s".formatted(
                                    name,
                                    skill.header().name()
                            ));
                        }

                        /*
                         * 检查版本是否和已加载的SKILL一致
                         * 如果一致则用已加载的
                         */
                        final var exist = entries.get(name);
                        if (null != exist && Objects.equals(exist.version(), skill.lastModifiedAt())) {
                            return CompletableFuture.completedStage(exist);
                        }

                        final var tool = new SkillFunction(skill).asTool();
                        return toolbox.subscribeTool("jinx", tool)
                                .thenApply(subscription -> {
                                    final var entry = new Entry(name, skill, skill.lastModifiedAt(), subscription);
                                    final var expired = entries.put(name, entry);
                                    if (null != expired) {
                                        IOUtils.closeQuietly(expired);
                                    }
                                    return entry;
                                })
                                .whenComplete((uu, ex) -> {
                                    if (null != ex) {
                                        logger.warn("{} register skill failed by reload! name={};", this, skill.header().name(), ex);
                                    } else {
                                        logger.debug("{} register skill success by reload. name={};", this, skill.header().name());
                                    }
                                });

                    } catch (Exception ex) {
                        return CompletableFuture.failedStage(new RuntimeException("Load skill: %s failed!".formatted(name), ex));
                    }

                });
    }


    private synchronized void detect() throws IOException {

        final var directory = SKILLS_DIR;
        if (!Files.isDirectory(directory)) {
            logger.warn("{} ignored by directory not exist. path={}", this, directory);
            return;
        }

        final var removes = new ArrayList<>(entries.keySet());
        try (final var __stream__ = Files.list(directory)) {
            __stream__
                    .filter(Files::isDirectory)
                    .forEach(home -> {
                        try {
                            reload(home)
                                    .thenAccept(entry -> removes.remove(entry.name()))
                                    .toCompletableFuture()
                                    .join();
                        } catch (Exception ex) {
                            final var cause = CompletableFutureUtils.unwrapEx(ex);
                            logger.warn("{} detect ignored. path={}", this, home, cause);
                        }
                    });
        }

        removes.forEach(this::remove);

    }

    private record Entry(
            String name,
            Skill skill,
            Instant version,
            ToolSubscription subscription
    ) implements AutoCloseable {

        @Override
        public void close() {
            IOUtils.closeQuietly(subscription());
        }

    }

}
