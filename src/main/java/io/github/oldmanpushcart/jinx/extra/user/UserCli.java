package io.github.oldmanpushcart.jinx.extra.user;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * user — 管理用户档案
 */
@Singleton
class UserCli implements Cli {

    private final User user;
    private final UserDetector detector;

    public UserCli(User user, UserDetector detector) {
        this.user = user;
        this.detector = detector;
    }

    @Override
    public String command() {
        return "user";
    }

    @Override
    public List<Item> usage() {
        return List.of(
                new Item("user", "Show user profile."),
                new Item("user reload", "Reload user profile from file.")
        );
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();
        if (!args.isEmpty() && "reload".equals(args.get(0))) {
            return Mono.fromCompletionStage(detector.reload(UserDetector.NAME))
                    .map(_content -> "User profile reloaded.")
                    .onErrorResume(ex -> Mono.just("User profile reload failed: %s".formatted(ex.getMessage())));
        }

        final var content = user.content();
        return Mono.just(content.isBlank() ? "(user profile is empty)" : content);
    }

}
