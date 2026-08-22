package io.github.oldmanpushcart.jinx.extra.skill;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * skill — 管理 SKILL 定义
 */
@Singleton
class SkillCli implements Cli {

    private final SkillDetector detector;

    public SkillCli(SkillDetector detector) {
        this.detector = detector;
    }

    @Override
    public String command() {
        return "skill";
    }

    @Override
    public List<Item> usage() {
        return List.of(
                new Item("skill", "Manage SKILL definitions."),
                new Item("skill list", "List all loaded SKILLs."),
                new Item("skill detail <NAME>", "Show detail of a specific SKILL."),
                new Item("skill reload <NAME>", "Reload a specific SKILL.")
        );
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();
        if (args.isEmpty()) {
            return Mono.just("Usage: skill <list|detail|reload>");
        }

        return switch (args.get(0)) {
            case "list" -> list();
            case "detail" -> detail(args.subList(1, args.size()));
            case "reload" -> reload(args.subList(1, args.size()));
            default -> Mono.just("Unknown subcommand: %s".formatted(args.get(0)));
        };
    }

    private Publisher<String> list() {
        return Mono.just(detector.list().stream()
                .map(skill -> skill.header().name())
                .collect(Collectors.joining("\n")));
    }

    private Publisher<String> detail(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: skill detail <NAME>");
        }
        final var name = args.get(0);
        final var skill = detector.get(name).orElse(null);
        if (skill == null) {
            return Mono.just("SKILL not found: %s".formatted(name));
        }
        return Mono.just(PromptTemplate.newBuilder()
                .template("""
                        HOME: ${skill.home}
                        NAME: ${skill.name}
                        DESCRIPTION: ${skill.description}
                        BODY:
                        ${skill.body}
                        """)
                .variable("skill.name", skill.header().name())
                .variable("skill.description", skill.header().description())
                .variable("skill.home", skill.home())
                .variable("skill.body", skill.body())
                .build()
                .render());
    }

    private Publisher<String> reload(List<String> args) {
        if (args.isEmpty()) {
            return Mono.just("Usage: skill reload <NAME>");
        }
        return Mono.fromCompletionStage(detector.reload(args.get(0)))
                .map(skill -> "SKILL reloaded: %s".formatted(skill.header().name()));
    }

}
