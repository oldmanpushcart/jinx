package io.github.oldmanpushcart.jinx.core.skill;

/**
 * Skill 配置文件条目
 *
 * @param name        Skill 名称（唯一标识）
 * @param enabled     是否启用
 * @param description Skill 描述
 * @param content     Skill 的 Markdown 内容
 */
public record SkillFileEntry(
        String name,
        boolean enabled,
        String description,
        String content
) {
}
