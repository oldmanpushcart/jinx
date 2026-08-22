package io.github.oldmanpushcart.jinx.extra.skill.cli;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.jinx.cli.Cli;
import io.github.oldmanpushcart.jinx.extra.skill.SkillDetector;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * skill detail NAME — 查看指定 SKILL 的详细信息
 */
@Singleton
class SkillDetailCli implements Cli {

    private final SkillDetector detector;

    public SkillDetailCli(SkillDetector detector) {
        this.detector = detector;
    }

    @Override
    public String command() {
        return "skill";
    }

    @Override
    public String sub() {
        return "detail";
    }

    @Override
    public String description() {
        return "Show detail of a specific SKILL";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();
        if (args.isEmpty()) {
            return Mono.just("Usage: skill detail NAME");
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

}
