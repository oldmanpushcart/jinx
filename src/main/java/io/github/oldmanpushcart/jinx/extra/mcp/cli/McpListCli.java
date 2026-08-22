package io.github.oldmanpushcart.jinx.extra.mcp.cli;

import io.github.oldmanpushcart.jinx.cli.Cli;
import io.github.oldmanpushcart.jinx.extra.mcp.McpDetector;
import io.github.oldmanpushcart.jinx.extra.mcp.McpMeta;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * mcp list — 列出所有已加载的 MCP
 */
@Singleton
class McpListCli implements Cli {

    private final McpDetector detector;

    public McpListCli(McpDetector detector) {
        this.detector = detector;
    }

    @Override
    public String command() {
        return "mcp";
    }

    @Override
    public String sub() {
        return "list";
    }

    @Override
    public String description() {
        return "List all loaded MCPs";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        return Mono.just(detector.list().stream()
                .map(McpMeta::name)
                .collect(Collectors.joining("\n")));
    }

}
