package io.github.oldmanpushcart.jinx.core.speech;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherSetting;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherLatcher;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerSetting;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;

/**
 * 音频模块启动器
 */
@Context
public class SpeechLatcher {

    private final Toolbox toolbox;
    private final SpeechToolkit speechToolkit;

    public SpeechLatcher(Toolbox toolbox, SpeakerSetting speakerSetting, CatcherSetting catcherSetting, CatcherLatcher catcherLatcher) {
        this.toolbox = toolbox;
        this.speechToolkit = new SpeechToolkit(speakerSetting, catcherSetting, catcherLatcher);
    }

    @PostConstruct
    void init() {
        toolbox.subscribeTools("speech", speechToolkit)
                .toCompletableFuture()
                .join();
    }

}
