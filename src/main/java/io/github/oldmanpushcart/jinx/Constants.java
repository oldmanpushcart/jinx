package io.github.oldmanpushcart.jinx;

import java.io.IOException;
import java.nio.file.Path;
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

    public static final Path USER_HOME = Path.of(System.getProperty("user.home"));
    public static final Path HOME = Path.of("./").normalize().toAbsolutePath();
    public static final Path LOGS = HOME.resolve("logs").normalize().toAbsolutePath();
    public static final Path CONF = HOME.resolve("conf").normalize().toAbsolutePath();
    public static final Path DATA = HOME.resolve("data").normalize().toAbsolutePath();
    public static final Path WORK = HOME.resolve("work").normalize().toAbsolutePath();

}
