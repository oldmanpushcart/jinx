package io.github.oldmanpushcart.jinx.extra.information;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 运行时信息钩子
 * <p>
 * 将运行时信息（静态段 + 当前会话 SESSION）注入上下文。
 * 插入位置在头部 system 消息序列之后，确保 persona/user 等静态段保持前缀稳定，缓存有效。
 * </p>
 */
@Singleton
class InformationHook implements PreparationHook {

    private final ChatInterceptor settingInterceptor;

    public InformationHook(Information information) {
        this.settingInterceptor = new SettingInterceptor(information);
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(settingInterceptor);
    }

    /**
     * 设置拦截器
     */
    static class SettingInterceptor implements ChatInterceptor {

        private final Information information;

        public SettingInterceptor(Information information) {
            this.information = information;
        }

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<ChatModel.Input, ChatModel.Output> request) {

            final var sessionId = (String) request.context().get("SESSION-ID");
            final var content = CommonUtils.isNotBlankString(sessionId)
                    ? "%s\nSESSION: %s".formatted(information.content(), sessionId)
                    : information.content();

            final var newRequest = AigcRequest.newBuilder(request)
                    .input(input -> ChatModel.Input.newBuilder(input)
                            .messages(messages -> {
                                // 定位头部 system 消息序列之后的位置插入，
                                // 避免逐会话变化的 SESSION 破坏 persona/user 的前缀缓存
                                int index = 0;
                                while (index < messages.size() && messages.get(index) instanceof SystemMessage) {
                                    index++;
                                }
                                messages.add(index, Message.system(content).withCache());
                                return messages;
                            })
                            .build())
                    .build();
            return chain.proceed(newRequest);
        }

    }

}
