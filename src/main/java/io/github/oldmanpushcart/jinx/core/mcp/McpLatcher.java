package io.github.oldmanpushcart.jinx.core.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NonNull;

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
                return List.of(list(), detail(), reload()).iterator();
            }

            private Tool list() {
                return FunctionTool.newBuilder()
                        .name("mcp_list")
                        .description("列出已加载的MCP")
                        .parameterType(Object.class)
                        .function(u -> detector.list())
                        .build();
            }

            private Tool detail() {
                return FunctionTool.newBuilder()
                        .name("mcp_detail")
                        .description("获取已加载的MCP详情")
                        .parameterType(DetailSpec.class)
                        .<DetailSpec>function(spec -> {
                            final var name = spec.name();
                            return detector.get(name).orElse(null);
                        })
                        .build();
            }

            private Tool reload() {
                return FunctionTool.newBuilder()
                        .name("mcp_reload")
                        .description("重新加载MCP")
                        .parameterType(ReloadSpec.class)
                        .<ReloadSpec>function(spec -> detector.reload(spec.name()))
                        .build();
            }

            private record DetailSpec(

                    @JsonProperty("name")
                    @JsonPropertyDescription("MCP名称")
                    String name

            ) {

            }

            private record ReloadSpec(

                    @JsonProperty("name")
                    @JsonPropertyDescription("MCP名称")
                    String name

            ) {

            }

        };
    }

}
