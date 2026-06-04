package io.github.oldmanpushcart.jinx.core.dashscope.agent.plugin;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.jinx.core.speech.speaker.Speaker;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerManager;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class SpeakerPlugin implements Plugin {

    private final ChatInterceptor speakerInterceptor;
    private final AtomicReference<Speaker> speakerRef = new AtomicReference<>();

    public SpeakerPlugin(SpeakerManager speakerManager) {
        this.speakerInterceptor = new SpeakerInterceptor(speakerManager);
    }

    @Override
    public CompletionStage<Extension> install(Agent agent) {
        return CompletableFuture.completedStage(new Extension() {

            @Override
            public Plugin plugin() {
                return SpeakerPlugin.this;
            }

            @Override
            public List<ChatInterceptor> interceptors(Phases phases) {
                return switch (phases) {
                    case PREPARATION -> List.of(speakerInterceptor);
                    case INTERACTION -> List.of();
                };
            }

        });
    }

    @Override
    public CompletionStage<Void> uninstall() {
        return CompletableFuture.completedStage(null);
    }


    /**
     * 播放器拦截器
     */
    private class SpeakerInterceptor implements ChatInterceptor {

        private final SpeakerManager speakerManager;

        private SpeakerInterceptor(SpeakerManager speakerManager) {
            this.speakerManager = speakerManager;
        }

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

            // 关掉之前的播放器
            Optional.ofNullable(speakerRef.getAndSet(null))
                    .ifPresent(Speaker::abort);

            // flow
            if (chain.type() == Type.FLOW) {
                return chain.proceed(request)
                        .thenCompose(r -> {

                            // 如果没启用播放器，则跳过
                            if (!speakerManager.isEnabled()) {
                                return CompletableFuture.completedStage(r);
                            }

                            // 获取语音播放器
                            return speakerManager.openSpeaker()
                                    .thenApply(speaker -> {

                                        // 关掉之前的播放器
                                        Optional.ofNullable(speakerRef.getAndSet(speaker))
                                                .ifPresent(Speaker::abort);

                                        //noinspection unchecked
                                        final var flow = (Publisher<AigcResponse<Output>>) r;

                                        // 监听流的变化并进行语音播报
                                        return Flux.from(flow)
                                                .doOnNext(response -> {
                                                    if (!speaker.isClosed()) {
                                                        speaker.speak(response.output().best().message().text());
                                                    }
                                                })
                                                .doFinally(signal -> speaker.close());

                                    });
                        });
            }

            // task & async
            else {
                return chain.proceed(request);
            }

        }

    }

}
