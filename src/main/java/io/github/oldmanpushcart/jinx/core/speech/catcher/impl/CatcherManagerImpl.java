package io.github.oldmanpushcart.jinx.core.speech.catcher.impl;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ConversationItemInputAudioTranscriptionCompletedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.RealtimeConnector;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherConfig;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherManager;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

@Singleton
public class CatcherManagerImpl implements CatcherManager {

    private static final Logger logger = LoggerFactory.getLogger(CatcherManagerImpl.class);
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
    public Publisher<String> catching() {

        if (!isEnabled()) {
            throw new UnsupportedOperationException("Catcher is disabled!");
        }

        return Flux.defer(() -> Flux.create(this::connect));

    }

    private void connect(FluxSink<String> sink) {
        final var connector = RealtimeConnector.newBuilder()
                .connectionFactory(() -> connectSession(sink))
                .retryStrategy((attempt, ex) -> null != ex ? Duration.ofSeconds(1) : null)
                .build();
        sink.onDispose(connector::shutdown);
        connector.connect()
                .whenComplete((u, ex) -> {
                    if (null != ex) {
                        sink.error(ex);
                    } else {
                        if (!sink.isCancelled()) {
                            return;
                        }
                        sink.complete();
                    }
                });
    }

    private CompletionStage<? extends Realtime.Connection> connectSession(FluxSink<String> sink) {
        final var session = QwenAsrRealtimeSession.newBuilder()
                .model(QwenAsrRealtimeModel.QWEN3_ASR_FLASH_REALTIME)
                .turnDetection(QwenAsrRealtimeSession.TurnDetection.SERVER_VAD)
                .inputAudioFormat(QwenAsrRealtimeSession.InputAudioFormat.PCM)
                .sampleRate(8000)
                .build();

        return client.realtime(session, new Realtime.Handler<>() {

            private volatile Picker picker;

            @Override
            public void onOpen(Realtime.Emitter<ClientEvent> emitter) {

                final var serverVad = (QwenAsrRealtimeEmitter.ServerVad) emitter;

                // 启动音频获取
                picker = new Picker(serverVad).begin();

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
                if (null != picker) {
                    picker.stop();
                }
            }

        });
    }


    private class Picker {

        private final QwenAsrRealtimeEmitter.ServerVad emitter;
        private final Thread daemon;
        private volatile boolean isRunning = true;

        private Picker(QwenAsrRealtimeEmitter.ServerVad emitter) {
            this.emitter = emitter;
            this.daemon = new Thread(this::picking, toString());
            this.daemon.setDaemon(true);
        }

        @Override
        public String toString() {
            return "jinx:/speech/catcher/picker";
        }

        private void picking() {
            try {
                logger.info("{} is running...", this);
                targetDataLineChannel.start();
                final var buffer = ByteBuffer.allocate(4028);
                while (isRunning
                        && targetDataLineChannel.isOpen()
                        && !emitter.isClosed()) {
                    targetDataLineChannel.read(buffer);
                    buffer.flip();
                    emitter.audio(buffer);
                }
            } catch (Exception ex) {
                emitter.close(ex);
            } finally {
                targetDataLineChannel.stop();
                logger.info("{} was stopped.", this);
            }
        }

        public Picker begin() {
            daemon.start();
            return this;
        }

        public void stop() {
            isRunning = false;
        }

    }

}
