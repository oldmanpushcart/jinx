package io.github.oldmanpushcart.jinx;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("jinx")
public record JinxConfig(Path dataspace, Path workspace) {
}
