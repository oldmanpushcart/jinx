package io.github.oldmanpushcart.jinx.extra.skill.cli;

import io.github.oldmanpushcart.jinx.cli.Cli;
import io.github.oldmanpushcart.jinx.extra.skill.SkillDetector;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * skill list — 列出所有已加载的 SKILL
 */
@Singleton
class SkillListCli implements Cli {

    private final SkillDetector detector;

    public SkillListCli(SkillDetector detector) {
        this.detector = detector;
    }

    @Override
    public String command() {
        return "skill";
    }

    @Override
    public String sub() {
        return "list";
    }

    @Override
    public String description() {
        return "List all loaded SKILLs";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        return Mono.just(detector.list().stream()
                .map(skill -> skill.header().name())
                .collect(Collectors.joining("\n")));
    }

}
