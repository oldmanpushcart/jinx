package io.github.oldmanpushcart.jinx.core.speech.catcher;

import org.reactivestreams.Publisher;

public interface CatcherManager {

    boolean isEnabled();

    Publisher<String> catching();

}
