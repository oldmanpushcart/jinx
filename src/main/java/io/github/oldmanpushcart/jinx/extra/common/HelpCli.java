package io.github.oldmanpushcart.jinx.extra.common;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
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
    public List<Item> usage() {
        return List.of(new Item("help", "Show help for all remote commands."));
    }

    @Override
    public Publisher<String> execute(Context ctx) {

        // 按 command() 分组，收集所有 Item
        final var groups = clis.stream()
                .collect(Collectors.groupingBy(
                        Cli::command,
                        TreeMap::new,
                        Collectors.flatMapping(
                                c -> c.usage().stream(),
                                Collectors.toList()
                        )
                ));

        // 全局列宽：所有 Item name 取最大长度 + 4 空格间距
        final var colW = groups.values().stream()
                .flatMap(List::stream)
                .mapToInt(i -> i.name().length())
                .max().orElse(12) + 4;

        final var fmt = "  %-" + colW + "s%s\n";
        final var stringBuf = new StringBuilder();
        stringBuf.append("USAGE:\n");

        var first = true;
        for (final var entry : groups.entrySet()) {

            // 组间空一行
            if (!first) {
                stringBuf.append('\n');
            }
            first = false;

            // 组内按 name 排序
            entry.getValue().stream()
                    .sorted(Comparator.comparing(Item::name))
                    .forEach(item -> {
                        final var row = fmt.formatted(item.name(), item.description());
                        stringBuf.append(row);
                    });

        }

        return Mono.just(stringBuf.toString());
    }

}
