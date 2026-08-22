package io.github.oldmanpushcart.jinx.extra.common;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * health — 健康检查
 */
@Singleton
class HealthCli implements Cli {

    @Override
    public String command() {
        return "health";
    }

    @Override
    public List<Item> usage() {
        return List.of(new Item("health", "Health check."));
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        return Mono.just("OK");
    }

}
