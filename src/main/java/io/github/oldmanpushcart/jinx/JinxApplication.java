package io.github.oldmanpushcart.jinx;

import io.micronaut.context.annotation.Context;
import io.micronaut.runtime.Micronaut;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Context
public class JinxApplication implements AutoCloseable {

    static {

        // 获取系统属性，如果不存在，则使用指定的默认路径
        String logbackConfig = System.getProperty("logback.configurationFile", "conf/logback.xml");
        String micronautConfig = System.getProperty("micronaut.config.files", "conf/application.yml,conf/jinx.yml");

        // 将解析后的值重新设置回系统属性中，确保 Micronaut 和 Logback 能读取到
        System.setProperty("logback.configurationFile", logbackConfig);
        System.setProperty("micronaut.config.files", micronautConfig);

    }

    private static final Logger logger = LoggerFactory.getLogger(JinxApplication.class);

    public static void main(String[] args) {
        try {
            logger.info("jinx://app starting...");
            logger.info("jinx://app logback config: {}", System.getProperty("logback.configurationFile"));
            logger.info("jinx://app micronaut config: {}", System.getProperty("micronaut.config.files"));
            Micronaut
                    .run(JinxApplication.class, args)
                    .start();
            logger.info("jinx://app started.");
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
