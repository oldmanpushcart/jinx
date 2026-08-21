package io.github.oldmanpushcart.jinx.core.speech;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherManager;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherSetting;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerManager;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerSetting;
import io.micronaut.context.annotation.Context;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


/**
 * 音频模块启动器
 */
@Context
class SpeechLatcher {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Agent agent;

    private final SpeakerSetting speakerSetting;
    private final SpeakerManager speakerManager;

    private final CatcherSetting catcherSetting;
    private final CatcherManager catcherManager;

    private volatile Disposable catchingDispose;

    public SpeechLatcher(
            final Agent agent,
            final SpeakerSetting speakerSetting,
            final SpeakerManager speakerManager,
            final CatcherSetting catcherSetting,
            final CatcherManager catcherManager
    ) {
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

    @EventListener
    void onSwitchEvent(SpeechSwitchEvent event) {
        switch (event.type()) {
            case SPEAKER -> {
                speakerSetting.setEnabled(event.enabled());
                if (event.enabled()) {
                    beginSpeaking();
                } else {
                    stopSpeaking();
                }
            }
            case CATCHER -> {
                catcherSetting.setEnabled(event.enabled());
                if (event.enabled()) {
                    beginCatching();
                } else {
                    stopCatching();
                }
            }
        }
    }

}
