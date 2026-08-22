package io.github.oldmanpushcart.jinx.extra.mcp.cli;

import io.github.oldmanpushcart.jinx.cli.Cli;
import io.github.oldmanpushcart.jinx.extra.mcp.McpDetector;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * mcp reload NAME — 重新加载指定 MCP
 */
@Singleton
class McpReloadCli implements Cli {

    private final McpDetector detector;

    public McpReloadCli(McpDetector detector) {
        this.detector = detector;
    }

    @Override
    public String command() {
        return "mcp";
    }

    @Override
    public String sub() {
        return "reload";
    }

    @Override
    public String description() {
        return "Reload a specific MCP";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();
        if (args.isEmpty()) {
            return Mono.just("Usage: mcp reload NAME");
        }
        return Mono.fromCompletionStage(detector.reload(args.get(0)))
                .map(meta -> "MCP reloaded: %s".formatted(meta.name()));
    }

}
