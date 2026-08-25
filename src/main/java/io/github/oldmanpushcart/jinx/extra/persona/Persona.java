package io.github.oldmanpushcart.jinx.extra.persona;

import jakarta.inject.Singleton;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 人格
 */
@Singleton
public class Persona {

    private final AtomicReference<String> contentRef = new AtomicReference<>("");

    /**
     * @return 人格内容
     */
    public String content() {
        return contentRef.get();
    }

    /**
     * 刷新人格内容（由探测器在文件变更时调用）
     *
     * @param content 人格内容
     */
    public void refresh(String content) {
        contentRef.set(content);
    }

}
