package io.github.oldmanpushcart.jinx.core.skill;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.nio.file.Path;
import java.util.List;

@ConfigurationProperties("jinx.skill")
public record SkillConfig(
        List<Path> directories
) {
}
