package io.github.oldmanpushcart.jinx.core.dashscope.client;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("jinx.dashscope.client")
public record DashscopeClientConfig(
        String ak,
        HttpClientConfig http
) {

    @ConfigurationProperties("http")
    public record HttpClientConfig(
            Duration connectTimeout,
            Duration readTimeout,
            Duration writeTimeout
    ) {

    }

}
