package io.github.oldmanpushcart.jinx.core.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.ToolSubscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport.ReconnectStrategies;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.jinx.JinxConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
class McpLatcher {

    private static final String MCP_FILE_SUFFIX = ".mcp.json";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JinxConfig config;
    private final Toolbox toolbox;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public McpLatcher(JinxConfig config, Toolbox toolbox) {
        this.config = config;
        this.toolbox = toolbox;
    }

    /**
     * 检测MCP并完成注册
     *
     * @throws IOException 检测失败
     */
    private synchronized void detect() throws IOException {

        final var directory = config.dataspace().resolve("mcp");
        if (!Files.isDirectory(directory)) {
            logger.warn("{} ignored by directory not exist. path={}", this, directory);
            return;
        }

        final var upserts = new ArrayList<Info>();
        final var removes = new ArrayList<>(entries.keySet());
        try (final var __stream__ = Files.list(directory)) {
            __stream__
                    .filter(Files::isRegularFile)
                    .filter(McpLatcher::isMcpFile)
                    .forEach(mcpPath -> {

                        final var mcpFilename = mcpPath.getFileName().toString();
                        final var mcpName = mcpFilename.substring(0, mcpFilename.length() - MCP_FILE_SUFFIX.length());

                        try {

                            final var version = Files.getLastModifiedTime(mcpPath).toInstant();
                            final var exist = entries.get(mcpName);

                            // 如果版本一致则跳过
                            if (null != exist && Objects.equals(version, exist.version())) {
                                removes.remove(mcpName);
                                logger.debug("{} ignored by version not changed. mcp={};path={}",
                                        this,
                                        mcpName,
                                        mcpPath
                                );
                                return;
                            }

                            // 解析出原数据
                            final var mcpMeta = parseFromFile(mcpPath);

                            // MCP元数据中的名字必须和文件名解析出来的一致
                            if (!Objects.equals(mcpName, mcpMeta.name())) {
                                logger.warn("{} ignored by name not matched. expect={};actual={};path={}",
                                        this,
                                        mcpName,
                                        mcpMeta.name(),
                                        mcpPath
                                );
                                return;
                            }

                            // 到了这一步，说明新增或者更新
                            upserts.add(new Info(mcpMeta, version));

                        } catch (Exception ex) {
                            logger.warn("{} ignored by error! mcp={};path={};",
                                    this,
                                    mcpName,
                                    mcpPath
                            );
                        }

                    });
        }

        // 取消对已失效的MCP的订阅
        removes.forEach(mcpName -> {
            final var exist = entries.remove(mcpName);
            if (null != exist) {
                IOUtils.closeQuietly(exist);
                logger.debug("{} unregister mcp by remove. mcp={}", this, mcpName);
            }
        });

        // 更新已变更的MCP订阅
        upserts.forEach(info -> {
            final var mcpMeta = info.meta();
            final var exist = entries.remove(mcpMeta.name());

            // 先清理已注册的
            if (null != exist) {
                IOUtils.closeQuietly(exist);
            }


            logger.debug("{} unregister mcp by upsert. mcp={}", this, mcpMeta.name());

            // 再注册新的
            final var transport = recoverable(m -> toTransport(mcpMeta));
            toolbox.subscribeMcp(mcpMeta.name(), transport)
                    .thenAccept(subscription -> {
                        final var entry = new Entry(mcpMeta.name(), mcpMeta, info.version(), subscription, transport);
                        entries.put(mcpMeta.name(), entry);
                    })
                    .handle((u, ex) -> {
                        if (null != ex) {
                            logger.warn("{} register mcp by upsert failed! mcp={};", this, mcpMeta.name(), ex);
                        } else {
                            logger.debug("{} register mcp by upsert success. mcp={};", this, mcpMeta.name());
                        }
                        return null;
                    })
                    .toCompletableFuture()
                    .join();

        });

    }

    /**
     * 是否是MCP文件
     *
     * @param mcpPath MCP路径
     * @return TRUE | FALSE
     */
    private static boolean isMcpFile(Path mcpPath) {
        return mcpPath.getFileName().toString().endsWith(MCP_FILE_SUFFIX);
    }

    /**
     * 从文件解析MCP元数据
     *
     * @param mcpPath MCP文件路径
     * @return MCP元数据
     * @throws IOException 解析失败
     */
    private static McpMeta parseFromFile(Path mcpPath) throws IOException {
        final var rawMcpJson = Files.readString(mcpPath, UTF_8);
        final var mcpJson = replaceHolder(rawMcpJson, System.getenv());
        return JacksonJsonUtils.toObject(mcpJson, McpMeta.class);
    }


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
                    .map(Matcher::quoteReplacement)
                    .orElseGet(matcher::group);
            matcher.appendReplacement(sb, value);
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
     * @param factory MCP传输器工厂
     * @return 可重连的MCP传输器
     */
    private static McpClientTransport recoverable(Function<McpJsonMapper, McpClientTransport> factory) {
        return RecoverableMcpClientTransport.newBuilder()
                .transportFactory(factory)
                .reconnectStrategy(ReconnectStrategies
                        .always()
                        .combine(ReconnectStrategies.delay(Duration.ofSeconds(1)))
                )
                .pingEnabled(true)
                .build();
    }

    @PostConstruct
    void init() {
        try {
            detect();
        } catch (IOException e) {
            logger.warn("{}/init detect ignored by error!", this, e);
        }
    }

    @Scheduled(fixedRate = "10s")
    void scan() {
        try {
            detect();
        } catch (IOException e) {
            logger.warn("{}/scan detect ignored by error!", this, e);
        }
    }

    @Override
    public String toString() {
        return "jinx://mcp/latcher";
    }

    /**
     * 注册信息
     *
     * @param meta    MCP原数据
     * @param version MCP版本
     */
    private record Info(McpMeta meta, Instant version) {

    }

    private record Entry(
            String name,
            McpMeta meta,
            Instant version,
            ToolSubscription subscription,
            McpClientTransport transport
    ) implements AutoCloseable {

        @Override
        public void close() {
            IOUtils.closeQuietly(subscription);
            transport.close();
        }
    }

}
