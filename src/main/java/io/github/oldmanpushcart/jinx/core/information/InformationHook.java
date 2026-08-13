package io.github.oldmanpushcart.jinx.core.information;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.jinx.JinxConfig;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CompletionStage;

@Singleton
public class InformationHook implements PreparationHook {

    private final ChatInterceptor settingInterceptor;

    public InformationHook(JinxConfig config) {
        this.settingInterceptor = new SettingInterceptor(Information.newInstant(config));
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(settingInterceptor);
    }

    private record SettingInterceptor(Information information) implements ChatInterceptor {

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
            final var newRequest = AigcRequest.newBuilder(request)
                    .input(input -> Input.newBuilder(input)
                            .messages(messages -> {
                                final var informationJson = JacksonJsonUtils.toJson(information);
                                messages.add(0, Message.system(informationJson).withCache());
                                return messages;
                            })
                            .build())
                    .build();
            return chain.proceed(newRequest);
        }

    }

}
