package io.github.oldmanpushcart.jinx.core.controller;

import io.github.oldmanpushcart.jinx.core.manager.AgentManager;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@Controller("/api/v1/agent")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AgentManager agentManager;

    public ChatController(AgentManager agentManager) {
        this.agentManager = agentManager;
    }

    @Get(uri = "/chat", produces = "text/plain;charset=UTF-8")
    public Publisher<String> chat(String sessionId, String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return Mono.error(() -> new IllegalArgumentException("Error: prompt cannot be empty"));
        }
        return agentManager.flow(sessionId, prompt);
    }
}
