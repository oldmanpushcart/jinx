package io.github.oldmanpushcart.jinx.core.manager;

import org.reactivestreams.Publisher;

/**
 * 智能助手管理器
 */
public interface AgentManager {

    /**
     * 流式对话
     *
     * @param sessionId 会话ID
     * @param content   内容
     * @return 流式结果
     */
    Publisher<String> flow(String sessionId, String content);

}
