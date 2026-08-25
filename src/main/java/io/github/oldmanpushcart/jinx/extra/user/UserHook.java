package io.github.oldmanpushcart.jinx.extra.user;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 用户档案钩子
 * <p>
 * 用于给智能体挂上用户档案特性，将用户档案内容作为系统消息常驻会话。
 * </p>
 */
@Singleton
class UserHook implements PreparationHook {

    private final ChatInterceptor settingInterceptor;

    public UserHook(User user) {
        this.settingInterceptor = new SettingInterceptor(user);
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(settingInterceptor);
    }

    /**
     * 设置拦截器
     */
    static class SettingInterceptor implements ChatInterceptor {

        private final User user;

        public SettingInterceptor(User user) {
            this.user = user;
        }

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<ChatModel.Input, ChatModel.Output> request) {

            final var userContent = user.content();
            if (CommonUtils.isBlankString(userContent)) {
                return chain.proceed(request);
            }

            final var newRequest = AigcRequest.newBuilder(request)
                    .input(input -> ChatModel.Input.newBuilder(input)
                            .messages(messages -> {
                                messages.add(0, Message.system(userContent).withCache());
                                return messages;
                            })
                            .build())
                    .build();
            return chain.proceed(newRequest);
        }

    }

}
