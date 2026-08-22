package io.github.oldmanpushcart.jinx.extra.persona;

import io.github.oldmanpushcart.jinx.cli.Cli;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * persona — 管理 AI 人格
 */
@Singleton
class PersonaCli implements Cli {

    private final Persona persona;

    public PersonaCli(Persona persona) {
        this.persona = persona;
    }

    @Override
    public String command() {
        return "persona";
    }

    @Override
    public List<Item> usage() {
        return List.of(
                new Item("persona", "Manage AI persona."),
                new Item("persona reload", "Reload persona from file.")
        );
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();
        if (!args.isEmpty() && "reload".equals(args.get(0))) {
            return Mono.fromCallable(() -> {
                persona.load();
                return "Persona reloaded.";
            });
        }

        final var content = persona.content();
        return Mono.just(content.isBlank() ? "(persona is empty)" : content);
    }

}
