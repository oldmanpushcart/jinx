package io.github.oldmanpushcart.jinx.core2.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.net.URL;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = McpEntity.Http.class, name = "sse"),
        @JsonSubTypes.Type(value = McpEntity.Http.class, name = "streamable-http"),
        @JsonSubTypes.Type(value = McpEntity.Stdio.class, name = "stdio")
})
public sealed interface McpEntity permits McpEntity.Stdio, McpEntity.Http {

    String name();

    Type type();

    boolean enabled();

    enum Type {

        @JsonProperty("stdio")
        STDIO,

        @JsonProperty("sse")
        SSE,

        @JsonProperty("streamable-http")
        STREAMABLE_HTTP

    }

    record Http(
            String name,
            Type type,
            boolean enabled,
            URL host,
            String endpoint,
            Map<String, String> headers
    ) implements McpEntity {
    }

    record Stdio(
            String name,
            Type type,
            boolean enabled,
            String cmd,
            List<String> args,
            Map<String, String> env
    ) implements McpEntity {
    }

}
