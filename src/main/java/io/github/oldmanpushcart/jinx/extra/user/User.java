package io.github.oldmanpushcart.jinx.extra.user;


import jakarta.inject.Singleton;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 用户档案
 * <p>
 * 存放用户主动告知的稳定画像信息（"你是谁"），与记录交互历史的MEMORY区分：
 * 仅记录称呼、职业、偏好、习惯等长期稳定的个人信息，不记录事件与对话内容。
 * </p>
 */
@Singleton
public class User {

    private final AtomicReference<String> contentRef = new AtomicReference<>("");

    /**
     * @return 用户档案内容
     */
    public String content() {
        return contentRef.get();
    }

    /**
     * 刷新用户档案内容（由探测器在文件变更时调用）
     *
     * @param content 用户档案内容
     */
    public void refresh(String content) {
        contentRef.set(content);
    }

}
