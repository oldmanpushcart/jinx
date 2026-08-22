package io.github.oldmanpushcart.jinx.extra.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * mcp — 管理 MCP 服务
 */
@Singleton
class McpCli implements Cli {

    private final McpDetector detector;

    public McpCli(McpDetector detector) {
        this.detector = detector;
    }

    @Override
    public String command() {
        return "mcp";
    }

    @Override
    public List<Item> usage() {
        return List.of(
                new Item("mcp", "Manage MCP services."),
                new Item("mcp list", "List all loaded MCPs."),
                new Item("mcp detail <NAME>", "Show detail of a specific MCP."),
                new Item("mcp reload <NAME>", "Reload a specific MCP.")
        );
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();
        if (args.isEmpty()) {
            return Mono.just("Usage: mcp <list|detail|reload>");
        }

        return switch (args.get(0)) {
            case "list" -> list();
            case "detail" -> detail(args.subList(1, args.size()));
            case "reload" -> reload(args.subList(1, args.size()));
            default -> Mono.just("Unknown subcommand: %s".formatted(args.get(0)));
        };
    }

    private Publisher<String> list() {
        return Mono.just(detector.list().stream()
                .map(McpMeta::name)
                .collect(Collectors.joining("\n")));
    }

    private Publisher<String> detail(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: mcp detail <NAME>");
        }
        final var name = args.get(0);
        final var mcp = detector.get(name).orElse(null);
        if (mcp == null) {
            return Mono.just("MCP not found: %s".formatted(name));
        }
        return Mono.just(formatDetail(mcp));
    }

    private Publisher<String> reload(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: mcp reload <NAME>");
        }
        return Mono.fromCompletionStage(detector.reload(args.get(0)))
                .map(meta -> "MCP reloaded: %s".formatted(meta.name()));
    }

    private static String formatDetail(McpMeta mcp) {
        return PromptTemplate.newBuilder()
                .template("""
                        NAME: ${mcp.name}
                        TYPE: ${mcp.type}
                        ${mcp.body}
                        """)
                .variable("mcp.name", mcp.name())
                .variable("mcp.type", mcp.type().toString().toLowerCase())
                .variable("mcp.body", PromptTemplate.newBuilder()
                        .building(bodyBuilder -> {
                            if (mcp instanceof McpMeta.Http http) {
                                bodyBuilder
                                        .template("""
                                                HTTP.HOST: ${http.host}
                                                HTTP.ENDPOINT: ${http.endpoint}
                                                HTTP.HEADERS: ${http.headers}
                                                """)
                                        .variable("http.host", http.host())
                                        .variable("http.endpoint", http.endpoint())
                                        .variable("http.headers", Objects.toString(http.headers()));
                            }
                            if (mcp instanceof McpMeta.Stdio stdio) {
                                bodyBuilder
                                        .template("""
                                                STDIO.CMD: ${stdio.cmd}
                                                STDIO.ARGS: ${stdio.args}
                                                STDIO.ENV: ${stdio.env}
                                                """)
                                        .variable("stdio.cmd", stdio.cmd())
                                        .variable("stdio.args", Objects.toString(stdio.args()))
                                        .variable("stdio.env", Objects.toString(stdio.env()));
                            }
                        })
                        .build())
                .build()
                .render();
    }

}
