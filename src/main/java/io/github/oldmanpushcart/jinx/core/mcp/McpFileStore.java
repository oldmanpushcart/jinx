package io.github.oldmanpushcart.jinx.core.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * MCP 配置文件存储
 * <p>
 * 负责扫描 conf/mcp/ 目录下的 .mcp.json 文件，完成文件的读取、写入、删除。
 * </p>
 */
@Singleton
public class McpFileStore {

    private static final Logger logger = LoggerFactory.getLogger(McpFileStore.class);
    private static final String MCP_FILE_SUFFIX = ".mcp.json";

    private final McpConfig config;
    private final ObjectMapper mapper;

    public McpFileStore(McpConfig config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    /**
     * 加载所有 MCP 配置条目
     *
     * @return MCP 配置条目列表（按名称排序）
     */
    public List<McpFileEntry> loadAll() {
        final var dir = config.configDirectory();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (final var stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(MCP_FILE_SUFFIX))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(this::readEntry)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            logger.error("jinx://mcp/file-store list directory failed: {}", dir, e);
            return List.of();
        }
    }

    /**
     * 保存 MCP 配置条目
     *
     * @param entry MCP 配置条目
     */
    public void save(McpFileEntry entry) throws IOException {
        final var dir = config.configDirectory();
        Files.createDirectories(dir);
        final var file = dir.resolve(entry.name() + MCP_FILE_SUFFIX);
        final var map = toMap(entry);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), map);
        logger.info("jinx://mcp/file-store saved: {}", file);
    }

    /**
     * 删除 MCP 配置文件
     *
     * @param name MCP 工具名称
     * @return 是否成功删除
     */
    public boolean delete(String name) throws IOException {
        final var file = config.configDirectory().resolve(name + MCP_FILE_SUFFIX);
        if (Files.exists(file)) {
            Files.delete(file);
            logger.info("jinx://mcp/file-store deleted: {}", file);
            return true;
        }
        return false;
    }

    /**
     * 判断 MCP 配置文件是否存在
     *
     * @param name MCP 工具名称
     * @return 是否存在
     */
    public boolean exists(String name) {
        final var file = config.configDirectory().resolve(name + MCP_FILE_SUFFIX);
        return Files.exists(file);
    }

    /**
     * 解析环境变量占位符
     * <p>
     * 将 ${ENV_VAR} 格式的字符串替换为对应的环境变量值。
     * 如果环境变量不存在，保留原始占位符。
     * </p>
     *
     * @param value 包含占位符的字符串
     * @return 解析后的字符串
     */
    public static String resolveEnvVars(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        final var pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)}");
        final var matcher = pattern.matcher(value);
        final var sb = new StringBuilder();
        while (matcher.find()) {
            final var envVar = matcher.group(1);
            final var envValue = System.getenv(envVar);
            matcher.appendReplacement(sb, envValue != null
                    ? java.util.regex.Matcher.quoteReplacement(envValue)
                    : java.util.regex.Matcher.quoteReplacement(matcher.group()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // --- private ---

    private McpFileEntry readEntry(Path file) {
        try {
            final var map = mapper.readValue(file.toFile(), new TypeReference<Map<String, Object>>() {});
            final var name = (String) map.get("name");
            final var enabled = (Boolean) map.getOrDefault("enabled", true);
            final var server = mapper.convertValue(map, McpConfig.Server.class);
            return new McpFileEntry(name != null ? name : nameFromFile(file), enabled, server);
        } catch (Exception e) {
            logger.warn("jinx://mcp/file-store read failed: {}", file, e);
            return null;
        }
    }

    private Map<String, Object> toMap(McpFileEntry entry) {
        final var map = new LinkedHashMap<String, Object>();
        map.put("name", entry.name());
        map.put("enabled", entry.enabled());

        final var server = entry.server();
        if (server instanceof McpConfig.HttpServer http) {
            map.put("type", typeString(http.type()));
            map.put("base-url", http.baseUrl().toString());
            map.put("endpoint", http.endpoint());
            if (http.headers() != null && !http.headers().isEmpty()) {
                map.put("headers", http.headers());
            }
        } else if (server instanceof McpConfig.StdioServer stdio) {
            map.put("type", typeString(stdio.type()));
            map.put("cmd", stdio.cmd());
            if (stdio.args() != null) {
                map.put("args", stdio.args());
            }
            if (stdio.env() != null && !stdio.env().isEmpty()) {
                map.put("env", stdio.env());
            }
        }
        return map;
    }

    private String typeString(McpConfig.Server.Type type) {
        return switch (type) {
            case STDIO -> "stdio";
            case SSE -> "sse";
            case STREAMABLE_HTTP -> "streamable-http";
        };
    }

    private String nameFromFile(Path file) {
        final var fileName = file.getFileName().toString();
        return fileName.substring(0, fileName.length() - MCP_FILE_SUFFIX.length());
    }

}
