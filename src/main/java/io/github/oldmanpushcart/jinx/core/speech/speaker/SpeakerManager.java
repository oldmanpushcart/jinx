package io.github.oldmanpushcart.jinx.core.speech.speaker;

import java.util.concurrent.CompletionStage;

public interface SpeakerManager {

    boolean isEnabled();

    CompletionStage<Speaker> openSpeaker();

}
