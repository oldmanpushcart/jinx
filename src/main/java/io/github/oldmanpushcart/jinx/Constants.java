package io.github.oldmanpushcart.jinx;

import java.io.IOException;
import java.util.Properties;

public class Constants {

    private final static Properties properties = new Properties();

    static {
        try {
            properties.load(Constants.class.getResourceAsStream("/jinx-meta.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 版本
     */
    public static final String VERSION = properties.getProperty("version");

}
