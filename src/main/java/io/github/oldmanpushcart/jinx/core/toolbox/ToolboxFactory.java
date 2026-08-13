package io.github.oldmanpushcart.jinx.core.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.hook.toolbox.ToolboxHook;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.EmbeddingToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.RuntimeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.jinx.JinxConfig;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Factory
class ToolboxFactory {

    @Singleton
    public Toolbox makeToolbox(JinxConfig config, DashscopeClient client) {
        return HashMapToolbox.newBuilder()
                .indexer(EmbeddingToolIndexer.newBuilder()
                        .storage(config.dataspace().resolve("embedding-tool-indexer.jsonl"))
                        .client(client)
                        .build())
                .syncInterval(Duration.ofSeconds(3))
                .build();
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
