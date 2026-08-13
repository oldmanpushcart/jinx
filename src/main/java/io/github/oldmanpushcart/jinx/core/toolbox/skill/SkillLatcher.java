package io.github.oldmanpushcart.jinx.core.toolbox.skill;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.skill.SkillsToolSource;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.jinx.JinxConfig;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Context
public class SkillLatcher {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JinxConfig config;
    private final Toolbox toolbox;
    private final Set<AutoCloseable> autoCloseableSet = ConcurrentHashMap.newKeySet();

    public SkillLatcher(JinxConfig config, Toolbox toolbox) {
        this.config = config;
        this.toolbox = toolbox;
    }

    @PostConstruct
    void init() {

        final var directory = config.dataspace().resolve("skills");

        final var source = SkillsToolSource.newBuilder()
                .namespace("dashscope4j")
                .directory(directory)
                .scanInterval(Duration.ofSeconds(3))
                .build();

        autoCloseableSet.add(source);

        source.initialize()
                .thenCompose(toolbox::subscribe)
                .toCompletableFuture()
                .join();

        logger.debug("jinx://toolbox/skills enabled. directory={}", directory);

    }

    @PreDestroy
    void destroy() {
        autoCloseableSet.forEach(IOUtils::closeQuietly);
    }

}
