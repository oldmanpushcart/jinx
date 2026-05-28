package io.github.oldmanpushcart.jinx.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.context.annotation.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

/**
 * DashScope 配置
 */
@ConfigurationProperties("jinx.agent")
public record AgentConfig(
        Model model,
        ClientConfig client
) {

    /**
     * 模型
     */
    public enum Model {

        @JsonProperty("qwen-flash")
        QWEN_FLASH,

        @JsonProperty("qwen-plus")
        QWEN_PLUS,

        @JsonProperty("qwen-max")
        QWEN_MAX

    }

    @ConfigurationProperties("client")
    public record ClientConfig(
            String ak,
            HttpConfig http
    ) {

        @ConfigurationProperties("http")
        public record HttpConfig(
                Duration connectTimeout,
                Duration readTimeout,
                Duration writeTimeout
        ) {

        }

    }


}
