package io.github.oldmanpushcart.jinx.cli;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
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
     * 执行指定命令
     *
     * @param cmdName 命令名称
     * @param args    参数列表
     * @param headers 请求头
     * @return 执行结果
     */
    @Get(uri = "/execute", produces = MediaType.TEXT_PLAIN)
    public Publisher<String> execute(

            @QueryValue("cmd")
            String cmdName,

            @QueryValue(value = "args", defaultValue = "")
            List<String> args,

            HttpHeaders headers

    ) {

        final var filteredArgs = args.stream()
                .filter(a -> !a.isBlank())
                .toList();

        final var sessionId = headers.get("X-Jinx-Session");
        final var ctx = new Cli.Context(filteredArgs, sessionId);

        final var cli = commands.get(cmdName);
        if (cli != null) {
            return cli.execute(ctx);
        }

        throw new NoSuchElementException("Unknown command: %s".formatted(cmdName));
    }

}
