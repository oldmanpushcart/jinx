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
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerSetting;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class SpeakerManagerImpl implements SpeakerManager {

    private static final Logger logger = LoggerFactory.getLogger(SpeakerManagerImpl.class);
    private final DashscopeClient client;
    private final SpeakerSetting setting;
    private final SourceDataLineChannel channel;

    private final AtomicReference<Speaker> speakerRef = new AtomicReference<>();

    public SpeakerManagerImpl(DashscopeClient client, SpeakerSetting setting) throws LineUnavailableException {
        this.client = client;
        this.setting = setting;
        this.channel = openSourceDataLineChannel(setting);
    }

    /**
     * 打开音频播放通道
     *
     * @param setting 播放器设置
     * @return 音频播播放通道
     * @throws LineUnavailableException 音频播放频道配置错误
     */
    private static SourceDataLineChannel openSourceDataLineChannel(SpeakerSetting setting) throws LineUnavailableException {
        final var sourceCfg = setting.getSource();

        final var format = new AudioFormat(
                sourceCfg.sampleRate(),
                sourceCfg.sampleSizeInBits(),
                sourceCfg.channels(),
                sourceCfg.signed(),
                sourceCfg.bigEndian()
        );
        final var source = AudioSystem.getSourceDataLine(format);
        source.open(format);
        source.start();

        logger.info("jinx://speech/speaker/channel opened. sample-rate={};channels={}",
                sourceCfg.sampleRate(),
                sourceCfg.channels()
        );
        return new SourceDataLineChannel(source) {

            @Override
            public void close() {
                super.close();
                logger.info("jinx://speech/speaker/channel closed.");
            }

        };
    }

    @Override
    public boolean isEnabled() {
        return setting.isEnabled();
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
                    channel.write(buffer);
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
