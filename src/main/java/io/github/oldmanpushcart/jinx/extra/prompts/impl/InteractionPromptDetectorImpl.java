package io.github.oldmanpushcart.jinx.extra.prompts.impl;

import io.github.oldmanpushcart.jinx.extra.prompts.PromptPhase;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

/**
 * 交互阶段提示词探测器
 * <p>
 * 探测{@code {jinx.data}/prompts/interaction}，内容在 InteractionHook 阶段植入上下文。
 * </p>
 */
@Singleton
class InteractionPromptDetectorImpl extends PromptFileDetector {

    InteractionPromptDetectorImpl() {
        super(PromptPhase.INTERACTION);
    }

    @PostConstruct
    void init() {
        detectQuietly("init");
    }

    @Scheduled(fixedDelay = "10s")
    void scan() {
        detectQuietly("scan");
    }

}
