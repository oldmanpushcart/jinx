package io.github.oldmanpushcart.jinx.core.speech.speaker;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.isNotBlankString;

@Singleton
public class SpeakerHook implements PreparationHook {

    private final ChatInterceptor speakerInterceptor;

    public SpeakerHook(SpeakerSetting setting, SpeakerManager speakerManager) {
        this.speakerInterceptor = new SpeakerInterceptor(speakerManager, setting);
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(speakerInterceptor);
    }

    /**
     * 播放器拦截器
     */
    private record SpeakerInterceptor(SpeakerManager speakerManager, SpeakerSetting setting)
            implements ChatInterceptor {

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

            // flow
            if (chain.type() == Type.FLOW) {
                return chain.proceed(request)
                        .thenCompose(r -> {

                            // 运行时检查语音播报开关
                            if (!setting.isEnabled()) {
                                return CompletableFuture.completedStage(r);
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
