package io.github.oldmanpushcart.jinx.core.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import jakarta.inject.Singleton;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ConfigurationProperties("jinx.mcp")
public record McpConfig(
        Map<String, Server> servers
) {

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            visible = true,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = HttpServer.class, name = "sse"),
            @JsonSubTypes.Type(value = HttpServer.class, name = "streamable-http"),
            @JsonSubTypes.Type(value = StdioServer.class, name = "stdio")
    })
    public sealed interface Server permits HttpServer, StdioServer {

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


    public record HttpServer(
            Type type,
            @JsonProperty("base-url") URL baseUrl,
            String endpoint,
            Map<String, String> headers
    ) implements Server {

    }


    public record StdioServer(
            Type type,
            String cmd,
            List<String> args,
            Map<String, String> env
    ) implements Server {

    }

    @Singleton
    record McpServerConfigConverter(ObjectMapper mapper)
            implements TypeConverter<Map<?, ?>, Server> {

        @Override
        public Optional<Server> convert(Map<?, ?> map, Class<Server> targetType, ConversionContext context) {
            try {
                final var config = mapper.convertValue(map, Server.class);
                return Optional.of(config);
            } catch (Exception e) {
                context.reject(map, e);
                return Optional.empty();
            }
        }

    }

}
