package io.github.oldmanpushcart.jinx.extra.help.cli;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * help — 显示所有远程命令的帮助信息
 */
@Singleton
class HelpCli implements Cli {

    private final List<Cli> clis;

    public HelpCli(List<Cli> clis) {
        this.clis = clis;
    }

    @Override
    public String command() {
        return "help";
    }

    @Override
    public String description() {
        return "Show help for all remote commands.";
    }

    @Override
    public Publisher<String> execute(Context ctx) {

        final var commands = clis.stream()
                .filter(c -> c.sub() == null)
                .collect(Collectors.toMap(Cli::command, Function.identity()));

        final var subs = clis.stream()
                .filter(c -> c.sub() != null)
                .collect(Collectors.groupingBy(
                        Cli::command,
                        Collectors.toMap(Cli::sub, Function.identity())
                ));

        final var sb = new StringBuilder();
        sb.append("REMOTE COMMANDS:\n");

        // 按命令名排序
        final var sortedCommands = new TreeMap<>(commands);
        for (var entry : sortedCommands.entrySet()) {
            final var cmd = entry.getValue();
            sb.append("  %-20s %s\n".formatted(cmd.command(), cmd.description()));

            // 输出该命令下的子命令
            final var subMap = subs.get(cmd.command());
            if (subMap != null) {
                for (var subEntry : new TreeMap<>(subMap).entrySet()) {
                    sb.append("      %-16s %s\n".formatted(subEntry.getKey(), subEntry.getValue().description()));
                }
            }
        }

        // 输出只有子命令、没有主命令的情况（防御性）
        for (var entry : new TreeMap<>(subs).entrySet()) {
            if (!commands.containsKey(entry.getKey())) {
                for (var subEntry : new TreeMap<>(entry.getValue()).entrySet()) {
                    sb.append("  %-20s %s\n".formatted(
                            entry.getKey() + " " + subEntry.getKey(),
                            subEntry.getValue().description()
                    ));
                }
            }
        }

        return Mono.just(sb.toString());
    }

}
