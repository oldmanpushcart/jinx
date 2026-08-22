package io.github.oldmanpushcart.jinx.extra.chat.cli;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * chat — 发送对话消息（流式输出）
 * <p>
 * sessionId 来自请求头 X-Jinx-Session，参数为消息内容。
 * </p>
 */
@Singleton
class ChatCli implements Cli {

    private static final Logger log = LoggerFactory.getLogger(ChatCli.class);

    private final Agent agent;

    public ChatCli(Agent agent) {
        this.agent = agent;
    }

    @Override
    public String command() {
        return "chat";
    }

    @Override
    public String description() {
        return "Send a chat message.";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var sessionId = ctx.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return Mono.just("Usage: Use -s SESSION_ID or run 'session new' first.");
        }

        final var content = String.join(" ", ctx.args());
        if (content.isBlank()) {
            return Mono.just("Error: Message is empty.");
        }

        final var inbound = Message.user(content);
        return Flux.from(agent.flow(sessionId, inbound))
                .map(Message::text)
                .onErrorMap(t -> {
                    final var cause = Exceptions.unwrap(t);
                    log.warn("jinx://cli/chat/{} occur error!", sessionId, cause);
                    return cause;
                });
    }

}
