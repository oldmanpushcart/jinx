package io.github.oldmanpushcart.jinx.core.mcp;

import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.jinx.JinxConfig;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * MCP启动器
 */
@Context
class McpLatcher {

    private static final String MCP_FILE_SUFFIX = ".mcp.json";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JinxConfig config;
    private final McpRegistry registry;
    private final Thread scanner;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();


    public McpLatcher(JinxConfig config, McpRegistry registry) {
        this.config = config;
        this.registry = registry;
        this.scanner = new Thread(this::scanning) {{
            setName("MCP-SCANNER");
            setDaemon(true);
        }};
    }

    @PostConstruct
    void init() {
        scanner.start();
    }

    @PreDestroy
    void destroy() {
        scanner.interrupt();
    }

    private void reload() throws IOException {

        final var directory = config.dataspace().resolve("mcp");
        if (!Files.isDirectory(directory)) {
            logger.warn("jinx://mcp directory does not exist. path={}", directory);
            return;
        }

        final var stages = new ArrayList<CompletionStage<Void>>();
        try (final var __stream__ = Files.list(directory)) {
            __stream__
                    .filter(Files::isRegularFile)
                    .filter(McpLatcher::isMcpFile)
                    .forEach(mcpPath -> {

                        final var mcpFilename = mcpPath.getFileName().toString();
                        final var mcpName = mcpFilename.substring(0, mcpFilename.length() - MCP_FILE_SUFFIX.length());

                        try {

                            final var lastModifiedAt = Files.getLastModifiedTime(mcpPath).toInstant();
                            final var exist = entries.get(mcpName);

                            // 如果和当前存在的最后修改时间相同，则说明没有变化
                            if (null != exist && Objects.equals(lastModifiedAt, exist.lastModifiedAt())) {
                                return;
                            }

                            final var mcpJson = Files.readString(mcpPath, UTF_8);
                            final var mcpMeta = JacksonJsonUtils.toObject(mcpJson, McpMeta.class);
                            final var stage = registry.upsert(mcpName, mcpMeta);

                        } catch (Exception ex) {

                        }

                    });
        }

    }

    private void scanning() {

        while (!Thread.currentThread().isInterrupted()) {

        }

    }

    private static boolean isMcpFile(Path mcpPath) {
        return mcpPath.getFileName().toString().endsWith(MCP_FILE_SUFFIX);
    }

    private record Entry(String name, McpMeta mcp, Instant lastModifiedAt) {

    }

}
