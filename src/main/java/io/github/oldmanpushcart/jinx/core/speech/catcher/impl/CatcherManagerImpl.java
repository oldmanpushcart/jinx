package io.github.oldmanpushcart.jinx.core.speech.catcher.impl;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ConversationItemInputAudioTranscriptionCompletedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherConfig;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherManager;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;

@Singleton
public class CatcherManagerImpl implements CatcherManager {

    private final CatcherConfig config;
    private final DashscopeClient client;
    private final TargetDataLineChannel targetDataLineChannel;

    public CatcherManagerImpl(CatcherConfig config, DashscopeClient client, TargetDataLineChannel targetDataLineChannel) {
        this.config = config;
        this.client = client;
        this.targetDataLineChannel = targetDataLineChannel;
    }

    @Override
    public boolean isEnabled() {
        return config.enabled();
    }

    @Override
    public Publisher<String> openCatcher() {

        if (!isEnabled()) {
            throw new UnsupportedOperationException("Catcher is disabled!");
        }

        return Flux.defer(() -> Flux.create(sink -> {

            final var session = QwenAsrRealtimeSession.newBuilder()
                    .model(QwenAsrRealtimeModel.QWEN3_ASR_FLASH_REALTIME)
                    .turnDetection(QwenAsrRealtimeSession.TurnDetection.SERVER_VAD)
                    .inputAudioFormat(QwenAsrRealtimeSession.InputAudioFormat.PCM)
                    .sampleRate(8000)
                    .build();

            client.realtime(session, new Realtime.Handler<>() {

                @Override
                public void onOpen(Realtime.Emitter<ClientEvent> emitter) {

                    final var serverVad = (QwenAsrRealtimeEmitter.ServerVad) emitter;

                    // 关联sink和realtime的链接
                    sink.onDispose(serverVad::abort);

                }

                @Override
                public void onData(ServerEvent output) {
                    if (output instanceof ConversationItemInputAudioTranscriptionCompletedServerEvent event) {
                        if (!sink.isCancelled()) {
                            sink.next(event.transcript());
                        }
                    }
                }

                @Override
                public void onBinary(ByteBuffer buffer) {

                }

                @Override
                public void onClosed(Throwable ex) {
                    if (null == ex) {
                        sink.complete();
                    } else {
                        sink.error(ex);
                    }
                }

            });

        }));

    }


    private class Picker extends Thread {

        private final QwenAsrRealtimeEmitter.ServerVad emitter;

        private Picker(QwenAsrRealtimeEmitter.ServerVad emitter) {
            this.emitter = emitter;
        }

        @Override
        public void run() {
            try {

                final var buffer = ByteBuffer.allocate(4028);
                while (!isInterrupted()
                        && !emitter.isClosed()
                        && targetDataLineChannel.isOpen()) {
                    targetDataLineChannel.read(buffer);
                    buffer.flip();
                    emitter.audio(buffer);
                }

            } catch (Exception ex) {
                emitter.close(ex);
            }
        }

    }

}
