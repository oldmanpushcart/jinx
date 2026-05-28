package io.github.oldmanpushcart.jinx.config;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("jinx")
public record Config(
    DashscopeConfig dashscope
) {
}
