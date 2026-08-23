package io.github.oldmanpushcart.jinx.extra.cron;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

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
                new Item("cron delete <NAME>", "Delete a scheduled task."),
                new Item("cron pause <NAME>", "Pause a scheduled task."),
                new Item("cron resume <NAME>", "Resume a paused task.")
        );
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();
        if (args.isEmpty()) {
            return Mono.just("Usage: cron <list|detail|delete|pause|resume>");
        }

        return switch (args.get(0)) {
            case "list" -> list();
            case "detail" -> detail(args.subList(1, args.size()));
            case "delete" -> delete(args.subList(1, args.size()));
            case "pause" -> pause(args.subList(1, args.size()));
            case "resume" -> resume(args.subList(1, args.size()));
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
                ENABLED: %s""".formatted(
                meta.name(),
                meta.cron(),
                meta.prompt(),
                meta.mode(),
                Boolean.toString(meta.enabled())
        ));
    }

    private Publisher<String> delete(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: cron delete <NAME>");
        }
        final var name = args.get(0);
        final var removed = detector.remove(name);
        if (removed == null) {
            return Mono.just("Cron task not found: %s".formatted(name));
        }
        return Mono.just("Cron task deleted: %s".formatted(name));
    }

    private Publisher<String> pause(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: cron pause <NAME>");
        }
        final var name = args.get(0);
        final var paused = detector.pause(name);
        if (paused == null) {
            return Mono.just("Cron task not found: %s".formatted(name));
        }
        return Mono.just("Cron task paused: %s".formatted(name));
    }

    private Publisher<String> resume(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: cron resume <NAME>");
        }
        final var name = args.get(0);
        final var resumed = detector.resume(name);
        if (resumed == null) {
            return Mono.just("Cron task not found: %s".formatted(name));
        }
        return Mono.just("Cron task resumed: %s".formatted(name));
    }

}
