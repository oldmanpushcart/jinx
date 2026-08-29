package io.github.oldmanpushcart.jinx.extra.prompts;

import java.util.Optional;

/**
 * 提示词阶段
 * <p>
 * 对应 Agent 生命周期中的两类钩子：
 * {@code preparation} 对应 PreparationHook，{@code interaction} 对应 InteractionHook。
 * </p>
 */
public enum PromptPhase {

    /**
     * 准备阶段（PreparationHook）
     */
    PREPARATION("preparation"),

    /**
     * 交互阶段（InteractionHook）
     */
    INTERACTION("interaction");

    private final String directory;

    PromptPhase(String directory) {
        this.directory = directory;
    }

    /**
     * @return 对应的数据目录名
     */
    public String directory() {
        return directory;
    }

    /**
     * 按目录名解析阶段
     *
     * @param name 目录名（如 "preparation"）
     * @return 阶段
     */
    public static Optional<PromptPhase> of(String name) {
        for (final var phase : values()) {
            if (phase.directory.equals(name)) {
                return Optional.of(phase);
            }
        }
        return Optional.empty();
    }

}
