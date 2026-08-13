package io.github.oldmanpushcart.jinx.core.speech.speaker;

import java.util.concurrent.CompletionStage;

/**
 * 播放器管理器
 */
public interface SpeakerManager {

    /**
     * @return 音频播放是否启用
     */
    boolean isEnabled();

    /**
     * 打开播放器
     * <p>
     * 播放器是独占的，若当前已经存在一个播放器，将会主动关闭之前的。
     * </p>
     *
     * @return 播放器
     */
    CompletionStage<Speaker> openSpeaker();

}
