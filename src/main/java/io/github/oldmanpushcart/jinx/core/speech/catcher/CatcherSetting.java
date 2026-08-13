package io.github.oldmanpushcart.jinx.core.speech.catcher;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("jinx.speech.catcher")
public class CatcherSetting {

    private boolean enabled;
    private final Target target;

    public CatcherSetting(Target target) {
        this.target = target;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Target getTarget() {
        return target;
    }

    @ConfigurationProperties("target-data-line")
    public record Target(
            int sampleRate,
            int sampleSizeInBits,
            int channels,
            boolean signed,
            boolean bigEndian
    ) {
    }

}
