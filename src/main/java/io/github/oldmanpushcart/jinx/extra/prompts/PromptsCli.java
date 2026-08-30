package io.github.oldmanpushcart.jinx.extra.prompts;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * prompts — 管理提示词
 */
@Singleton
class PromptsCli implements Cli {

    private final PromptDetector detector;

    public PromptsCli(PromptDetector detector) {
        this.detector = detector;
    }

    @Override
    public String command() {
        return "prompts";
    }

    @Override
    public List<Item> usage() {
        return List.of(
                new Item("prompts", "List all prompts. (no subcommand defaults to list)"),
                new Item("prompts reload <NAME>", "Reload a specific prompt."),
                new Item("prompts detail <NAME>", "Show content of a specific prompt.")
        );
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();

        // 空参数默认列举全部
        if (args.isEmpty()) {
            return list();
        }

        return switch (args.get(0)) {
            case "reload" -> reload(args.subList(1, args.size()));
            case "detail" -> detail(args.subList(1, args.size()));
            default -> Mono.just("Unknown subcommand: %s".formatted(args.get(0)));
        };
    }

    private Publisher<String> list() {
        final var names = detector.list().stream()
                .map(PromptMeta::name)
                .sorted()
                .collect(Collectors.joining("\n"));
        return Mono.just(names.isBlank() ? "(no prompts)" : names);
    }

    private Publisher<String> reload(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: prompts reload <NAME>");
        }
        final var name = args.get(0);
        return Mono.fromCompletionStage(detector.reload(name))
                .map(_meta -> "Prompt reloaded: %s".formatted(name))
                .onErrorResume(IOException.class, _ex -> Mono.just("Prompt not found: %s".formatted(name)))
                .onErrorResume(ex -> Mono.just("Prompt reload failed: %s, cause: %s".formatted(name, ex.getMessage())));
    }

    private Publisher<String> detail(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: prompts detail <NAME>");
        }
        final var name = args.get(0);
        final var prompt = detector.get(name).orElse(null);
        if (prompt == null) {
            return Mono.just("Prompt not found: %s".formatted(name));
        }
        final var content = prompt.content();
        return Mono.just(content.isBlank() ? "(%s is empty)".formatted(name) : content);
    }

}
