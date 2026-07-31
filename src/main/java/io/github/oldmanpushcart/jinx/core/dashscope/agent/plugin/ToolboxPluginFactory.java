package io.github.oldmanpushcart.jinx.core.dashscope.agent.plugin;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.EmbeddingToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.ToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.McpToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.skill.SkillsToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.toolkit.ToolkitToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.TextFileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.network.HttpToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.jinx.core.ConfigTools;
import io.github.oldmanpushcart.jinx.core.JinxConfigToolkit;
import io.github.oldmanpushcart.jinx.core.dashscope.agent.DashscopeAgentConfig;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import io.modelcontextprotocol.spec.McpClientTransport;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.sequentialMap;

@Factory
public class ToolboxPluginFactory {

    @Singleton
    public Plugin makeToolboxPlugin(
            final DashscopeAgentConfig config,
            final DashscopeClient client,
            final ConfigTools configTools,
            @Value("${jinx.skill.directories}") final List<Path> skillDirectories,
            @Named("namedMcpClientTransportMap") final Map<String, McpClientTransport> namedMcpClientTransportMap
    ) {
        return ToolboxPlugin.newBuilder()
                .fixes(List.of(buildingFixedToolbox(config, client)))
                .dynamics(List.of(buildingDynamicToolbox(config, client, configTools, skillDirectories, namedMcpClientTransportMap)))
                .build();
    }

    private Toolbox buildingFixedToolbox(
            DashscopeAgentConfig config,
            DashscopeClient client
    ) {

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(EmbeddingToolIndexer.newBuilder()
                        .client(client)
                        .storage(config.dataspace().resolve("./fixed-toolbox-indexer.json"))
                        .build())
                .build();

        final var mergeTs = List.of(
                ToolkitToolSource.newBuilder()
                        .append(
                                ShellToolkit.newBuilder()
                                        .securityLevel(ShellToolkit.SecurityLevel.NONE)
                                        .build()
                        )
                        .build()
        );

        return sequentialMap(mergeTs, ToolkitToolSource::initialize)
                .thenCompose(sources -> sequentialMap(sources, toolbox::subscribe))
                .thenApply(u -> toolbox)
                .toCompletableFuture()
                .join();

    }

    private Toolbox buildingDynamicToolbox(
            DashscopeAgentConfig config,
            DashscopeClient client,
            ConfigTools configTools,
            List<Path> skillDirectories,
            Map<String, McpClientTransport> namedMcpClientTransportMap
    ) {

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(EmbeddingToolIndexer.newBuilder()
                        .client(client)
                        .storage(config.dataspace().resolve("./dynamic-toolbox-indexer.json"))
                        .build())
                .build();

        final var mcpTs = namedMcpClientTransportMap.entrySet()
                .stream()
                .map(entry -> {
                    final var name = entry.getKey();
                    final var transport = entry.getValue();
                    return McpToolSource.newBuilder()
                            .namespace(name)
                            .transport(transport)
                            .build();
                })
                .toList();

        final var skillTs = skillDirectories.stream()
                .map(directory ->
                        SkillsToolSource.newBuilder()
                                .directory(directory)
                                .build())
                .toList();

        final var toolkitTs = List.of(ToolkitToolSource.newBuilder()
                .append(
                        DashscopeToolkit.create(),
                        HttpToolkit.newBuilder()
                                .workspace(config.workspace())
                                .build(),
                        FileOpsToolkit.newBuilder()
                                .workspace(config.workspace())
                                .build(),
                        TextFileOpsToolkit.newBuilder()
                                .workspace(config.workspace())
                                .build(),
                        JinxConfigToolkit.create(configTools)
                )
                .build());

        final var mergeTs = Stream.of(mcpTs, skillTs, toolkitTs)
                .flatMap(List::stream)
                .toList();

        return sequentialMap(mergeTs, ToolSource::initialize)
                .thenCompose(sources -> sequentialMap(sources, toolbox::subscribe))
                .thenApply(u -> toolbox)
                .toCompletableFuture()
                .join();

    }

}
