package io.github.oldmanpushcart.jinx.controller;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.jinx.core.mcp.McpDetector;
import io.github.oldmanpushcart.jinx.core.mcp.McpMeta;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Controller("/api/mcp")
public class JinxMcpController {

    private final McpDetector detector;

    public JinxMcpController(McpDetector detector) {
        this.detector = detector;
    }

    @Get(value = "/list", produces = MediaType.TEXT_PLAIN)
    public String list() {
        return detector.list().stream()
                .map(McpMeta::name)
                .collect(Collectors.joining("\n"));
    }

    @Get(value = "/detail", produces = MediaType.TEXT_PLAIN)
    public String detail(

            @QueryValue("name")
            String name

    ) {

        final var mcp = detector.get(name).orElse(null);
        if (mcp == null) {
            return "MCP not found: %s".formatted(name);
        }

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

    @Get(value = "/reload", produces = MediaType.TEXT_PLAIN)
    public CompletionStage<String> reload(

            @QueryValue("name")
            String name

    ) {
        return detector.reload(name)
                .thenApply(meta -> "MCP reloaded: %s".formatted(meta.name()));
    }

}
