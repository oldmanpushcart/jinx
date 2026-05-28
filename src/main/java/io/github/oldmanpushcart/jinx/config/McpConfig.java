package io.github.oldmanpushcart.jinx.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;

import java.net.URL;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("jinx.mcp")
public record McpConfig(
        Map<String, McpServerConfig> servers
) {

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            visible = true,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = McpConfig.HttpMcpServerConfig.class, name = "sse"),
            @JsonSubTypes.Type(value = McpConfig.HttpMcpServerConfig.class, name = "streamable-http"),
            @JsonSubTypes.Type(value = McpConfig.StdioMcpServerConfig.class, name = "stdio")
    })
    public sealed interface McpServerConfig permits McpConfig.HttpMcpServerConfig, McpConfig.StdioMcpServerConfig {

        @JsonProperty
        Type type();

        enum Type {

            @JsonProperty("stdio")
            STDIO,

            @JsonProperty("sse")
            SSE,

            @JsonProperty("streamable-http")
            STREAMABLE_HTTP

        }

    }


    public record HttpMcpServerConfig(
            Type type,
            URL url,
            Map<String, String> headers
    ) implements McpServerConfig {

    }


    public record StdioMcpServerConfig(
            Type type,
            String command,
            List<String> args,
            Map<String, String> env
    ) implements McpServerConfig {

    }

    @Factory
    public static class McpConfigFactory {

        @Singleton
        @Bean
        // 直接注入 YAML 中 jinx.mcp.servers 下的原始 Map 数据
        public McpConfig mcpConfig(@Property(name = "jinx.mcp.servers") Map<String, Object> rawServers) throws Exception {
            // 利用 Jackson 的 ObjectMapper 手动将 Map 转换成你的多态 Record 对象
            ObjectMapper mapper = new ObjectMapper();
            // 将 Map 转成 JSON 字符串再转回目标对象，触发 Jackson 的 @JsonTypeInfo 多态解析
            Map<String, McpServerConfig> servers = mapper.convertValue(
                    rawServers,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, McpServerConfig.class)
            );
            return new McpConfig(servers);
        }

    }
}
