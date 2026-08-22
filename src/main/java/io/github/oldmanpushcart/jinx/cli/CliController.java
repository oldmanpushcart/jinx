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
 * 自动发现所有 {@link Cli} 实现，提供命令路由。
 * </p>
 */
@Controller("/api/cli")
public class CliController {

    private final Map<String, Cli> commands;
    private final Map<String, Map<String, Cli>> subs;

    public CliController(List<Cli> clis) {

        // 主命令：sub() == null
        this.commands = clis.stream()
                .filter(c -> c.sub() == null)
                .collect(Collectors.toMap(Cli::command, Function.identity()));

        // 子命令：sub() != null，按 command 分组
        this.subs = clis.stream()
                .filter(c -> c.sub() != null)
                .collect(Collectors.groupingBy(
                        Cli::command,
                        Collectors.toMap(Cli::sub, Function.identity())
                ));
    }

    /**
     * 执行指定命令
     *
     * @param cmdName   命令名称
     * @param args      参数列表
     * @param headers   请求头
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

        // 过滤空参数
        final var filteredArgs = args.stream()
                .filter(a -> !a.isBlank())
                .toList();

        // 构建执行上下文
        final var sessionId = headers.get("X-Jinx-Session");
        final var ctx = new Cli.Context(filteredArgs, sessionId);

        // 1. 尝试匹配子命令
        if (!filteredArgs.isEmpty()) {
            final var subMap = subs.get(cmdName);
            if (subMap != null) {
                final var subCli = subMap.get(filteredArgs.get(0));
                if (subCli != null) {
                    return subCli.execute(new Cli.Context(filteredArgs.subList(1, filteredArgs.size()), sessionId));
                }
            }
        }

        // 2. 回退到主命令
        final var mainCli = commands.get(cmdName);
        if (mainCli != null) {
            return mainCli.execute(ctx);
        }

        // 3. 未知命令
        throw new NoSuchElementException("Unknown command: %s".formatted(cmdName));
    }

}
