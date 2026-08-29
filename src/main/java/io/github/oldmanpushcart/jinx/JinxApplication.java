package io.github.oldmanpushcart.jinx;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.micronaut.context.annotation.Context;
import io.micronaut.runtime.Micronaut;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Context
public class JinxApplication implements AutoCloseable {

    private static void setSystemProperty(String key, String def) {
        System.setProperty(key, System.getProperty(key, def));
    }

    static {
        setSystemProperty("logback.configurationFile", "conf/logback.xml");
        setSystemProperty("micronaut.config.files", "conf/application.yml,conf/jinx.yml");
    }

    private static final Logger logger = LoggerFactory.getLogger(JinxApplication.class);

    public static void main(String[] args) {
        try {

            // 初始化目录
            List.of(
                    Constants.CONF,
                    Constants.DATA,
                    Constants.DATA.resolve("prompts/preparation"),
                    Constants.DATA.resolve("prompts/interaction"),
                    Constants.LOGS,
                    Constants.WORK
            ).forEach(path -> {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    throw new RuntimeException("Init directory: %s failed!".formatted(path), e);
                }
            });

            logger.info("jinx://app starting...");
            logger.info("jinx://app logback config: {}", System.getProperty("logback.configurationFile"));
            logger.info("jinx://app micronaut config: {}", System.getProperty("micronaut.config.files"));
            final var context = Micronaut
                    .run(JinxApplication.class, args)
                    .start();
            logger.info("jinx://app started.");

            context.getBean(Agent.class);

        } catch (Throwable t) {
            logger.error("jinx://app start due to an error!", t);
            System.exit(-1);
        }
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        logger.info("jinx://app stopped.");
    }

}
