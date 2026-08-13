package io.github.oldmanpushcart.jinx.core.speech.speaker;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("jinx.speech.speaker")
public class SpeakerSetting {

    private boolean enabled;
    private final Source source;

    public SpeakerSetting(Source source) {
        this.source = source;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Source getSource() {
        return source;
    }

    /**
     * 源数据行配置
     *
     * @param sampleRate       采样率
     * @param sampleSizeInBits 采样位数
     * @param channels         声道数
     * @param signed           符号
     * @param bigEndian        大小端
     */
    @ConfigurationProperties("source-data-line")
    public record Source(
            int sampleRate,
            int sampleSizeInBits,
            int channels,
            boolean signed,
            boolean bigEndian
    ) {
    }

}
