package io.github.oldmanpushcart.jinx.extra.common;

import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

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
    public List<Item> usage() {
        return List.of(new Item("version", "Show version information."));
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        return Mono.just(Constants.VERSION);
    }

}
