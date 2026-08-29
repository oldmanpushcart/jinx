package io.github.oldmanpushcart.jinx.extra.prompts.impl;

import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.core.detector.FileDetector;
import io.github.oldmanpushcart.jinx.extra.prompts.PromptMeta;
import io.github.oldmanpushcart.jinx.extra.prompts.PromptDetector;
import io.github.oldmanpushcart.jinx.extra.prompts.PromptPhase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 提示词文件探测器
 * <p>
 * 扫描{@code {jinx.data}/prompts/{stage}}目录，将每个{@code *.md}文件解析为一条提示词。
 * 提示词内容为静态文本，不做任何变量替换。
 * </p>
 */
abstract class PromptFileDetector extends FileDetector<PromptMeta> implements PromptDetector {

    private static final String EXTENSION = ".md";

    private final PromptPhase phase;

    protected PromptFileDetector(PromptPhase phase) {
        this.phase = phase;
    }

    @Override
    public PromptPhase phase() {
        return phase;
    }

    @Override
    public String toString() {
        return "jinx://prompts/%s/detector".formatted(phase.directory());
    }

    // ---- FileDetector钩子实现 ----

    @Override
    protected Path directory() {
        return Constants.DATA.resolve("prompts").resolve(phase.directory());
    }

    @Override
    protected Path pathOf(String name) {
        return directory().resolve(name + EXTENSION);
    }

    @Override
    protected boolean isTarget(Path path) {
        return Files.isRegularFile(path)
                && path.getFileName().toString().endsWith(EXTENSION);
    }

    @Override
    protected String nameOf(Path path) {
        final var fileName = path.getFileName().toString();
        return fileName.substring(0, fileName.length() - EXTENSION.length());
    }

    @Override
    protected PromptMeta parse(Path path) throws IOException {
        return new PromptMeta(nameOf(path), Files.readString(path, UTF_8));
    }

    @Override
    protected String nameOf(PromptMeta item) {
        return item.name();
    }

    @Override
    protected Instant versionOf(Path path, PromptMeta item) throws IOException {
        return Files.getLastModifiedTime(path).toInstant();
    }

    /**
     * 激活提示词：无资源需要注册，注册表本身即事实源，直接返回空句柄。
     */
    @Override
    protected CompletionStage<AutoCloseable> activate(String name, PromptMeta item, Instant version) {
        return CompletableFuture.completedFuture(() -> {

        });
    }

}
