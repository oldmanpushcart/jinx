package io.github.oldmanpushcart.jinx.core.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * MCP条目
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = McpMeta.Http.class, name = "sse"),
        @JsonSubTypes.Type(value = McpMeta.Http.class, name = "streamable-http"),
        @JsonSubTypes.Type(value = McpMeta.Stdio.class, name = "stdio")
})
public sealed interface McpMeta permits McpMeta.Http, McpMeta.Stdio {

    /**
     * @return 类型
     */
    Type type();

    /**
     * 类型
     */
    enum Type {

        @JsonProperty("stdio")
        STDIO,

        @JsonProperty("sse")
        SSE,

        @JsonProperty("streamable-http")
        STREAMABLE_HTTP

    }

    record Http(
            Type type,
            URL host,
            String endpoint,
            Map<String, String> headers
    ) implements McpMeta {

    }


    record Stdio(
            Type type,
            String cmd,
            List<String> args,
            Map<String, String> env
    ) implements McpMeta {

    }
}
