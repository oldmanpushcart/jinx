package io.github.oldmanpushcart.jinx.extra.skill;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.skill.Skill;
import io.github.oldmanpushcart.jinx.Constants;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface SkillDetector {

    Path SKILLS_DIR = Constants.DATA.resolve("skills").normalize().toAbsolutePath();

    List<Skill> list();

    Optional<Skill> get(String name);

    CompletionStage<Skill> reload(String name);

}
