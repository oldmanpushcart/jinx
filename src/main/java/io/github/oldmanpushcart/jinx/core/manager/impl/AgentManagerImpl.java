package io.github.oldmanpushcart.jinx.core.manager.impl;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeAgent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.jinx.config.DashscopeConfig;
import io.github.oldmanpushcart.jinx.core.manager.AgentManager;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PreDestroy;
import okhttp3.OkHttpClient;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Context
public class AgentManagerImpl implements AgentManager {

    private static final Logger log = LoggerFactory.getLogger(AgentManagerImpl.class);

    private Agent agent;

    public AgentManagerImpl(DashscopeConfig config) {
        initialize(config);
    }

    private void initialize(DashscopeConfig config) {

        final var model = Optional.ofNullable(config.model())
                .map(m -> switch (m) {
                    case QWEN_PLUS -> ChatModel.QWEN_PLUS;
                    case QWEN_FLASH -> ChatModel.QWEN_FLASH;
                    case QWEN_MAX -> ChatModel.QWEN_MAX;
                })
                .orElse(ChatModel.QWEN_FLASH);

        final var http = new OkHttpClient.Builder()
                .build();

        final var client = DashscopeClient.newBuilder()
                .ak(config.ak())
                .http(http)
                .build();

        this.agent = DashscopeAgent.newBuilder()
                .name("jinx-agent")
                .description("Jinx AI Agent")
                .client(client)
                .model(model)
                .buildAsync()
                .whenComplete((u, ex) -> {
                    if (null == ex) {
                        log.info("jinx://mgr/agent initialize successfully. model={}", model.name());
                    } else {
                        log.warn("jinx://mgr/agent initialize failed!", ex);
                    }
                })
                .toCompletableFuture()
                .join();
    }

    @PreDestroy
    public void destroy() {
        if (agent != null) {
            try {
                agent.close();
                log.info("jinx://agent/destroy DashScope Agent closed successfully");
            } catch (Exception e) {
                log.error("jinx://agent/destroy Failed to close DashScope Agent [error={};]", e.getMessage());
            }
        }
    }

    @Override
    public Publisher<String> flow(String sessionId, String content) {
        final String finalSessionId = (sessionId == null || sessionId.isEmpty()) ? "default-session" : sessionId;

        UserMessage message = UserMessage.newBuilder()
                .contents(List.of(Content.text(content)))
                .build();

        return Flux.from(agent.flow(finalSessionId, message))
                .map(assistantMessage -> assistantMessage.text())
                .doOnNext(text -> log.debug("jinx://agent/chat_stream Stream output chunk [session_id={};chunk_length={};]", finalSessionId, text.length()))
                .onErrorResume(e -> {
                    log.error("jinx://agent/chat_stream DashScope API call failed [session_id={};error={};]", finalSessionId, e.getMessage());
                    return Flux.error(new RuntimeException("AI service call failed: " + e.getMessage()));
                });
    }
}
