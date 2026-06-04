package io.github.oldmanpushcart.jinx.core.speech.speaker;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("jinx.speech.speaker")
public record SpeakerConfig(
        boolean enabled,
        SourceDataLineConfig sourceDataLine
) {

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
