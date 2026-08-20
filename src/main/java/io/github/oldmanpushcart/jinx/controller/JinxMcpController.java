package io.github.oldmanpushcart.jinx.controller;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import io.github.oldmanpushcart.jinx.core.mcp.McpDetector;
import io.github.oldmanpushcart.jinx.core.mcp.McpMeta;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

@Controller("/api/mcp")
public class JinxMcpController {

    private final McpDetector detector;

    public JinxMcpController(McpDetector detector) {
        this.detector = detector;
    }

    @Get(value = "/list", produces = MediaType.TEXT_PLAIN)
    public String list() {
        final var header = new String[]{"NAME", "TYPE", "PATH"};
        final var body = detector.list().stream()
                .map(mcp -> new String[]{
                        mcp.name(),
                        mcp.type().toString().toLowerCase(),
                        McpDetector.MCP_DIR
                                .resolve(mcp.name())
                                .normalize()
                                .toAbsolutePath()
                                .toString()
                })
                .toArray(String[][]::new);
        return AsciiTable.getTable(header, body);
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
        final var header = new Column[]{
                new Column().header("ITEM").dataAlign(HorizontalAlign.RIGHT),
                new Column().header("VALUE").dataAlign(HorizontalAlign.LEFT)
        };
        final var body = new String[5][2];
        body[0] = new String[]{"NAME", mcp.name()};
        body[1] = new String[]{"TYPE", mcp.type().toString()};
        if (mcp instanceof McpMeta.Http http) {
            body[2] = new String[]{"HOST", http.host().toString()};
            body[3] = new String[]{"ENDPOINT", http.endpoint()};
            body[4] = new String[]{"HEADERS", Objects.toString(http.headers())};
        } else if (mcp instanceof McpMeta.Stdio stdio) {
            body[2] = new String[]{"CMD", stdio.cmd()};
            body[3] = new String[]{"ARGS", Objects.toString(stdio.args())};
            body[4] = new String[]{"ENV", Objects.toString(stdio.env())};
        }
        return AsciiTable.getTable(header, body);
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
