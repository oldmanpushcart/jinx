package io.github.oldmanpushcart.jinx.extra.cron;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.toolkit.ToolkitToolSource;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时任务生命周期管理
 * <p>
 * 负责将 CronToolkit 注册到 Toolbox 中。
 * </p>
 */
@Singleton
class CronLatcher {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Toolbox toolbox;
    private final CronDetector detector;
    private final Set<AutoCloseable> subscriptions = ConcurrentHashMap.newKeySet();

    public CronLatcher(Toolbox toolbox, CronDetector detector) {
        this.toolbox = toolbox;
        this.detector = detector;
    }

    @PostConstruct
    void init() {
        final var cronToolkit = new CronToolkit(detector);

        final var source = ToolkitToolSource.newBuilder()
                .namespace("jinx")
                .append(cronToolkit)
                .build();

        subscriptions.add(source);

        CompletableFuture.completedStage(null)
                .thenCompose(_u -> source.initialize())
                .thenCompose(toolbox::subscribe)
                .thenAccept(sub -> {
                    subscriptions.add(sub);
                    logger.debug("jinx://cron/latcher registered cron$add tool.");
                })
                .exceptionally(ex -> {
                    logger.warn("jinx://cron/latcher failed to register cron$add tool.", ex);
                    return null;
                })
                .toCompletableFuture()
                .join();
    }

    @PreDestroy
    void destroy() {
        subscriptions.forEach(IOUtils::closeQuietly);
    }

}
