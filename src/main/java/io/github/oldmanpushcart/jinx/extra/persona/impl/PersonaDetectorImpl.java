package io.github.oldmanpushcart.jinx.extra.persona.impl;

import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.core.detector.FileDetector;
import io.github.oldmanpushcart.jinx.extra.persona.Persona;
import io.github.oldmanpushcart.jinx.extra.persona.PersonaDetector;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 人格文件探测器
 * <p>
 * 探测{@code {jinx.data}/PERSONA.md}，文件变更时自动刷新{@link Persona}，
 * 文件被移除时清空人格内容。
 * </p>
 */
@Singleton
class PersonaDetectorImpl extends FileDetector<String> implements PersonaDetector {

    private static final String FILE_NAME = "PERSONA.md";

    private final Persona persona;

    PersonaDetectorImpl(Persona persona) {
        this.persona = persona;
    }

    @PostConstruct
    void init() {
        detectQuietly("init");
    }

    @Scheduled(fixedDelay = "10s")
    void scan() {
        detectQuietly("scan");
    }

    @Override
    public String toString() {
        return "jinx://persona/detector";
    }

    // ---- FileDetector钩子实现 ----

    @Override
    protected Path directory() {
        return Constants.DATA;
    }

    @Override
    protected Path pathOf(String name) {
        return Constants.DATA.resolve(FILE_NAME);
    }

    @Override
    protected boolean isTarget(Path path) {
        return Files.isRegularFile(path)
                && FILE_NAME.equals(path.getFileName().toString());
    }

    @Override
    protected String nameOf(Path path) {
        return NAME;
    }

    @Override
    protected String parse(Path path) throws IOException {
        return Files.readString(path, UTF_8);
    }

    @Override
    protected String nameOf(String item) {
        return NAME;
    }

    @Override
    protected Instant versionOf(Path path, String item) throws IOException {
        return Files.getLastModifiedTime(path).toInstant();
    }

    /**
     * 激活人格：刷新人格内容
     * <p>
     * 资源句柄关闭时（文件被移除逐出条目）清空人格内容。
     * </p>
     */
    @Override
    protected CompletionStage<AutoCloseable> activate(String name, String item, Instant version) {
        persona.refresh(item);
        return CompletableFuture.completedFuture(() -> {
            
        });
    }

}
