package io.github.oldmanpushcart.jinx.core.information;

import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.JinxConfig;

import java.nio.file.Path;

public record Information(
        Jinx jinx,
        Computer computer
) {

    public record Jinx(
            String version,
            Path home,
            Path dataspace,
            Path workspace,
            Path logspace,
            Path confspace
    ) {

    }

    public record Computer(
            String osName,
            String osVersion,
            String osArch,
            String home
    ) {

    }

    public static Information newInstant(JinxConfig config) {
        return new Information(
                new Jinx(
                        Constants.VERSION,
                        Path.of("./"),
                        config.dataspace(),
                        config.workspace(),
                        Path.of("./logs"),
                        Path.of("./conf")
                ),
                new Computer(
                        System.getProperty("os.name"),
                        System.getProperty("os.version"),
                        System.getProperty("os.arch"),
                        System.getProperty("user.home")
                )
        );
    }

}
