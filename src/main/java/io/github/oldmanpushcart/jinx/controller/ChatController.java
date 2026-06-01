package io.github.oldmanpushcart.jinx.controller;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.validation.Validated;
import jakarta.validation.constraints.NotBlank;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

@Validated
@Controller("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final Agent agent;

    public ChatController(Agent agent) {
        this.agent = agent;
    }

    @Post(
            uri = "/chat/{sessionId}",
            consumes = "text/plain;charset=UTF-8",
            produces = "text/plain;charset=UTF-8"
    )
    public Publisher<String> chat(

            @NotBlank
            @PathVariable("sessionId")
            String sessionId,

            @NotBlank
            @Body
            String content

    ) {

        final var inbound = Message.user(content);
        return Flux.from(agent.flow(sessionId, inbound))
                .map(Message::text)
                .onErrorMap(t -> {
                    final var cause = Exceptions.unwrap(t);
                    log.warn("jinx://api/chat/{} occur error!", sessionId, cause);
                    return cause;
                })
                ;
    }

}
