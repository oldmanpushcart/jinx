package io.github.oldmanpushcart.jinx.core.dashscope.agent.plugin;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.session.SessionPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.session.storage.FileFragmentStorage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.jinx.core.dashscope.agent.DashscopeAgentConfig;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class SessionPluginFactory {

    @Singleton
    public Plugin makeSessionPlugin(DashscopeAgentConfig config) {
        return SessionPlugin.newBuilder()
                .storage(FileFragmentStorage.newBuilder()
                        .directory(config.dataspace())
                        .build())

                // 1M
                .maxTokens(1000 * 1000)

                // 70%触发压缩
                .gcRatio(0.7)

                // 压缩模型
                .model(ChatModel.QWEN_FLASH)

                .build();
    }

}
