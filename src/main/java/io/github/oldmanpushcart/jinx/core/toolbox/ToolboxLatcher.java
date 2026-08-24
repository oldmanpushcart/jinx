package io.github.oldmanpushcart.jinx.core.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.toolkit.ToolkitToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.TextFileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.network.HttpToolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.jinx.Constants;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具箱启动器
 */
@Context
class ToolboxLatcher {

    private final Toolbox toolbox;
    private final List<Toolkit> toolkits;
    private final List<Tool> tools;
    private final Set<AutoCloseable> autoCloseableSet = ConcurrentHashMap.newKeySet();

    public ToolboxLatcher(
            Toolbox toolbox,
            List<Toolkit> toolkits,
            List<Tool> tools
    ) {
        this.toolbox = toolbox;
        this.toolkits = toolkits;
        this.tools = tools;
    }

    @PreDestroy
    void destroy() {
        autoCloseableSet.forEach(IOUtils::closeQuietly);
    }

    @PostConstruct
    void init() {
        CompletableFuture.completedStage(null)
                .thenCompose(u -> subscribeTools())
                .toCompletableFuture()
                .join();
    }

    private CompletionStage<?> subscribeTools() {
        final var source = ToolkitToolSource.newBuilder()
                .namespace("dashscope4j")
                .building(builder -> {

                    if (CommonUtils.isNotEmpty(tools)) {
                        tools.forEach(builder::append);
                    }

                    if (CommonUtils.isNotEmpty(toolkits)) {
                        toolkits.forEach(builder::append);
                    }

                })
                .append(
                        toolkit(),
                        DashscopeToolkit.create(),
                        HttpToolkit.newBuilder()
                                .workspace(Constants.WORK)
                                .build(),
                        FileOpsToolkit.newBuilder()
                                .workspace(Constants.USER_HOME)
                                .build(),
                        TextFileOpsToolkit.newBuilder()
                                .workspace(Constants.USER_HOME)
                                .build()
                )
                .build();
        autoCloseableSet.add(source);
        return source.initialize()
                .thenCompose(toolbox::subscribe);
    }

    private Toolkit toolkit() {
        return new Toolkit() {

            private final List<Tool> tools = List.of(session());

            @Override
            public @NonNull Iterator<Tool> iterator() {
                return tools.iterator();
            }

            private Tool session() {
                return FunctionTool.newBuilder()
                        .name("session")
                        .description("获取当前会话的SESSION")
                        .parameterType(Object.class)
                        .function((caller, u) -> {
                            final var request = caller.request();
                            final var sessionId = (String) request.context().get("SESSION-ID");
                            if (CommonUtils.isNotBlankString(sessionId)) {
                                return Map.of(
                                        "id", sessionId
                                );
                            } else {
                                return null;
                            }
                        })
                        .build();
            }

        };
    }

}
