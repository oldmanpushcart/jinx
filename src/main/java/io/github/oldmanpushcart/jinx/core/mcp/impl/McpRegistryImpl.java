package io.github.oldmanpushcart.jinx.core.mcp.impl;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.ToolSubscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.jinx.core.mcp.McpMeta;
import io.github.oldmanpushcart.jinx.core.mcp.McpRegistry;
import io.modelcontextprotocol.spec.McpClientTransport;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class McpRegistryImpl implements McpRegistry {

    private final Toolbox toolbox;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public McpRegistryImpl(Toolbox toolbox) {
        this.toolbox = toolbox;
    }


    @Override
    public CompletionStage<Void> upsert(String name, McpMeta meta) {
        return CompletableFuture.completedStage(null)
                .thenApply(u -> recoverable(m -> McpHelper.toTransport(meta)))
                .thenCompose(transport -> {
                    remove(name);
                    return toolbox.subscribeMcp(name, transport);
                })
                .thenAccept(subscription -> {
                    final var entry = new Entry(name, meta, subscription);
                    entries.put(name, entry);
                });
    }

    @Override
    public Optional<McpMeta> remove(String name) {
        final var exist = entries.remove(name);
        if (null != exist) {
            IOUtils.closeQuietly(exist);
            return Optional.of(exist.meta());
        } else {
            return Optional.empty();
        }
    }

    private McpClientTransport recoverable(RecoverableMcpClientTransport.McpClientTransportFactory factory) {
        return RecoverableMcpClientTransport.newBuilder()
                .transportFactory(factory)
                .build();
    }

    private record Entry(String name, McpMeta meta, ToolSubscription subscription)
            implements AutoCloseable {

        @Override
        public void close() {
            IOUtils.closeQuietly(subscription());
            IOUtils.closeQuietly(subscription.source());
        }

    }

}
