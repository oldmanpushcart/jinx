package io.github.oldmanpushcart.jinx.core.dashscope.agent.plugin;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.SimpleToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.TextFileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.network.HttpToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.RuntimeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.jinx.core.dashscope.agent.DashscopeAgentConfig;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import io.modelcontextprotocol.spec.McpClientTransport;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse.Mode.DYNAMIC;
import static io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse.Mode.FIXED;

@Factory
public class ToolboxPluginFactory {

    @Singleton
    public Plugin makeToolboxPlugin(
            final DashscopeAgentConfig config,
            @Value("${jinx.skill.directories}") final List<Path> skillDirectories,
            @Named("namedMcpClientTransportMap") final Map<String, McpClientTransport> namedMcpClientTransportMap
    ) {
        return SimpleToolboxPlugin.newBuilder()

                // 注册SKILL目录
                .building(builder -> {
                    if (CommonUtils.isNotEmpty(skillDirectories)) {
                        skillDirectories.forEach(directory ->
                                builder.skill(DYNAMIC, directory));
                    }
                })

                // 注册MCP
                .building(builder -> {
                    if (CommonUtils.isNotEmpty(namedMcpClientTransportMap)) {
                        namedMcpClientTransportMap.forEach((name, transport) ->
                                builder.mcp(DYNAMIC, name, transport));
                    }
                })

                // 注册动态工具
                .building(builder -> {
                    List.of(
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
                    ).forEach(kit -> builder.toolkit(DYNAMIC, kit));
                })

                // 注册静态工具
                .building(builder -> {
                    List.of(
                            ShellToolkit.newBuilder()
                                    .securityLevel(ShellToolkit.SecurityLevel.NONE)
                                    .build(),
                            RuntimeToolkit.create()
                    ).forEach(kit -> builder.toolkit(FIXED, kit));
                })

                .enableSearchTools(true)
                .syncInterval(Duration.ofSeconds(5))
                .build();
    }

}
