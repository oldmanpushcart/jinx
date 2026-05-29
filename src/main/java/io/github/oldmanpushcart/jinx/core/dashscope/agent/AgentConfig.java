package io.github.oldmanpushcart.jinx.core.dashscope.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * DashScope 配置
 */
@ConfigurationProperties("jinx.dashscope.agent")
public record AgentConfig(
        Model model
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

}
