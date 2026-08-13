package io.github.oldmanpushcart.jinx.core.speech;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherManager;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherSetting;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerManager;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerSetting;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


/**
 * 音频模块启动器
 */
@Context
class SpeechLatcher {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Toolbox toolbox;
    private final Agent agent;

    private final SpeakerSetting speakerSetting;
    private final SpeakerManager speakerManager;

    private final CatcherSetting catcherSetting;
    private final CatcherManager catcherManager;

    private volatile Disposable catchingDispose;

    public SpeechLatcher(
            final Toolbox toolbox,
            final Agent agent,
            final SpeakerSetting speakerSetting,
            final SpeakerManager speakerManager,
            final CatcherSetting catcherSetting,
            final CatcherManager catcherManager
    ) {
        this.toolbox = toolbox;
        this.agent = agent;
        this.speakerSetting = speakerSetting;
        this.speakerManager = speakerManager;
        this.catcherSetting = catcherSetting;
        this.catcherManager = catcherManager;
    }

    @PostConstruct
    void init() {

        initCatching();
        initSpeaking();

        toolbox.subscribeTools("speech", sppechToolkit())
                .toCompletableFuture()
                .join();

    }

    private void initCatching() {
        if (catcherSetting.isEnabled()) {
            beginCatching();
        }
    }

    private synchronized void beginCatching() {
        // If already catching, do nothing
        if (null != catchingDispose) {
            return;
        }

        final var disposeRef = new AtomicReference<Disposable>();
        catchingDispose = Flux.from(catcherManager.catching())
                .subscribe(text -> {
                    final var dispose = Flux.from(agent.flow("SPEECH-SESSION-001", Message.user(text)))
                            .subscribe();

                    //Mono.fromCompletionStage()
                    Optional.ofNullable(disposeRef.getAndSet(dispose))
                            .ifPresent(Disposable::dispose);

                });
        logger.debug("jinx://speech/catcher enabled.");
    }

    private synchronized void stopCatching() {
        if (null != catchingDispose && !catchingDispose.isDisposed()) {
            catchingDispose.dispose();
            catchingDispose = null;
            logger.debug("jinx://speech/catcher disabled.");
        }
    }

    private void initSpeaking() {
        if (speakerSetting.isEnabled()) {
            beginSpeaking();
        }
    }

    private synchronized void beginSpeaking() {
        logger.debug("jinx://speech/speaker enabled.");
    }

    private synchronized void stopSpeaking() {
        logger.debug("jinx://speech/speaker disabled.");
    }


    /**
     * @return 语音工具集
     */
    private Toolkit sppechToolkit() {
        return new Toolkit() {

            private final List<Tool> tools = List.of(
                    speakerSwitch(),
                    catcherSwitch(),
                    speechSettingShow()
            );

            @Override
            public @NonNull Iterator<Tool> iterator() {
                return tools.iterator();
            }

            private Tool speakerSwitch() {
                return FunctionTool.newBuilder()
                        .name("speech$speaker$switch")
                        .description("语音播报开关")
                        .parameterType(SwitchSpec.class)
                        .<SwitchSpec>function(spec -> {
                            final var speakingEnabled = spec.enabled();
                            speakerSetting.setEnabled(speakingEnabled);
                            if (speakingEnabled) {
                                beginSpeaking();
                            } else {
                                stopSpeaking();
                            }
                            return "SUCCESS";
                        })
                        .build();
            }

            private Tool catcherSwitch() {
                return FunctionTool.newBuilder()
                        .name("speech$catcher$switch")
                        .description("语音捕获开关")
                        .parameterType(SwitchSpec.class)
                        .<SwitchSpec>function(spec -> {
                            catcherSetting.setEnabled(spec.enabled());
                            if (spec.enabled()) {
                                beginCatching();
                            } else {
                                stopCatching();
                            }
                            return "SUCCESS";
                        })
                        .build();
            }

            private Tool speechSettingShow() {
                return FunctionTool.newBuilder()
                        .name("speech$setting$show")
                        .description("语音配置显示")
                        .parameterType(Object.class)
                        .function(u -> Map.of(
                                "speaker", speakerSetting,
                                "catcher", catcherSetting
                        ))
                        .build();
            }

            private record SwitchSpec(boolean enabled) {

            }

        };
    }

}
