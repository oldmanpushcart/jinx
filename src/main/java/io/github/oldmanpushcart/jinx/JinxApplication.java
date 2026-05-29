package io.github.oldmanpushcart.jinx;

import io.micronaut.context.annotation.Context;
import io.micronaut.runtime.Micronaut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Context
public class JinxApplication {

    static {
        System.setProperty("logback.configurationFile", "conf/logback.xml");
        System.setProperty("micronaut.config.files", "conf/application.yml");
    }

    private static final Logger logger = LoggerFactory.getLogger(JinxApplication.class);

    public static void main(String[] args) {
        try {
            logger.info("jinx://app starting...");
            Micronaut
                    .run(JinxApplication.class, args)
                    .start();
            logger.info("jinx://app started.");
        } catch (Throwable t) {
            logger.warn("jinx://app start occur error!", t);
            t.printStackTrace(System.err);
            System.exit(-1);
        }
    }

}
