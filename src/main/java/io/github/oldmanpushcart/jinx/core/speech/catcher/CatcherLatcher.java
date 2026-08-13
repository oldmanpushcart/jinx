package io.github.oldmanpushcart.jinx.core.speech.catcher;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Context
public class CatcherLatcher {

    private static final Logger logger = LoggerFactory.getLogger(CatcherLatcher.class);
    private final CatcherSetting setting;
    private final Agent agent;
    private final CatcherManager catcherManager;
    private final CompletableFuture<?> initF = new CompletableFuture<>();
    private volatile Disposable catchingDispose;

    public CatcherLatcher(CatcherSetting setting, Agent agent, CatcherManager catcherManager) {
        this.setting = setting;
        this.agent = agent;
        this.catcherManager = catcherManager;
    }

    @PostConstruct
    void init() {
        if (initF.complete(null)) {
            if (setting.isEnabled()) {
                begin();
            }
        }
    }

    public synchronized void begin() {

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
        logger.debug("jinx://speech/speaker enabled.");
    }

    public synchronized void stop() {
        if (null != catchingDispose && !catchingDispose.isDisposed()) {
            catchingDispose.dispose();
            catchingDispose = null;
            logger.debug("jinx://speech/speaker disabled.");
        }
    }

}
