package io.github.oldmanpushcart.jinx.extra.skill.impl;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.skill.Skill;
import io.github.oldmanpushcart.jinx.core.detector.FileDetector;
import io.github.oldmanpushcart.jinx.extra.skill.SkillDetector;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletionStage;

@Singleton
class SkillDetectorImpl extends FileDetector<Skill> implements SkillDetector {

    private final Toolbox toolbox;

    SkillDetectorImpl(Toolbox toolbox) {
        this.toolbox = toolbox;
    }

    @PostConstruct
    void init() {
        detectQuietly("init");
    }

    @Scheduled(fixedDelay = "10s")
    void scan() {
        detectQuietly("scan");
    }

    @Override
    public String toString() {
        return "jinx://skill/detector";
    }

    // ---- FileDetector钩子实现 ----

    @Override
    protected Path directory() {
        return SKILLS_DIR;
    }

    @Override
    protected Path pathOf(String name) {
        return SKILLS_DIR.resolve(name).normalize();
    }

    /**
     * SKILL以目录为来源
     *
     * @param path 路径
     * @return TRUE | FALSE
     */
    @Override
    protected boolean isTarget(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    protected String nameOf(Path path) {
        return path.getFileName().toString();
    }

    /**
     * 从目录中加载出SKILL
     *
     * @param path SKILL目录
     * @return SKILL
     * @throws IOException 加载失败
     */
    @Override
    protected Skill parse(Path path) throws IOException {
        return Skill.of(path);
    }

    @Override
    protected String nameOf(Skill skill) {
        return skill.header().name();
    }

    @Override
    protected Instant versionOf(Path path, Skill skill) {
        return skill.lastModifiedAt();
    }

    /**
     * 激活SKILL：包装为工具并订阅
     *
     * @param name    SKILL名称
     * @param skill   SKILL
     * @param version 版本指纹
     * @return 资源句柄（关闭订阅）
     */
    @Override
    protected CompletionStage<AutoCloseable> activate(String name, Skill skill, Instant version) {
        final var tool = new SkillFunction(skill).asTool();
        return toolbox.subscribeTool("jinx", tool)
                .thenApply(subscription -> (AutoCloseable) subscription);
    }

}
