package io.github.oldmanpushcart.jinx.extra.prompts;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import jakarta.inject.Singleton;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 提示词钩子
 * <p>
 * 将提示词作为系统消息在每次智能体对话时植入上下文头部。
 * 每条提示词按名称排序后仅植入一次，保持前缀稳定以命中缓存。
 * </p>
 */
@Singleton
class PromptsHook implements PreparationHook, ChatInterceptor {

    private final PromptDetector detector;

    public PromptsHook(PromptDetector detector) {
        this.detector = detector;
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(this);
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<ChatModel.Input, ChatModel.Output> request) {

        // 按名称排序，过滤空白内容，每条提示词仅植入一次
        final var systems = detector.list().stream()
                .sorted(Comparator.comparing(PromptMeta::name))
                .filter(meta -> CommonUtils.isNotBlankString(meta.content()))
                .map(meta -> Message
                        .system("""
                                > %s
                                
                                %s
                                """.formatted(
                                meta.path().toAbsolutePath(),
                                meta.content()
                        ))
                        .withCache())
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
