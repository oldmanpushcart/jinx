package io.github.oldmanpushcart.jinx.core;

import org.reactivestreams.Publisher;

import java.util.Map;

public interface CommandHandler {
    
    Publisher<String> handle(Map<String, String> params);
}
