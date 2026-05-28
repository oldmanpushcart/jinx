package io.github.oldmanpushcart.jinx.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * DashScope 配置
 */
@ConfigurationProperties("jinx.dashscope")
public record DashscopeConfig(

        @JsonProperty("api-key")
        String ak,

        @JsonProperty("model")
        Model model

) {

    /**
     * 模型
     */
    public enum Model {
        QWEN_FLASH,
        QWEN_PLUS,
        QWEN_MAX
    }

}
