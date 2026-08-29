package io.github.oldmanpushcart.jinx.extra.mcp.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport.ReconnectStrategies;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.jinx.core.detector.FileDetector;
import io.github.oldmanpushcart.jinx.extra.mcp.McpDetector;
import io.github.oldmanpushcart.jinx.extra.mcp.McpMeta;
import io.micronaut.scheduling.annotation.Scheduled;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
class McpDetectorImpl extends FileDetector<McpMeta> implements McpDetector {

    private static final String MCP_FILE_SUFFIX = ".mcp.json";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private final Toolbox toolbox;

    public McpDetectorImpl(Toolbox toolbox) {
        this.toolbox = toolbox;
    }

    @PostConstruct
    void init() {
        detectQuietly("init");
    }

    @Scheduled(fixedDelay = "10s")
    void scan() {
        detectQuietly("scan");
    }

    @Override
    public String toString() {
        return "jinx://mcp/detector";
    }

    // ---- FileDetector钩子实现 ----

    @Override
    protected Path directory() {
        return MCP_DIR;
    }

    @Override
    protected Path pathOf(String name) {
        return MCP_DIR.resolve("%s%s".formatted(name, MCP_FILE_SUFFIX));
    }

    /**
     * 是否是MCP文件
     *
     * @param path 路径
     * @return TRUE | FALSE
     */
    @Override
    protected boolean isTarget(Path path) {
        return Files.isRegularFile(path)
                && path.getFileName().toString().endsWith(MCP_FILE_SUFFIX);
    }

    @Override
    protected String nameOf(Path path) {
        final var filename = path.getFileName().toString();
        return filename.substring(0, filename.length() - MCP_FILE_SUFFIX.length());
    }

    /**
     * 从文件解析MCP元数据
     *
     * @param path MCP文件路径
     * @return MCP元数据
     * @throws IOException 解析失败
     */
    @Override
    protected McpMeta parse(Path path) throws IOException {
        final var rawMcpJson = Files.readString(path, UTF_8);
        final var mcpJson = replaceHolder(rawMcpJson, System.getenv());
        return JacksonJsonUtils.toObject(mcpJson, McpMeta.class);
    }

    @Override
    protected String nameOf(McpMeta meta) {
        return meta.name();
    }

    @Override
    protected Instant versionOf(Path path, McpMeta meta) throws IOException {
        return Files.getLastModifiedTime(path).toInstant();
    }

    /**
     * 激活MCP：连接MCP服务并订阅其工具
     *
     * @param name    MCP名称
     * @param meta    MCP元数据
     * @param version 版本指纹
     * @return 资源句柄（关闭订阅和传输器）
     */
    @Override
    protected CompletionStage<AutoCloseable> activate(String name, McpMeta meta, Instant version) {
        final var transport = recoverable(name, _m -> toTransport(meta));
        return toolbox.subscribeMcp(meta.name(), transport)

                /*
                 * 连接重构后就需要重新进行注册
                 * 注册的时候会逐出之前已注册的MCP并关闭，所以这里就算发生了并发，也只会有最后一个注册成功的生效。
                 */
                .thenApply(subscription -> (AutoCloseable) () -> {
                    IOUtils.closeQuietly(subscription);
                    transport.close();
                })

                /*
                 * 连接失败或者注册失败，则需要关闭之前已创建的transport，
                 * 避免资源泄漏。
                 */
                .whenComplete((resource, ex) -> {
                    if (null != ex) {
                        transport.close();
                    }
                });
    }

    // ---- MCP专属逻辑 ----

    /**
     * 占位符替换
     *
     * @param string    原始字符串
     * @param variables 变量表
     * @return 替换后的字符串
     */
    private static String replaceHolder(String string, Map<String, String> variables) {
        if (string == null || variables == null || variables.isEmpty()) {
            return string;
        }
        final var matcher = PLACEHOLDER.matcher(string);
        final var sb = new StringBuilder();
        while (matcher.find()) {
            final var value = Optional.ofNullable(variables.get(matcher.group(1)))
                    .orElseGet(matcher::group);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * MCP原数据转换为MCP传输器
     *
     * @param mcp MCP原数据
     * @return MCP传输器
     */
    private static McpClientTransport toTransport(McpMeta mcp) {
        return switch (mcp.type()) {

            // stdio
            case STDIO -> {
                final var stdio = (McpMeta.Stdio) mcp;
                final var params = ServerParameters.builder(stdio.cmd())
                        .args(stdio.args())
                        .env(stdio.env())
                        .build();
                final var mapper = new JacksonMcpJsonMapper(new ObjectMapper());
                yield new StdioClientTransport(params, mapper);
            }

            // sse
            case SSE -> {
                final var sse = (McpMeta.Http) mcp;
                final var baseUrl = sse.host().toString();
                final var endpoint = sse.endpoint();
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
                final var http = (McpMeta.Http) mcp;
                final var baseUrl = http.host().toString();
                final var endpoint = http.endpoint();
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

    /**
     * MCP传输器重连加固
     *
     * @param name    MCP名称
     * @param factory MCP传输器工厂
     * @return 可重连的MCP传输器
     */
    private static McpClientTransport recoverable(String name, Function<McpJsonMapper, McpClientTransport> factory) {
        return RecoverableMcpClientTransport.newBuilder()
                .name(name)
                .transportFactory(factory)
                .reconnectStrategy(ReconnectStrategies
                        .always()
                        .combine(ReconnectStrategies.delay(Duration.ofSeconds(1)))
                )
                .pingEnabled(true)
                .build();
    }

}
