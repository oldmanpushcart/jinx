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
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherManager;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherSetting;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

@Singleton
public class CatcherManagerImpl implements CatcherManager {

    private static final Logger logger = LoggerFactory.getLogger(CatcherManagerImpl.class);
    private final CatcherSetting setting;
    private final DashscopeClient client;
    private final TargetDataLineChannel channel;

    public CatcherManagerImpl(CatcherSetting setting, DashscopeClient client) throws LineUnavailableException {
        this.setting = setting;
        this.client = client;
        this.channel = openTargetDataLineChannel(setting);
    }

    private TargetDataLineChannel openTargetDataLineChannel(CatcherSetting setting) throws LineUnavailableException {

        final var targetCfg = setting.getTarget();

        final var format = new AudioFormat(
                targetCfg.sampleRate(),
                targetCfg.sampleSizeInBits(),
                targetCfg.channels(),
                targetCfg.signed(),
                targetCfg.bigEndian()
        );
        final var target = AudioSystem.getTargetDataLine(format);
        target.open(format);

        logger.info("jinx://speech/catcher/channel opened. sample-rate={};channels={}",
                targetCfg.sampleRate(),
                targetCfg.channels()
        );
        return new TargetDataLineChannel(target) {

            @Override
            public void close() {
                super.close();
                logger.info("jinx://speech/catcher/channel closed.");
            }

        };
    }

    @Override
    public boolean isEnabled() {
        return setting.isEnabled();
    }

    @Override
    public Publisher<String> catching() {
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

    @PreDestroy
    void destroy() {
        IOUtils.closeQuietly(channel);
    }

    /**
     * 拾音器
     */
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
                channel.start();
                final var buffer = ByteBuffer.allocate(4028);
                while (isRunning
                        && channel.isOpen()
                        && !emitter.isClosed()) {
                    channel.read(buffer);
                    buffer.flip();
                    emitter.audio(buffer);
                }
            } catch (Exception ex) {
                emitter.close(ex);
            } finally {
                channel.stop();
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
