package io.github.oldmanpushcart.jinx.core.controller;

import io.github.oldmanpushcart.jinx.core.manager.AgentManager;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.isBlankString;

@Controller("/api/v1/agent")
public class ChatController {

    private final AgentManager agentManager;

    public ChatController(AgentManager agentManager) {
        this.agentManager = agentManager;
    }

    @Get(uri = "/chat", produces = "text/plain;charset=UTF-8")
    public Publisher<String> chat(String sessionId, String prompt) {
        if (isBlankString(prompt)) {
            return Mono.error(() -> new IllegalArgumentException("Error: prompt cannot be empty"));
        }
        return agentManager.flow(sessionId, prompt);
    }
}
