package io.github.oldmanpushcart.jinx.cli;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import org.reactivestreams.Publisher;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CLI命令控制器
 * <p>
 * 自动发现所有 {@link Cli} 实现，按 command() 路由。
 * 子命令分发由各 Cli 实现自行处理。
 * </p>
 */
@Controller("/api/cli")
public class CliController {

    private final Map<String, Cli> commands;

    public CliController(List<Cli> clis) {
        this.commands = clis.stream()
                .collect(Collectors.toMap(Cli::command, Function.identity(), (a, b) -> a));
    }

    /**
     * 表单请求体（form-urlencoded）
     *
     * @param cmd  命令名
     * @param args 参数列表（可重复字段）
     */
    @Introspected
    public record FormBody(String cmd, List<String> args) {
    }

    /**
     * 执行指定命令
     * <p>
     * 表单字段：cmd（命令名）、args（参数，可重复）。
     * </p>
     *
     * @param request HTTP请求
     * @param form    表单请求体
     * @return 执行结果
     */
    @Post(uri = "/execute", consumes = MediaType.APPLICATION_FORM_URLENCODED, produces = MediaType.TEXT_PLAIN)
    public Publisher<String> execute(HttpRequest<Void> request, @Body FormBody form) {

        final var cmdName = form.cmd();
        final var filteredArgs = Optional.ofNullable(form.args()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .filter(a -> !a.isBlank())
                .toList();

        final var sessionId = request.getHeaders().get("X-Jinx-Session");
        final var ctx = new Cli.Context(filteredArgs, sessionId);

        final var cli = commands.get(cmdName);
        if (cli != null) {
            return cli.execute(ctx);
        }

        throw new NoSuchElementException("Unknown command: %s".formatted(cmdName));
    }

}
