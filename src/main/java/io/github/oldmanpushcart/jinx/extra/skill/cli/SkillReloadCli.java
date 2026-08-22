package io.github.oldmanpushcart.jinx.extra.skill.cli;

import io.github.oldmanpushcart.jinx.cli.Cli;
import io.github.oldmanpushcart.jinx.extra.skill.SkillDetector;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * skill reload NAME — 重新加载指定 SKILL
 */
@Singleton
class SkillReloadCli implements Cli {

    private final SkillDetector detector;

    public SkillReloadCli(SkillDetector detector) {
        this.detector = detector;
    }

    @Override
    public String command() {
        return "skill";
    }

    @Override
    public String sub() {
        return "reload";
    }

    @Override
    public String description() {
        return "Reload a specific SKILL";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();
        if (args.isEmpty()) {
            return Mono.just("Usage: skill reload NAME");
        }
        return Mono.fromCompletionStage(detector.reload(args.get(0)))
                .map(skill -> "SKILL reloaded: %s".formatted(skill.header().name()));
    }

}
