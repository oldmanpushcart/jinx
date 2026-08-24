package io.github.oldmanpushcart.jinx.extra.skill;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.skill.Skill;
import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.core.detector.Detector;

import java.nio.file.Path;

/**
 * SKILL探测器
 */
public interface SkillDetector extends Detector<Skill> {

    Path SKILLS_DIR = Constants.DATA.resolve("skills").normalize().toAbsolutePath();

}
