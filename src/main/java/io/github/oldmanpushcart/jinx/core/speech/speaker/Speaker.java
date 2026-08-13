package io.github.oldmanpushcart.jinx.core.speech.speaker;

/**
 * 播放器
 */
public interface Speaker extends AutoCloseable {

    /**
     * 播放文本内容
     *
     * @param text 文本
     */
    void speak(String text);

    /**
     * @return 是否已关闭
     */
    boolean isClosed();

    /**
     * 关闭播放器
     */
    @Override
    void close();

    /**
     * 中断播放
     */
    void abort();

}
