package io.github.oldmanpushcart.jinx.core.dashscope.agent;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;

import java.util.List;

@Factory
public class DashscopeAgentFactory {

    @Context
    public Agent makeDashscopeAgent(
            final DashscopeAgentConfig config,
            final DashscopeClient client,
            final List<Plugin> plugins
    ) {
        return ReActAgent.newBuilder()
                .name("Jinx")
                .description("Jinx Agent")
                .client(client)
                .model(decideModel(config))
                .plugins(plugins)
                .build();
    }

    private ChatModel decideModel(DashscopeAgentConfig config) {
        if (null == config.model()) {
            return ChatModel.QWEN_FLASH;
        }
        return switch (config.model()) {
            case QWEN_FLASH -> ChatModel.QWEN_FLASH;
            case QWEN_PLUS -> ChatModel.QWEN_PLUS;
            case QWEN_MAX -> ChatModel.QWEN_MAX;
        };
    }

}
