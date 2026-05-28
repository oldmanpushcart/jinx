package io.github.oldmanpushcart.jinx;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import io.micronaut.context.annotation.Context;
import io.micronaut.runtime.Micronaut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Context
public class JinxApplication {

    private static final Logger logger = LoggerFactory.getLogger(JinxApplication.class);

    public static void main(String[] args) {
        try {
            initLogging();
            logger.info("jinx://app starting...");
            bootstrapApp();
            logger.info("jinx://app started.");
        } catch (Throwable t) {
            logger.warn("jinx://app start occur error!", t);
            t.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    // 初始化日志
    private static void initLogging() throws Exception {
        final var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final var configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(new File("conf/logback.xml"));
    }

    // 引导应用程序启动
    private static void bootstrapApp(String... args) {
        System.setProperty("micronaut.config.files", "conf/application.yml");
        Micronaut
                .run(JinxApplication.class, args)
                .start();
    }

}
