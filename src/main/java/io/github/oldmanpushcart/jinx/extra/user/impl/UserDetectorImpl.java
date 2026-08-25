package io.github.oldmanpushcart.jinx.extra.user.impl;

import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.core.detector.FileDetector;
import io.github.oldmanpushcart.jinx.extra.user.User;
import io.github.oldmanpushcart.jinx.extra.user.UserDetector;
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
 * 用户档案文件探测器
 * <p>
 * 探测{@code {jinx.data}/USER.md}，文件变更时自动刷新{@link User}，
 * 文件被移除时清空用户档案内容。
 * </p>
 */
@Singleton
class UserDetectorImpl extends FileDetector<String> implements UserDetector {

    private static final String FILE_NAME = "USER.md";

    private final User user;

    UserDetectorImpl(User user) {
        this.user = user;
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
        return "jinx://user/detector";
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
     * 激活用户档案：刷新用户档案内容
     * <p>
     * 资源句柄关闭时（文件被移除逐出条目）清空用户档案内容。
     * </p>
     */
    @Override
    protected CompletionStage<AutoCloseable> activate(String name, String item, Instant version) {
        user.refresh(item);
        return CompletableFuture.completedFuture(() -> user.refresh(""));
    }

}
