package io.github.oldmanpushcart.jinx.core;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeAgent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.List;

public class AgentService {
    
    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    
    private Agent agent;
    
    public void init() {
        // 创建 DashScope 客户端
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("环境变量 DASHSCOPE_API_KEY 未设置");
        }
        
        DashscopeClient client = DashscopeClient.newBuilder()
            .ak(apiKey)
            .build();
        
        // 创建 Agent
        this.agent = DashscopeAgent.newBuilder()
            .name("jinx-agent")
            .description("Jinx AI Agent")
            .client(client)
            .model(ChatModel.QWEN_PLUS)
            .buildAsync()
            .toCompletableFuture()
            .join();
        
        log.info("DashScope Agent 初始化完成");
    }
    
    public Publisher<String> chatStream(String prompt, String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "default-session";
        }
        
        // 创建 UserMessage
        UserMessage message = UserMessage.newBuilder()
            .contents(List.of(Content.text(prompt)))
            .build();
        
        return Flux.from(agent.flow(sessionId, message))
            .map(assistantMessage -> assistantMessage.text())
            .doOnNext(text -> log.debug("流式输出: {}", text))
            .onErrorResume(e -> {
                log.error("DashScope调用失败", e);
                return Flux.error(new RuntimeException("AI服务调用失败: " + e.getMessage()));
            });
    }
}
