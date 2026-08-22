package io.github.oldmanpushcart.jinx.extra.persona.cli;

import io.github.oldmanpushcart.jinx.cli.Cli;
import io.github.oldmanpushcart.jinx.extra.persona.Persona;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * persona 主命令 — 查看当前人格内容
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
    public String description() {
        return "Manage AI persona.";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var content = persona.content();
        return Mono.just(content.isBlank() ? "(persona is empty)" : content);
    }

}
