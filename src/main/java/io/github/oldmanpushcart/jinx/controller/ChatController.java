package io.github.oldmanpushcart.jinx.controller;

import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.isBlankString;

@Controller("/api/v1/agent")
public class ChatController {

    private final DashscopeAgent agent;

    public ChatController(DashscopeAgent agent) {
        this.agent = agent;
    }

    @Get(uri = "/chat", produces = "text/plain;charset=UTF-8")
    public Publisher<String> chat(String sessionId, String prompt) {
        if (isBlankString(prompt)) {
            return Mono.error(() -> new IllegalArgumentException("Error: prompt cannot be empty"));
        }
        final var inbound = Message.user(prompt);
        return Flux.from(agent.flow(sessionId, inbound))
                .map(Message::text);
    }
}
