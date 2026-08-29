package io.github.oldmanpushcart.jinx.extra.prompts;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * prompts — 管理提示词
 */
@Singleton
class PromptsCli implements Cli {

    private final Map<PromptPhase, PromptDetector> detectors;

    public PromptsCli(List<PromptDetector> detectorList) {
        this.detectors = detectorList.stream()
                .collect(Collectors.toMap(PromptDetector::phase, detector -> detector));
    }

    @Override
    public String command() {
        return "prompts";
    }

    @Override
    public List<Item> usage() {
        return List.of(
                new Item("prompts", "List all prompts. (no subcommand defaults to list)"),
                new Item("prompts <PHASE>", "List prompts of a phase (preparation|interaction)."),
                new Item("prompts <PHASE> reload <NAME>", "Reload a specific prompt."),
                new Item("prompts <PHASE> detail <NAME>", "Show content of a specific prompt.")
        );
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();

        // 空参数默认列举全部
        if (args.isEmpty()) {
            return listAll();
        }

        final var phase = PromptPhase.of(args.get(0)).orElse(null);
        if (phase == null) {
            return Mono.just("Unknown phase: %s (expect: preparation|interaction)".formatted(args.get(0)));
        }

        // 仅指定阶段：列举该阶段
        if (args.size() == 1) {
            return list(phase);
        }

        final var detector = detectors.get(phase);
        return switch (args.get(1)) {
            case "reload" -> reload(detector, phase, args.subList(2, args.size()));
            case "detail" -> detail(detector, phase, args.subList(2, args.size()));
            default -> Mono.just("Unknown subcommand: %s".formatted(args.get(1)));
        };
    }

    private Publisher<String> listAll() {
        return Mono.just(List.of(PromptPhase.values()).stream()
                .map(phase -> "## %s\n%s".formatted(phase.directory(), namesOf(detectors.get(phase))))
                .collect(Collectors.joining("\n\n")));
    }

    private Publisher<String> list(PromptPhase phase) {
        final var names = namesOf(detectors.get(phase));
        return Mono.just(names.isBlank() ? "(%s is empty)".formatted(phase.directory()) : names);
    }

    private Publisher<String> reload(PromptDetector detector, PromptPhase phase, List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: prompts %s reload <NAME>".formatted(phase.directory()));
        }
        final var name = args.get(0);
        return Mono.fromCompletionStage(detector.reload(name))
                .map(_meta -> "Prompt reloaded: %s/%s".formatted(phase.directory(), name))
                .onErrorResume(IOException.class, _ex -> Mono.just("Prompt not found: %s/%s".formatted(phase.directory(), name)))
                .onErrorResume(ex -> Mono.just("Prompt reload failed: %s/%s, cause: %s".formatted(phase.directory(), name, ex.getMessage())));
    }

    private Publisher<String> detail(PromptDetector detector, PromptPhase phase, List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: prompts %s detail <NAME>".formatted(phase.directory()));
        }
        final var name = args.get(0);
        final var prompt = detector.get(name).orElse(null);
        if (prompt == null) {
            return Mono.just("Prompt not found: %s/%s".formatted(phase.directory(), name));
        }
        final var content = prompt.content();
        return Mono.just(content.isBlank() ? "(%s/%s is empty)".formatted(phase.directory(), name) : content);
    }

    private static String namesOf(PromptDetector detector) {
        return detector.list().stream()
                .map(PromptMeta::name)
                .sorted()
                .collect(Collectors.joining("\n"));
    }

}
