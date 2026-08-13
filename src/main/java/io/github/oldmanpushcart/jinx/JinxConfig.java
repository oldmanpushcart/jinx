package io.github.oldmanpushcart.jinx;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ConfigurationProperties("jinx")
public record JinxConfig(Path dataspace, Path workspace) {

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(dataspace());
        Files.createDirectories(workspace());
    }

}
