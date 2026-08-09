package io.github.oldmanpushcart.jinx.core2.mcp.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.ToolSubscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.ToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.McpToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.jinx.core.mcp.McpFileStore;
import io.github.oldmanpushcart.jinx.core2.CoreErrorCodes;
import io.github.oldmanpushcart.jinx.core2.mcp.McpConfig;
import io.github.oldmanpushcart.jinx.core2.mcp.McpEntity;
import io.github.oldmanpushcart.jinx.core2.mcp.McpException;
import io.github.oldmanpushcart.jinx.core2.mcp.McpRegistry;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;

@Singleton
public class McpRegistryImpl implements McpRegistry, CoreErrorCodes {

    private static final String MCP_FILE_SUFFIX = ".mcp.json";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final McpConfig config;
    private final Toolbox toolbox;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public McpRegistryImpl(McpConfig config, Toolbox toolbox) {
        this.config = config;
        this.toolbox = toolbox;
    }

    @Override
    public void register(McpEntity mcp) {

        // 存储MCP
        store(mcp);

        // 阻塞订阅
        subscribe(mcp)
                .toCompletableFuture()
                .join();

        logger.debug("{}/register completed. mcp={};", this, mcp.name());

    }

    private McpClientTransport toTransport(McpEntity mcp) {
        return switch (mcp.type()) {

            // stdio
            case STDIO -> {
                final var stdio = (McpEntity.Stdio) mcp;
                final var params = ServerParameters.builder(stdio.cmd())
                        .args(stdio.args())
                        .env(stdio.env())
                        .build();
                final var mapper = new JacksonMcpJsonMapper(new ObjectMapper());
                yield new StdioClientTransport(params, mapper);
            }

            // sse
            case SSE -> {
                final var sse = (McpEntity.Http) mcp;
                final var baseUrl = McpFileStore.resolveEnvVars(sse.host().toString());
                final var endpoint = McpFileStore.resolveEnvVars(sse.endpoint());
                yield HttpClientSseClientTransport.builder(baseUrl)
                        .sseEndpoint(endpoint)
                        .httpRequestCustomizer((builder, method, ep, body, context) -> {
                            if (CommonUtils.isNotEmpty(sse.headers())) {
                                sse.headers().forEach(builder::header);
                            }
                        })
                        .build();
            }

            // streamable-http
            case STREAMABLE_HTTP -> {
                final var http = (McpEntity.Http) mcp;
                final var baseUrl = McpFileStore.resolveEnvVars(http.host().toString());
                final var endpoint = McpFileStore.resolveEnvVars(http.endpoint());
                yield HttpClientStreamableHttpTransport.builder(baseUrl)
                        .endpoint(endpoint)
                        .httpRequestCustomizer((builder, method, ep, body, context) -> {
                            if (CommonUtils.isNotEmpty(http.headers())) {
                                http.headers().forEach(builder::header);
                            }
                        })
                        .build();
            }

        };
    }

    private ToolSource toTs(McpEntity mcp) {
        return McpToolSource.newBuilder()
                .namespace(mcp.name())
                .transport(recoverable(m -> toTransport(mcp)))
                .build();
    }


    /**
     * 存储MCP
     *
     * @param mcp MCP
     */
    private synchronized void store(McpEntity mcp) {

        if (null == config.directory()) {
            return;
        }

        try {

            // 存储目录
            final var directory = config.directory();
            Files.createDirectories(directory);

            // 覆盖重写MCP文件
            final var mcpFile = directory.resolve(mcp.name() + MCP_FILE_SUFFIX);
            final var mcpJson = JacksonJsonUtils.toJson(mcp);
            Files.writeString(mcpFile, mcpJson, UTF_8, CREATE, WRITE, TRUNCATE_EXISTING, SYNC);

        } catch (IOException ioEx) {
            throw McpException.of(mcp, MCP_STORE_ERROR, "Store fail!", ioEx);
        }

    }

    private McpClientTransport recoverable(RecoverableMcpClientTransport.McpClientTransportFactory factory) {
        return RecoverableMcpClientTransport.newBuilder()
                .transportFactory(factory)
                .build();
    }

    private CompletionStage<Void> subscribe(McpEntity mcp) {
        return CompletableFuture

                // 转换为工具源
                .completedStage(toTs(mcp))

                // 工具源初始化
                .thenCompose(ToolSource::initialize)
                .exceptionally(ex -> {
                    throw McpException.of(mcp, MCP_INIT_ERROR, "Initialize fail!", ex);
                })

                // 工具箱订阅
                .thenCompose(toolbox::subscribe)
                .exceptionally(ex -> {
                    throw McpException.of(mcp, MCP_SUBSCRIBE_ERROR, "Subscribe fail!", ex);
                })

                // 添加到注册表
                .thenAccept(sub -> {
                    final var entry = new Entry(mcp.name(), mcp, sub);
                    final var exists = entries.put(mcp.name(), entry);
                    if (null != exists) {
                        exists.close();
                    }
                });
    }

    @PostConstruct
    public void init() {
        final var directory = config.directory();

        if (null == directory) {
            return;
        }

        final var stages = new ArrayList<CompletionStage<Void>>();

        // 遍历MCP目录并完成注册
        try (final var stream = Files.list(directory)) {

            // 过滤出符合格式要求的MCP文件
            final var mcpFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(MCP_FILE_SUFFIX))
                    .toList();

            // 从文件中注册上来
            mcpFiles.forEach(mcpFile -> {
                final var filename = mcpFile.getFileName().toString();
                final var mcpName = filename.substring(0, filename.length() - MCP_FILE_SUFFIX.length());
                try {

                    // 加载
                    final var mcpJson = Files.readString(mcpFile, UTF_8);
                    final var mcp = JacksonJsonUtils.toObject(mcpJson, McpEntity.class);
                    if (!Objects.equals(mcp.name(), mcpName)) {
                        throw new RuntimeException("Illegal mcp name, expect: %s but was: %s".formatted(
                                mcpName,
                                mcp.name()
                        ));
                    }

                    // 订阅
                    final var stage = subscribe(mcp).whenComplete((u, ex) -> {
                        if (null != ex) {
                            logger.warn("{}/init mcp ignored by subscribe error! mcp={};", this, mcpName, ex);
                        } else {
                            logger.debug("{}/init mcp registered. mcp={};", this, mcpName);
                        }
                    });
                    stages.add(stage);

                } catch (Exception ex) {
                    logger.warn("{}/init mcp ignored by load error! mcp={};file={}", this, mcpName, mcpFile, ex);
                }
            });

        } catch (IOException ioEx) {
            logger.warn("{}/init skipped by error!", this, ioEx);
        }

        // 阻塞等待加载完成
        CompletableFutureUtils.allOf(stages)
                .toCompletableFuture()
                .join();

    }

    @Override
    public Optional<McpEntity> unregister(String name) {
        final Optional<Entry> removeOpt = Optional.ofNullable(entries.remove(name));
        removeOpt.ifPresent(Entry::close);
        logger.debug("{}/{} unregister completed. exists={}", this, name, removeOpt.isPresent());
        return removeOpt
                .map(Entry::mcp);
    }

    @Override
    public List<McpEntity> listAll() {
        return entries.values().stream()
                .map(Entry::mcp)
                .toList();
    }

    @Override
    public Optional<McpEntity> get(String name) {
        return Optional.ofNullable(entries.get(name))
                .map(Entry::mcp);
    }

    private record Entry(String name, McpEntity mcp, ToolSubscription subscription)
            implements Closeable {

        @Override
        public void close() {
            subscription.close();
            subscription.source().close();
        }

    }

}
