package io.github.oldmanpushcart.jinx.extra.mcp.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.ToolSubscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp.RecoverableMcpClientTransport.ReconnectStrategies;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
class McpDetectorImpl implements McpDetector {

    private static final String MCP_FILE_SUFFIX = ".mcp.json";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Toolbox toolbox;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public McpDetectorImpl(Toolbox toolbox) {
        this.toolbox = toolbox;
    }

    /**
     * 检测MCP并完成注册
     *
     * @throws IOException 检测失败
     */
    private synchronized void detect() throws IOException {

        final var directory = MCP_DIR;
        if (!Files.isDirectory(directory)) {
            logger.warn("{} ignored by directory not exist. path={}", this, directory);
            return;
        }

        final var removes = new ArrayList<>(entries.keySet());

        /*
         * 加载MCP目录下所有的元数据文件
         * 阻塞遍历MCP目录，找出所有的mcp元数据文件，逐一加载。
         */
        try (final var __stream__ = Files.list(directory)) {
            __stream__
                    .filter(Files::isRegularFile)
                    .filter(McpDetectorImpl::isMcpFile)
                    .forEach(mcpPath -> {

                        try {
                            reload(mcpPath)
                                    .thenAccept(mcpMeta -> removes.remove(mcpMeta.name()))
                                    .toCompletableFuture()
                                    .join();
                        } catch (Exception ex) {
                            final var cause = CompletableFutureUtils.unwrapEx(ex);
                            logger.warn("{} detect ignored. path={}", this, mcpPath, cause);
                        }

                    });
        }

        // 取消对已失效的MCP的订阅
        removes.forEach(this::remove);

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

    @Scheduled(fixedDelay = "10s")
    void scan() {
        try {
            detect();
        } catch (IOException e) {
            logger.warn("{}/scan detect ignored by error!", this, e);
        }
    }

    @Override
    public String toString() {
        return "jinx://mcp/detector";
    }

    @Override
    public List<McpMeta> list() {
        return entries.values()
                .stream()
                .map(Entry::meta)
                .toList();
    }

    @Override
    public Optional<McpMeta> get(String name) {
        return Optional.ofNullable(entries.get(name))
                .map(Entry::meta);
    }

    /**
     * 重加载指定的MCP配置
     *
     * @param mcpPath MCP文件路径
     * @return MCP元数据
     */
    private CompletionStage<McpMeta> reload(Path mcpPath) {
        final var mcpFilename = mcpPath.getFileName().toString();
        final var mcpName = mcpFilename.substring(0, mcpFilename.length() - MCP_FILE_SUFFIX.length());
        return CompletableFuture.completedStage(null)
                .thenCompose(_u -> {

                    try {

                        /*
                         * 检查已注册的版本和当前版本是否一致
                         * 如果一致就不用重新加载了
                         */
                        final var mcpVersion = Files.getLastModifiedTime(mcpPath).toInstant();
                        final var exist = entries.get(mcpName);
                        if (null != exist && Objects.equals(exist.version(), mcpVersion)) {
                            return CompletableFuture.completedStage(exist.meta());
                        }

                        /*
                         * 检查MCP名称是否和期望的一致
                         * 要求是MCP文件名中的名称必须和MCP元数据中的名称一致
                         *
                         * amap.mc.json为例
                         * 期待的名称就是：amap，元数据中配置的名称也必须是amap
                         */
                        final var mcpMeta = parseFromFile(mcpPath);
                        if (!Objects.equals(mcpName, mcpMeta.name())) {
                            return CompletableFuture.failedStage(new RuntimeException("MCP name mismatch! expect: %s, actual: %s".formatted(
                                    mcpName,
                                    mcpMeta.name()
                            )));
                        }

                        // 重新进行注册
                        final var transport = recoverable(m -> toTransport(mcpMeta));
                        return toolbox.subscribeMcp(mcpMeta.name(), transport)

                                /*
                                 * 连接重构后就需要重新进行注册
                                 * 注册的时候回逐出之前已注册的MCP并关闭，所以这里就算发生了并发，也只会有最后一个注册成功的生效。
                                 */
                                .thenApply(subscription -> {
                                    final var entry = new Entry(mcpName, mcpMeta, mcpVersion, subscription, transport);
                                    final var expired = entries.put(mcpName, entry);
                                    if (null != expired) {
                                        IOUtils.closeQuietly(expired);
                                    }
                                    return mcpMeta;
                                })

                                /*
                                 * 连接失败或者注册失败，则需要关闭之前已创建的transport，
                                 * 避免资源泄漏。
                                 */
                                .whenComplete((uu, ex) -> {
                                    if (null != ex) {
                                        logger.warn("{} register mcp failed by reload! mcp={};", this, mcpMeta.name(), ex);
                                        transport.close();
                                    } else {
                                        logger.debug("{} register mcp success by reload. mcp={};", this, mcpMeta.name());
                                    }
                                });
                    } catch (Exception e) {
                        return CompletableFuture.failedFuture(e);
                    }
                });

    }

    @Override
    public CompletionStage<McpMeta> reload(String name) {
        final var mcpPath = MCP_DIR
                .resolve("%s.mcp.json".formatted(name));
        if (!Files.exists(mcpPath)) {
            return CompletableFuture.failedStage(new IOException("MCP %s not exist!".formatted(name)));
        }
        return reload(mcpPath);
    }

    @Override
    public synchronized McpMeta remove(String name) {
        final var exist = entries.remove(name);
        if (null != exist) {
            IOUtils.closeQuietly(exist);
            return exist.meta();
        }
        return null;
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
