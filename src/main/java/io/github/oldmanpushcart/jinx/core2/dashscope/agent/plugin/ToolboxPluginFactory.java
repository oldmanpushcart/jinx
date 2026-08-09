package io.github.oldmanpushcart.jinx.core2.dashscope.agent.plugin;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.EmbeddingToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.ToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.toolkit.ToolkitToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.TextFileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.network.HttpToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.RuntimeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolLookup;
import io.github.oldmanpushcart.dashscope4j.client.aigc.embedding.TextEmbeddingModel;
import io.github.oldmanpushcart.jinx.JinxConfig;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Factory
public class ToolboxPluginFactory {

    @Singleton
    public Toolbox makeToolbox(JinxConfig config, DashscopeClient client) {
        final var toolbox = HashMapToolbox.newBuilder()
                .syncInterval(Duration.ofSeconds(5))
                .indexer(EmbeddingToolIndexer.newBuilder()
                        .client(client)
                        .embeddingModel(TextEmbeddingModel.TEXT_EMBEDDING_V4)
                        .model(ChatModel.QWEN_FLASH)
                        .storage(config.dataspace().resolve(".embedding-tool-indexer.json"))
                        .build())
                .build();

        final var toolkitTs = ToolkitToolSource.newBuilder()

                // 文件操作工具集
                .append(FileOpsToolkit.newBuilder()
                        .workspace(config.workspace())
                        .maxResults(1000)
                        .build())

                // 文本文件操作工具集
                .append(TextFileOpsToolkit.newBuilder()
                        .workspace(config.workspace())
                        .charset(StandardCharsets.UTF_8)
                        .maxFileSize(1024 * 1024 * 2)
                        .build())

                // HTTP工具集
                .append(HttpToolkit.newBuilder()
                        .workspace(config.workspace())
                        .maxDownloadSize(1024 * 1024 * 100)
                        .smallTextThreshold(1024 * 1024 * 5)
                        .build())

                .build();

        toolbox.subscribe(toolkitTs)
                .toCompletableFuture()
                .join();

        return toolbox;
    }

    @Singleton
    public ToolboxPlugin makeToolboxPlugin(Toolbox toolbox) {

        final var lookups = List.of(

                // 运行时工具集
                ToolLookup.tools(RuntimeToolkit.create().tools()),

                // SHELL工具集
                ToolLookup.tools(ShellToolkit.newBuilder()
                        .securityLevel(ShellToolkit.SecurityLevel.NONE)
                        .timeout(Duration.ofMinutes(3))
                        .build().tools())

        );

        return ToolboxPlugin.newBuilder()
                .fixes(lookups)
                .dynamics(List.of(toolbox))
                .build();
    }

}
