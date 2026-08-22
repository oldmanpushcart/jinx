package io.github.oldmanpushcart.jinx.extra.mcp.cli;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;

/**
 * mcp 主命令（fallback）
 */
@Singleton
class McpCli implements Cli {

    @Override
    public String command() {
        return "mcp";
    }

    @Override
    public String description() {
        return "Manage MCP services.";
    }

    @Override
    public org.reactivestreams.Publisher<String> execute(Context ctx) {
        return reactor.core.publisher.Mono.just("Usage: mcp <list|detail|reload>");
    }

}
