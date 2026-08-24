package io.github.oldmanpushcart.jinx.extra.cron;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * cron — 管理定时调度任务
 */
@Singleton
class CronCli implements Cli {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final CronDetector detector;

    public CronCli(CronDetector detector) {
        this.detector = detector;
    }

    @Override
    public String command() {
        return "cron";
    }

    @Override
    public List<Item> usage() {
        return List.of(
                new Item("cron", "Manage scheduled tasks."),
                new Item("cron list", "List all scheduled tasks."),
                new Item("cron detail <NAME>", "Show detail of a specific task."),
                new Item("cron reload <NAME>", "Reload a specific task.")
        );
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();
        if (args.isEmpty()) {
            return Mono.just("Usage: cron <list|detail|reload>");
        }

        return switch (args.get(0)) {
            case "list" -> list();
            case "detail" -> detail(args.subList(1, args.size()));
            case "reload" -> reload(args.subList(1, args.size()));
            default -> Mono.just("Unknown subcommand: %s".formatted(args.get(0)));
        };
    }

    private Publisher<String> list() {
        final var metas = detector.list();
        if (metas.isEmpty()) {
            return Mono.just("No scheduled tasks.");
        }
        return Mono.just(metas.stream()
                .map(m -> "%-20s %-30s %s".formatted(
                        m.name(),
                        m.cron(),
                        m.enabled() ? "ENABLED" : "DISABLED"))
                .collect(Collectors.joining("\n")));
    }

    private Publisher<String> detail(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: cron detail <NAME>");
        }
        final var name = args.get(0);
        final var meta = detector.get(name).orElse(null);
        if (meta == null) {
            return Mono.just("Cron task not found: %s".formatted(name));
        }

        return Mono.just("""
                NAME: %s
                CRON: %s
                PROMPT: %s
                MODE: %s
                SESSION: %s
                ENABLED: %s""".formatted(
                meta.name(),
                meta.cron(),
                meta.prompt(),
                meta.mode(),
                meta.sessionId(),
                Boolean.toString(meta.enabled())
        ));
    }

    private Publisher<String> reload(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: cron reload <NAME>");
        }
        final var name = args.get(0);
        return Mono.fromCompletionStage(detector.reload(name))
                .map(_meta -> "Cron task reloaded: %s".formatted(name))
                .onErrorResume(IOException.class, _ex -> Mono.just("Cron task not found: %s".formatted(name)))
                .onErrorResume(ex -> Mono.just("Cron task reload failed: %s".formatted(name)));
    }

}
