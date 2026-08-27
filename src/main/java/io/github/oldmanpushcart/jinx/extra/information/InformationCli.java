package io.github.oldmanpushcart.jinx.extra.information;

import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * info — 显示运行时信息
 */
@Singleton
class InformationCli implements Cli {

    private final Information information;

    public InformationCli(Information information) {
        this.information = information;
    }

    @Override
    public String command() {
        return "info";
    }

    @Override
    public List<Item> usage() {
        return List.of(new Item("info", "Show runtime information."));
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var content = CommonUtils.isNotBlankString(ctx.sessionId())
                ? "%s\nSESSION: %s".formatted(information.content(), ctx.sessionId())
                : information.content();
        return Mono.just(content);
    }

}
