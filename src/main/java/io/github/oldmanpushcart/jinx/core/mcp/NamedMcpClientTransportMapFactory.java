package io.github.oldmanpushcart.jinx.core.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.mcp.RecoverableMcpClientTransport.McpClientTransportFactory;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Factory
public class NamedMcpClientTransportMapFactory {

    @Singleton
    @Named("namedMcpClientTransportMap")
    public Map<String, McpClientTransport> makeNamedMcpClientTransportMap(McpConfig config) {
        if (CommonUtils.isEmpty(config.servers())) {
            return Map.of();
        }
        return config.servers().entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        entry -> {
                            final var server = entry.getValue();
                            return recoverable(mapper -> toTransport(server));
                        })
                );
    }

    private McpClientTransport toTransport(McpConfig.Server server) {

        return switch (server.type()) {

            // stdio
            case STDIO -> {
                final var stdio = (McpConfig.StdioServer) server;
                final var params = ServerParameters.builder(stdio.cmd())
                        .args(stdio.args())
                        .env(stdio.env())
                        .build();
                final var mapper = new JacksonMcpJsonMapper(new ObjectMapper());
                yield new StdioClientTransport(params, mapper);
            }

            // sse
            case SSE -> {
                final var sse = (McpConfig.HttpServer) server;
                //noinspection deprecation
                yield HttpClientSseClientTransport.builder(sse.baseUrl().toString())
                        .sseEndpoint(sse.endpoint())
                        .httpRequestCustomizer((builder, method, endpoint, body, context) -> {
                            if (CommonUtils.isNotEmpty(sse.headers())) {
                                sse.headers().forEach(builder::header);
                            }
                        })
                        .build();
            }

            // streamable-http
            case STREAMABLE_HTTP -> {
                final var http = (McpConfig.HttpServer) server;
                yield HttpClientStreamableHttpTransport.builder(http.baseUrl().toString())
                        .endpoint(http.endpoint())
                        .httpRequestCustomizer((builder, method, endpoint, body, context) -> {
                            if (CommonUtils.isNotEmpty(http.headers())) {
                                http.headers().forEach(builder::header);
                            }
                        })
                        .build();
            }

        };
    }

    private McpClientTransport recoverable(McpClientTransportFactory factory) {
        return RecoverableMcpClientTransport.newBuilder()
                .transportFactory(factory)
                .build();
    }

}
