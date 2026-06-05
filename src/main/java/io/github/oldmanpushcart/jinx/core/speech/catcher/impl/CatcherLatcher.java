package io.github.oldmanpushcart.jinx.core.speech.catcher.impl;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherManager;
import io.micronaut.context.annotation.Context;
import reactor.core.publisher.Flux;

@Context
public class CatcherLatcher {



    public CatcherLatcher(Agent agent, CatcherManager catcherManager) {
        init(agent, catcherManager);
    }

    private void init(Agent agent, CatcherManager catcherManager) {
        Flux.from(catcherManager.openCatcher())
                .subscribe(text-> {
                    System.out.println("====="+text);
                })
        ;
    }

}
