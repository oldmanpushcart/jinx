package io.github.oldmanpushcart.jinx.extra.persona;

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
 * 人格钩子
 * <p>
 * 用于给智能体挂上人格特性
 * </p>
 */
@Singleton
class PersonaHook implements PreparationHook {

    private final ChatInterceptor settingInterceptor;

    public PersonaHook(Persona persona) {
        this.settingInterceptor = new SettingInterceptor(persona);
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(settingInterceptor);
    }

    /**
     * 设置拦截器
     */
    static class SettingInterceptor implements ChatInterceptor {

        private final Persona persona;

        public SettingInterceptor(Persona persona) {
            this.persona = persona;
        }

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<ChatModel.Input, ChatModel.Output> request) {

            final var personaContent = persona.content();
            if (CommonUtils.isBlankString(personaContent)) {
                return chain.proceed(request);
            }

            final var newRequest = AigcRequest.newBuilder(request)
                    .input(input -> ChatModel.Input.newBuilder(input)
                            .messages(messages -> {
                                messages.add(0, Message.system(personaContent).withCache());
                                return messages;
                            })
                            .build())
                    .build();
            return chain.proceed(newRequest);
        }

    }

}
