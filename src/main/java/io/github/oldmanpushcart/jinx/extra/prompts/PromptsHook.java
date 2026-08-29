package io.github.oldmanpushcart.jinx.extra.prompts;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.InteractionHook;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import jakarta.inject.Singleton;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 提示词钩子
 * <p>
 * 将各阶段的提示词作为系统消息植入上下文头部。
 * {@code preparation} 目录对应 PreparationHook，{@code interaction} 目录对应 InteractionHook。
 * 每条提示词按名称排序后仅植入一次，保持前缀稳定以命中缓存。
 * </p>
 */
@Singleton
class PromptsHook implements PreparationHook, InteractionHook {

    private final ChatInterceptor preparationInterceptor;
    private final ChatInterceptor interactionInterceptor;

    public PromptsHook(List<PromptDetector> detectors) {
        this.preparationInterceptor = new InjectInterceptor(detectorOf(detectors, PromptPhase.PREPARATION));
        this.interactionInterceptor = new InjectInterceptor(detectorOf(detectors, PromptPhase.INTERACTION));
    }

    private static PromptDetector detectorOf(List<PromptDetector> detectors, PromptPhase phase) {
        return detectors.stream()
                .filter(detector -> detector.phase() == phase)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PromptDetector not found for phase: %s".formatted(phase)));
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(preparationInterceptor);
    }

    @Override
    public List<? extends ChatInterceptor> onInteraction(Agent agent) {
        return List.of(interactionInterceptor);
    }

    /**
     * 注入拦截器：将探测到的提示词逐条植入上下文头部
     */
    static class InjectInterceptor implements ChatInterceptor {

        private final PromptDetector detector;

        public InjectInterceptor(PromptDetector detector) {
            this.detector = detector;
        }

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<ChatModel.Input, ChatModel.Output> request) {

            // 按名称排序，过滤空白内容，每条提示词仅植入一次
            final var systems = detector.list().stream()
                    .sorted(Comparator.comparing(PromptMeta::name))
                    .map(PromptMeta::content)
                    .filter(content -> !content.isBlank())
                    .map(content -> Message.system(content).withCache())
                    .toList();

            if (systems.isEmpty()) {
                return chain.proceed(request);
            }

            final var newRequest = AigcRequest.newBuilder(request)
                    .input(input -> ChatModel.Input.newBuilder(input)
                            .messages(messages -> {
                                messages.addAll(0, systems);
                                return messages;
                            })
                            .build())
                    .build();
            return chain.proceed(newRequest);
        }

    }

}
