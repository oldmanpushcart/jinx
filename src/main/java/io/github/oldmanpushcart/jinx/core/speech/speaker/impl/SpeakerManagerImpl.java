package io.github.oldmanpushcart.jinx.core.speech.speaker.impl;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ResponseAudioDeltaServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.jinx.core.speech.speaker.Speaker;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerConfig;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerManager;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class SpeakerManagerImpl implements SpeakerManager {

    private static final Logger logger = LoggerFactory.getLogger(SpeakerManagerImpl.class);
    private final SpeakerConfig config;
    private final DashscopeClient client;
    private final SourceDataLineChannel sourceDataLineChannel;

    public SpeakerManagerImpl(SpeakerConfig config, DashscopeClient client, SourceDataLineChannel sourceDataLineChannel) {
        this.config = config;
        this.client = client;
        this.sourceDataLineChannel = sourceDataLineChannel;
    }

    @Override
    public boolean isEnabled() {
        return config.enabled();
    }

    @Override
    public CompletionStage<Speaker> openSpeaker() {

        if (!isEnabled()) {
            return CompletableFuture.failedStage(new UnsupportedOperationException("Speaker is disabled!"));
        }

        final var connectF = new CompletableFuture<QwenTtsRealtimeEmitter.ServerVad>();

        final var session = QwenTtsRealtimeSession.newBuilder()
                .mode(QwenTtsRealtimeSession.Mode.SERVER_COMMIT)
                .model(QwenTtsRealtimeModel.QWEN3_TTS_FLASH_REALTIME)
                .voice("Cherry")
                .responseFormat(QwenTtsRealtimeSession.ResponseFormat.PCM)
                .sampleRate(8000)
                .speechRate(1.2f)
                .build();

        client.realtime(session, new Realtime.Handler<>() {

            private volatile QwenTtsRealtimeEmitter.ServerVad emitter;

            @Override
            public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
                this.emitter = (QwenTtsRealtimeEmitter.ServerVad) emitter;
                flushAndStart();
                connectF.complete(this.emitter);
            }

            @Override
            public void onData(ServerEvent output) {
                if (output instanceof ResponseAudioDeltaServerEvent event) {
                    final var buffer = event.delta();
                    sourceDataLineChannel.write(buffer);
                }
            }

            @Override
            public void onBinary(ByteBuffer buffer) {

            }

            @Override
            public void onClosed(Throwable ex) {
                if (null != emitter && !emitter.isClosed()) {
                    emitter.close();
                }
                drainAndStop();
            }

        });

        return connectF.thenApply(SpeakerImpl::new);
    }

    private synchronized void flushAndStart() {
        logger.debug("jinx:/speaker/source-data-line-channel flush-and-start!");
        sourceDataLineChannel.flush();
        sourceDataLineChannel.start();
    }

    private synchronized void drainAndStop() {
        logger.debug("jinx:/speaker/source-data-line-channel drain-and-stop!");
        sourceDataLineChannel.drain();
        sourceDataLineChannel.stop();
    }

}
