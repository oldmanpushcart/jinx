package io.github.oldmanpushcart.jinx.core.speech;

/**
 * 语音开关事件
 *
 * @param type    类型
 * @param enabled 是否开启
 */
public record SpeechSwitchEvent(Type type, boolean enabled) {

    public enum Type {
        CATCHER,
        SPEAKER
    }

}
