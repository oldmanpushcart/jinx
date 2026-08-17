package io.github.oldmanpushcart.jinx.core.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

@Context
class McpLatcher {

    private final McpDetector detector;
    private final Toolbox toolbox;

    McpLatcher(McpDetector detector, Toolbox toolbox) {
        this.detector = detector;
        this.toolbox = toolbox;
    }

    @PostConstruct
    void init() {

        toolbox.subscribeTools("dashscope4j", mcpToolkits())
                .toCompletableFuture()
                .join();

    }

    private Toolkit mcpToolkits() {
        return new Toolkit() {

            @Override
            public @NonNull Iterator<Tool> iterator() {
                return List.of(list(), detail(), remove(), append()).iterator();
            }

            private Tool list() {
                return FunctionTool.newBuilder()
                        .name("mcp_meta_list")
                        .description("列出已加载的MCP元数据")
                        .parameterType(Object.class)
                        .function(u -> detector.list())
                        .build();
            }

            private Tool detail() {
                return FunctionTool.newBuilder()
                        .name("mcp_meta_detail")
                        .description("获取已加载的MCP元数据详情")
                        .parameterType(DetailSpec.class)
                        .<DetailSpec>function(spec -> {
                            final var name = spec.name();
                            return detector.get(name)
                                    .orElseThrow(() -> new RuntimeException("MCP: %s 不存在".formatted(name)));
                        })
                        .build();
            }

            private Tool remove() {
                return FunctionTool.newBuilder()
                        .name("mcp_meta_remove")
                        .description("移除已加载的MCP元数据")
                        .parameterType(RemoveSpec.class)
                        .<RemoveSpec>function(spec -> detector.remove(spec.name()))
                        .build();
            }

            private Tool append() {
                return FunctionTool.newBuilder()
                        .name("mcp_meta_append")
                        .description("添加MCP元数据")
                        .parameterType(AppendSpec.class)
                        .<AppendSpec>function(spec -> detector.append(spec.path()))
                        .build();
            }

            private record DetailSpec(String name) {

            }

            private record RemoveSpec(String name) {

            }

            private record AppendSpec(Path path) {

            }

        };
    }

}
