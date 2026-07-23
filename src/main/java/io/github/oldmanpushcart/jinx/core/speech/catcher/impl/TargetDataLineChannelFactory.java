package io.github.oldmanpushcart.jinx.core.speech.catcher.impl;

import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherConfig;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

@Factory
public class TargetDataLineChannelFactory {

    private static final Logger logger = LoggerFactory.getLogger(TargetDataLineChannelFactory.class);

    @Singleton
    @Requires(property = "jinx.speech.capture.enabled", value = "true")
    public TargetDataLineChannel openTargetDataLineChannel(CatcherConfig config) throws LineUnavailableException {
        if (!config.enabled()) {
            return null;
        }
        final var format = new AudioFormat(
                config.targetDataLine().sampleRate(),
                config.targetDataLine().sampleSizeInBits(),
                config.targetDataLine().channels(),
                config.targetDataLine().signed(),
                config.targetDataLine().bigEndian()
        );
        final var target = AudioSystem.getTargetDataLine(format);
        target.open(format);

        logger.info("jinx:/speech/target-data-line-channel opened. sample-rate={};channels={}",
                config.targetDataLine().sampleRate(),
                config.targetDataLine().channels()
        );
        return new TargetDataLineChannel(target) {

            @Override
            public void close() {
                super.close();
                logger.info("jinx:/speech/target-data-line-channel closed.");
            }

        };
    }

}
