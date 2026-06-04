package io.github.oldmanpushcart.jinx.core.speech;

import org.reactivestreams.Publisher;

import java.util.concurrent.CompletionStage;

public interface Speaker {

    CompletionStage<Void> speak(Publisher<String> flow);

}
