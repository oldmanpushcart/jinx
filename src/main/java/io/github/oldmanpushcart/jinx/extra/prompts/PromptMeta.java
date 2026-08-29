package io.github.oldmanpushcart.jinx.extra.prompts;

/**
 * 提示词条目
 *
 * @param name    名称（即去掉扩展名的文件名）
 * @param content 静态内容（不做变量替换）
 */
public record PromptMeta(
        String name,
        String content
) {

}
