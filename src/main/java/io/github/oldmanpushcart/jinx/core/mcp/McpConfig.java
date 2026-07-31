package io.github.oldmanpushcart.jinx.core.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.context.annotation.ConfigurationProperties;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("jinx.mcp")
public record McpConfig(
        Path configDirectory
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

}
