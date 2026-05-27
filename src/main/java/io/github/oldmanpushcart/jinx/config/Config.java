package io.github.oldmanpushcart.jinx.config;

public record Config(
    ServerConfig server,
    String logbackConfigPath
) {}
