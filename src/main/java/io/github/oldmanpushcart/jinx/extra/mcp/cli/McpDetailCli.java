package io.github.oldmanpushcart.jinx.extra.mcp.cli;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.jinx.cli.Cli;
import io.github.oldmanpushcart.jinx.extra.mcp.McpDetector;
import io.github.oldmanpushcart.jinx.extra.mcp.McpMeta;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * mcp detail NAME — 查看指定 MCP 的详细信息
 */
@Singleton
class McpDetailCli implements Cli {

    private final McpDetector detector;

    public McpDetailCli(McpDetector detector) {
        this.detector = detector;
    }

    @Override
    public String command() {
        return "mcp";
    }

    @Override
    public String sub() {
        return "detail";
    }

    @Override
    public String description() {
        return "Show detail of a specific MCP";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();
        if (args.isEmpty()) {
            return Mono.just("Usage: mcp detail NAME");
        }

        final var name = args.get(0);
        final var mcp = detector.get(name).orElse(null);
        if (mcp == null) {
            return Mono.just("MCP not found: %s".formatted(name));
        }

        return Mono.just(formatDetail(mcp));
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
