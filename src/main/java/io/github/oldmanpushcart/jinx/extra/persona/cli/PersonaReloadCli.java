package io.github.oldmanpushcart.jinx.extra.persona.cli;

import io.github.oldmanpushcart.jinx.cli.Cli;
import io.github.oldmanpushcart.jinx.extra.persona.Persona;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * persona reload — 从文件重新加载人格
 */
@Singleton
class PersonaReloadCli implements Cli {

    private final Persona persona;

    public PersonaReloadCli(Persona persona) {
        this.persona = persona;
    }

    @Override
    public String command() {
        return "persona";
    }

    @Override
    public String sub() {
        return "reload";
    }

    @Override
    public String description() {
        return "Reload persona from file";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        return Mono.fromCallable(() -> {
            persona.load();
            return "Persona reloaded.";
        });
    }

}
