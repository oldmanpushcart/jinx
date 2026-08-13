package io.github.oldmanpushcart.jinx.core.mcp;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface McpRegistry {

    CompletionStage<Void> upsert(String name, McpMeta meta);

    Optional<McpMeta> remove(String name);

}
