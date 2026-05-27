package io.github.oldmanpushcart.jinx.handler;

import io.github.oldmanpushcart.jinx.annotation.Command;
import io.github.oldmanpushcart.jinx.core.AgentService;
import io.github.oldmanpushcart.jinx.core.CommandHandler;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;

@Command(path = "chat", description = "智能对话")
public class ChatCommandHandler implements CommandHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ChatCommandHandler.class);
    
    private final AgentService agentService;
    
    public ChatCommandHandler() {
        this.agentService = new AgentService();
    }
    
    @Override
    public Publisher<String> handle(Map<String, String> params) {
        String prompt = params.get("prompt");
        String sessionId = params.get("sessionId");
        
        if (prompt == null || prompt.isEmpty()) {
            return Mono.error(new IllegalArgumentException("prompt不能为空"));
        }
        
        log.info("收到聊天请求: prompt={}, sessionId={}", prompt, sessionId);
        
        try {
            return agentService.chatStream(prompt, sessionId);
        } catch (Exception e) {
            log.error("聊天失败", e);
            return Mono.error(e);
        }
    }
}
