package io.github.oldmanpushcart.jinx.core2.mcp;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("jinx.mcp")
public record McpConfig(
        Path directory
) {
}
