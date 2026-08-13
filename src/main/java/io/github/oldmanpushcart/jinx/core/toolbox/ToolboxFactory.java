package io.github.oldmanpushcart.jinx.core.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.hook.toolbox.ToolboxHook;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.EmbeddingToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.TextFileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.network.HttpToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.RuntimeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.jinx.JinxConfig;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Factory
public class ToolboxFactory {

    @Singleton
    public Toolbox makeToolbox(JinxConfig config, DashscopeClient client, List<Toolkit> toolkits) {
        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(EmbeddingToolIndexer.newBuilder()
                        .storage(config.dataspace().resolve("embedding-tool-indexer.jsonl"))
                        .client(client)
                        .build())
                .syncInterval(Duration.ofSeconds(3))
                .build();

        final List<Iterable<? extends Tool>> merged = new ArrayList<>(toolkits);
        merged.addAll(List.of(
                DashscopeToolkit.create(),
                HttpToolkit.newBuilder()
                        .workspace(config.workspace())
                        .build(),
                FileOpsToolkit.newBuilder()
                        .workspace(config.workspace())
                        .build(),
                TextFileOpsToolkit.newBuilder()
                        .workspace(config.workspace())
                        .build()
        ));

        toolbox.subscribeTools("dashscope4j", merged)
                .toCompletableFuture()
                .join();

        return toolbox;
    }

    @Singleton
    public ToolboxHook makeToolboxHook(Toolbox toolbox) {

        final var tools = Stream.of(
                        RuntimeToolkit.create(),
                        ShellToolkit.newBuilder()
                                .securityLevel(ShellToolkit.SecurityLevel.NONE)
                                .build()
                )
                .flatMap(toolkit -> StreamSupport.stream(toolkit.spliterator(), false))
                .toList();

        return ToolboxHook.newBuilder()
                .toolbox(toolbox)
                .tools(tools)
                .build();
    }

}
