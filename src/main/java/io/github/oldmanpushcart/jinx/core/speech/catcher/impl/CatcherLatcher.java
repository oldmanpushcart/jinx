package io.github.oldmanpushcart.jinx.core.speech.catcher.impl;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherConfig;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherManager;
import io.micronaut.context.annotation.Context;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Context
public class CatcherLatcher {

    public CatcherLatcher(CatcherConfig config, Agent agent, CatcherManager catcherManager) {
        if (config.enabled()) {
            init(agent, catcherManager);
        }
    }

    private void init(Agent agent, CatcherManager catcherManager) {
        final var disposeRef = new AtomicReference<Disposable>();
        Flux.from(catcherManager.catching())
                .subscribe(text -> {
                    final var dispose = Flux.from(agent.flow("SPEECH-SESSION-001", Message.user(text)))
                            .subscribe();

                    //Mono.fromCompletionStage()
                    Optional.ofNullable(disposeRef.getAndSet(dispose))
                            .ifPresent(Disposable::dispose);

                })
        ;
    }

}
