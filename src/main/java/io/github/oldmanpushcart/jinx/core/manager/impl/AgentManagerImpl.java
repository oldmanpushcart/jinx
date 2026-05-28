package io.github.oldmanpushcart.jinx.core.manager.impl;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeAgent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.RetryInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.retry.RetryStrategies;
import io.github.oldmanpushcart.jinx.config.AgentConfig;
import io.github.oldmanpushcart.jinx.config.Config;
import io.github.oldmanpushcart.jinx.core.manager.AgentManager;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Singleton
@Context
public class AgentManagerImpl implements AgentManager {

    private static final String DEFAULT_SESSION_ID = "default-session";
    private static final Logger log = LoggerFactory.getLogger(AgentManagerImpl.class);

    private Agent agent;

    public AgentManagerImpl(Config config1, AgentConfig config) {
        initialize(config);
    }

    private void initialize(AgentConfig config) {

        final var model = Optional.ofNullable(config.model())
                .map(m -> switch (m) {
                    case QWEN_PLUS -> ChatModel.QWEN_PLUS;
                    case QWEN_FLASH -> ChatModel.QWEN_FLASH;
                    case QWEN_MAX -> ChatModel.QWEN_MAX;
                })
                .orElse(ChatModel.QWEN_FLASH);

        final var http = new OkHttpClient.Builder()
                .connectTimeout(config.client().http().connectTimeout())
                .readTimeout(config.client().http().readTimeout())
                .writeTimeout(config.client().http().writeTimeout())
                .build();

        final var client = DashscopeClient.newBuilder()
                .ak(config.client().ak())
                .http(http)
                .interceptors(List.of(
                        RetryInterceptor.newBuilder()
                                .strategy(RetryStrategies.fixedDelay(Duration.ofSeconds(5), 5))
                                .build()
                ))
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
                log.info("jinx://mgr/agent close successfully");
            } catch (Exception e) {
                log.warn("jinx://mgr/agent close failed!", e);
            }
        }
    }

    @Override
    public Publisher<String> flow(String sessionId, String content) {

        final String finalSessionId = CommonUtils.isNotBlankString(sessionId)
                ? sessionId
                : DEFAULT_SESSION_ID;

        final var message = Message.user(content);

        return Flux.from(agent.flow(finalSessionId, message))
                .map(AssistantMessage::text)
                .doOnError(ex -> log.warn("jinx://mgr/agent/{} failed!", finalSessionId, ex))
                ;
    }
}
