package io.github.oldmanpushcart.jinx.extra.skill.cli;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;

/**
 * skill 主命令（fallback）
 */
@Singleton
class SkillCli implements Cli {

    @Override
    public String command() {
        return "skill";
    }

    @Override
    public String description() {
        return "Manage SKILL definitions.";
    }

    @Override
    public org.reactivestreams.Publisher<String> execute(Context ctx) {
        return reactor.core.publisher.Mono.just("Usage: skill <list|detail|reload>");
    }

}
