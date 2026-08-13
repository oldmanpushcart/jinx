package io.github.oldmanpushcart.jinx.core.dashscope;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;

import java.nio.file.Files;
import java.time.Duration;

@ConfigurationProperties("jinx.dashscope")
public record DashscopeConfig(Client client, Agent agent) {

    @ConfigurationProperties("client")
    public record Client(String ak, Http http) {

        @ConfigurationProperties("http")
        public record Http(
                Duration connectTimeout,
                Duration readTimeout,
                Duration writeTimeout
        ) {

        }

    }

    @ConfigurationProperties("agent")
    public record Agent(
            String name,
            String description
    ) {

    }

}
