package io.github.oldmanpushcart.jinx.extra.version.cli;

import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * version — 显示版本信息
 */
@Singleton
class VersionCli implements Cli {

    @Override
    public String command() {
        return "version";
    }

    @Override
    public String description() {
        return "Show version information.";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        return Mono.just(Constants.VERSION);
    }

}
