package io.github.oldmanpushcart.jinx.core2.mcp;

import java.util.List;
import java.util.Optional;

public interface McpRegistry {

    void register(McpEntity mcp);

    Optional<McpEntity> unregister(String name);

    List<McpEntity> listAll();

    Optional<McpEntity> get(String name);

}
