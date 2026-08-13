package io.github.oldmanpushcart.jinx.core.mcp.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.jinx.core.mcp.McpMeta;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

class McpHelper {

    private static final Pattern PLACE_HOLDER_PATTERN = compile("\\$\\{([^}]+)}");

    public static String resolveEnvVars(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        final var matcher = PLACE_HOLDER_PATTERN.matcher(value);
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

    public static McpClientTransport toTransport(McpMeta mcp) {
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
                final var baseUrl = McpHelper.resolveEnvVars(sse.host().toString());
                final var endpoint = McpHelper.resolveEnvVars(sse.endpoint());
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
                final var baseUrl = McpHelper.resolveEnvVars(http.host().toString());
                final var endpoint = McpHelper.resolveEnvVars(http.endpoint());
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

}
