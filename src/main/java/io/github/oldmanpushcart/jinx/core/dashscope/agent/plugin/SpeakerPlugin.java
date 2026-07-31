package io.github.oldmanpushcart.jinx.core.dashscope.agent.plugin;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.jinx.core.JinxSettings;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerManager;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.isNotBlankString;

@Singleton
public class SpeakerPlugin implements Plugin {

    private final JinxSettings settings;
    private final ChatInterceptor speakerInterceptor;


    public SpeakerPlugin(JinxSettings settings, SpeakerManager speakerManager) {
        this.settings = settings;
        this.speakerInterceptor = new SpeakerInterceptor(speakerManager, settings);
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
                    // 始终安装拦截器，由拦截器在运行时动态检查开关状态
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
    private record SpeakerInterceptor(SpeakerManager speakerManager, JinxSettings settings) implements ChatInterceptor {

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

            // flow
            if (chain.type() == Type.FLOW) {
                return chain.proceed(request)
                        .thenCompose(r -> {

                            // 运行时检查语音播报开关
                            if (!settings.isSpeakerEnabled()) {
                                return java.util.concurrent.CompletableFuture.completedStage(r);
                            }

                            // 获取语音播放器
                            return speakerManager.openSpeaker()
                                    .thenApply(speaker -> {

                                        //noinspection unchecked
                                        final var flow = (Publisher<AigcResponse<Output>>) r;

                                        // 监听流变化并进行语音播报
                                        return Flux.from(flow)
                                                .doOnNext(response -> {
                                                    final var text = response.output().best().message().text();
                                                    if (isNotBlankString(text) && !speaker.isClosed()) {
                                                        speaker.speak(text);
                                                    }
                                                })
                                                .doFinally(signal -> {
                                                    if (!speaker.isClosed()) {
                                                        speaker.close();
                                                    }
                                                });

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
