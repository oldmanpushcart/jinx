package io.github.oldmanpushcart.jinx.extra.persona;

import io.github.oldmanpushcart.jinx.Constants;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 人格
 */
@Singleton
public class Persona {

    private final Path personaPath;
    private final AtomicReference<String> contentRef = new AtomicReference<>("");

    public Persona() {
        this.personaPath = Constants.DATA.resolve("PERSONA.md");
    }

    /**
     * @return 人格内容
     */
    public String content() {
        return contentRef.get();
    }

    /**
     * 加载人格
     *
     * @return 人格内容
     * @throws IOException 加载失败
     */
    @PostConstruct
    public Persona load() throws IOException {
        if (!Files.isReadable(personaPath)) {
            return this;
        }
        final var content = Files.readString(personaPath, UTF_8);
        contentRef.set(content);
        return this;
    }

}
