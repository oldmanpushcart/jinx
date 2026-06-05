package io.github.oldmanpushcart.jinx.core.speech.catcher;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("jinx.speech.catcher")
public record CatcherConfig(
        boolean enabled,
        TargetDataLineConfig targetDataLine
) {

    @ConfigurationProperties("target-data-line")
    public record TargetDataLineConfig(
            int sampleRate,
            int sampleSizeInBits,
            int channels,
            boolean signed,
            boolean bigEndian
    ) {
    }

}
