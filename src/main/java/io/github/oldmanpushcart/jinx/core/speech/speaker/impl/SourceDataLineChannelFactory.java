package io.github.oldmanpushcart.jinx.core.speech.speaker.impl;

import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerConfig;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

@Factory
public class SourceDataLineChannelFactory {

    private static final Logger logger = LoggerFactory.getLogger(SourceDataLineChannelFactory.class);

    @Singleton
    public SourceDataLineChannel openSourceDataLineChannel(SpeakerConfig config) throws LineUnavailableException {
        if (!config.enabled()) {
            return null;
        }
        final var format = new AudioFormat(
                config.sourceDataLine().sampleRate(),
                config.sourceDataLine().sampleSizeInBits(),
                config.sourceDataLine().channels(),
                config.sourceDataLine().signed(),
                config.sourceDataLine().bigEndian()
        );
        final var source = AudioSystem.getSourceDataLine(format);
        source.open(format);

        logger.info("jinx:/speech/source-data-line-channel opened. sample-rate={};channels={}",
                config.sourceDataLine().sampleRate(),
                config.sourceDataLine().channels()
        );
        return new SourceDataLineChannel(source) {

            @Override
            public void close() {
                super.close();
                logger.info("jinx:/speech/source-data-line-channel closed.");
            }

        };
    }

}
