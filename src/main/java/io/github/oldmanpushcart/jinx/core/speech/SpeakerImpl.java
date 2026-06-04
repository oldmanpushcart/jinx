package io.github.oldmanpushcart.jinx.core.speech;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ResponseAudioDeltaServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class SpeakerImpl implements Speaker {

    private final DashscopeClient client;
    private final SourceDataLineChannel sourceDataLineChannel;

    public SpeakerImpl(DashscopeClient client, SourceDataLineChannel sourceDataLineChannel) {
        this.client = client;
        this.sourceDataLineChannel = sourceDataLineChannel;
    }

    @Override
    public CompletionStage<Void> speak(Publisher<String> flow) {

        return CompletableFuture.completedStage(flow)
                .thenCompose(this::synthesis)
                .thenCompose(byteFlow -> {
                    final var completed = new CompletableFuture<Void>();
                    Flux.from(byteFlow)
                            .doOnSubscribe(sub -> this.flushAndStart())
                            .doOnTerminate(this::drainAndStop)
                            .doOnCancel(this::drainAndStop)
                            .subscribe(
                                    sourceDataLineChannel::write,
                                    completed::completeExceptionally,
                                    () -> completed.complete(null)
                            );
                    return completed;
                });
    }

    private CompletionStage<Publisher<ByteBuffer>> synthesis(Publisher<String> textFlow) {
        final var session = QwenTtsRealtimeSession.newBuilder()
                .mode(QwenTtsRealtimeSession.Mode.SERVER_COMMIT)
                .model(QwenTtsRealtimeModel.QWEN3_TTS_FLASH_REALTIME)
                .voice("Cherry")
                .responseFormat(QwenTtsRealtimeSession.ResponseFormat.PCM)
                .sampleRate(8000)
                .build();


        final var sink = Sinks.many().multicast().<ByteBuffer>onBackpressureBuffer();
        final var completeF = new CompletableFuture<Publisher<ByteBuffer>>();
        client.realtime(session, new Realtime.Handler<ClientEvent, ServerEvent>() {

            private Disposable subscription;

            @Override
            public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
                final var serverVad = (QwenTtsRealtimeEmitter.ServerVad) emitter;
                subscription = Flux.from(textFlow)
                        .subscribe(
                                serverVad::text,
                                sink::tryEmitError,
                                serverVad::close
                        );
                completeF.complete(sink.asFlux());
            }

            @Override
            public void onData(ServerEvent output) {
                if (output instanceof ResponseAudioDeltaServerEvent event) {
                    final var buffer = event.delta();
                    if (sink.tryEmitNext(buffer).isFailure()) {
                        if (null != subscription && !subscription.isDisposed()) {
                            subscription.dispose();
                        }
                    }
                }
            }

            @Override
            public void onBinary(ByteBuffer buffer) {

            }

            @Override
            public void onClosed(Throwable ex) {

                if (null == ex) {
                    sink.tryEmitComplete();
                } else {
                    sink.tryEmitError(ex);
                }

                if (null != subscription && !subscription.isDisposed()) {
                    subscription.dispose();
                }

            }

        });

        return completeF;
    }

    private synchronized void flushAndStart() {
        sourceDataLineChannel.flush();
        sourceDataLineChannel.start();
    }

    private synchronized void drainAndStop() {
        sourceDataLineChannel.drain();
        sourceDataLineChannel.stop();
    }

}
