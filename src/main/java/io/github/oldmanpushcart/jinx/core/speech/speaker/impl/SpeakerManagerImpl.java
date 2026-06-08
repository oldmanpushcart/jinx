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
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerManager;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class SpeakerManagerImpl implements SpeakerManager {

    private static final Logger logger = LoggerFactory.getLogger(SpeakerManagerImpl.class);
    private final DashscopeClient client;
    private final SourceDataLineChannel sourceDataLineChannel;

    private final AtomicReference<Speaker> speakerRef = new AtomicReference<>();

    public SpeakerManagerImpl(DashscopeClient client, SourceDataLineChannel sourceDataLineChannel) {
        this.client = client;
        this.sourceDataLineChannel = sourceDataLineChannel;
    }

    @Override
    public CompletionStage<Speaker> openSpeaker() {

        final var connectF = new CompletableFuture<Speaker>();

        final var session = QwenTtsRealtimeSession.newBuilder()
                .mode(QwenTtsRealtimeSession.Mode.SERVER_COMMIT)
                .model(QwenTtsRealtimeModel.QWEN3_TTS_FLASH_REALTIME)
                .voice("Cherry")
                .responseFormat(QwenTtsRealtimeSession.ResponseFormat.PCM)
                .sampleRate(8000)
                .speechRate(1.2f)
                .build();

        client.realtime(session, new Realtime.Handler<>() {

            private volatile Speaker speaker;

            @Override
            public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
                final var serverVad = (QwenTtsRealtimeEmitter.ServerVad) emitter;
                final var speaker = new SpeakerImpl(serverVad);
                this.speaker = speaker;

                // 自旋锁，确保只有一个播放器
                while (true) {
                    final var exists = speakerRef.get();
                    if (speakerRef.compareAndSet(exists, speaker)) {
                        if (null != exists) {
                            exists.abort();
                        }
                        break;
                    }
                }

                connectF.complete(speaker);
                logger.debug("{} opened.", speaker);
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
                speakerRef.compareAndSet(speaker, null);
                logger.debug("{} closed.", speaker, ex);
            }

        });

        return connectF;
    }

}
