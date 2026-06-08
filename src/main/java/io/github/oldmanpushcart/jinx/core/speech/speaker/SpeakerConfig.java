package io.github.oldmanpushcart.jinx.core.speech.speaker;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * 播放器配置
 *
 * @param enabled        是否启用
 * @param sourceDataLine 源数据行配置
 */
@ConfigurationProperties("jinx.speech.speaker")
public record SpeakerConfig(
        boolean enabled,
        SourceDataLineConfig sourceDataLine
) {

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
    public record SourceDataLineConfig(
            int sampleRate,
            int sampleSizeInBits,
            int channels,
            boolean signed,
            boolean bigEndian
    ) {
    }

}
