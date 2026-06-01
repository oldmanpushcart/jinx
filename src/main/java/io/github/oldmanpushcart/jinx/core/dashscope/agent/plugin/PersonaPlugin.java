package io.github.oldmanpushcart.jinx.core.dashscope.agent.plugin;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
public class PersonaPlugin implements Plugin {

    private static final Logger log = LoggerFactory.getLogger(PersonaPlugin.class);

    private static Message loadPersonaMessage() {
        try {
            final var personaMd = Files.readString(Path.of("./conf/PERSONA.md"), UTF_8);
            return Message.system(personaMd)
                    .withCache();
        } catch (IOException e) {
            log.warn("jinx://persona disabled, read ./conf/PERSONA.md failed!", e);
            return null;
        }
    }

    private static final Message personaMessage = loadPersonaMessage();

    @Override
    public CompletionStage<Extension> install(Agent agent) {
        return CompletableFuture.completedStage(new Extension() {
            @Override
            public Plugin plugin() {
                return PersonaPlugin.this;
            }

            @Override
            public List<ChatInterceptor> interceptors(Phases phases) {
                return switch (phases) {
                    case PREPARATION -> {
                        if (null != personaMessage) {
                            yield List.of(new SettingInterceptor(personaMessage));
                        } else {
                            yield List.of();
                        }
                    }
                    case INTERACTION -> List.of();
                };
            }
        });
    }

    @Override
    public CompletionStage<Void> uninstall() {
        return CompletableFuture.completedStage(null);
    }


    static class SettingInterceptor implements ChatInterceptor {

        private final Message personaMessage;

        public SettingInterceptor(Message personaMessage) {
            this.personaMessage = personaMessage;
        }

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
            final var newRequest = AigcRequest.newBuilder(request)
                    .input(input -> Input.newBuilder(input)
                            .messages(messages -> {
                                messages.add(0, personaMessage);
                                return messages;
                            })
                            .build())
                    .build();
            return chain.proceed(newRequest);
        }

    }

}
