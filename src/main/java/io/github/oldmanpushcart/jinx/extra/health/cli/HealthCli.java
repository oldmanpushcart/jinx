package io.github.oldmanpushcart.jinx.extra.health.cli;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

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
    public String description() {
        return "Health check.";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        return Mono.just("OK");
    }

}
