package io.github.oldmanpushcart.jinx.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Jinx 运行时功能开关
 * <p>
 * 使用 AtomicBoolean 持有运行时可调的开关值，支持动态切换和持久化。
 * 查询走内存（实时），修改时同时更新内存和文件。
 * </p>
 */
@Singleton
public class JinxSettings {

    private static final Logger logger = LoggerFactory.getLogger(JinxSettings.class);

    private static final Path JINX_YML = Path.of("./conf/jinx.yml");

    private final AtomicBoolean speakerEnabled = new AtomicBoolean(true);
    private final AtomicBoolean catcherEnabled = new AtomicBoolean(false);

    /**
     * 启动时从 jinx.yml 加载初始值
     */
    @PostConstruct
    void init() {
        try {
            if (Files.exists(JINX_YML)) {
                final var yamlMapper = new YAMLMapper();
                final var root = yamlMapper.readTree(JINX_YML.toFile());
                final var speakerNode = root.path("jinx").path("speech").path("speaker").path("enabled");
                final var catcherNode = root.path("jinx").path("speech").path("catcher").path("enabled");

                if (!speakerNode.isMissingNode()) {
                    speakerEnabled.set(speakerNode.asBoolean());
                }
                if (!catcherNode.isMissingNode()) {
                    catcherEnabled.set(catcherNode.asBoolean());
                }
            }
            logger.info("jinx://settings initialized: speaker={}, catcher={}",
                    speakerEnabled.get(), catcherEnabled.get());
        } catch (IOException e) {
            logger.warn("jinx://settings failed to load from {}, using defaults", JINX_YML, e);
        }
    }

    /**
     * 查询语音播报是否启用
     *
     * @return 是否启用
     */
    public boolean isSpeakerEnabled() {
        return speakerEnabled.get();
    }

    /**
     * 查询语音输入是否启用
     *
     * @return 是否启用
     */
    public boolean isCatcherEnabled() {
        return catcherEnabled.get();
    }

    /**
     * 设置语音播报开关
     *
     * @param enabled 是否启用
     */
    public synchronized void setSpeakerEnabled(boolean enabled) {
        speakerEnabled.set(enabled);
        persistSpeechSetting("speaker", enabled);
        logger.info("jinx://settings speaker {}", enabled ? "enabled" : "disabled");
    }

    /**
     * 设置语音输入开关
     *
     * @param enabled 是否启用
     */
    public synchronized void setCatcherEnabled(boolean enabled) {
        catcherEnabled.set(enabled);
        persistSpeechSetting("catcher", enabled);
        logger.info("jinx://settings catcher {}", enabled ? "enabled" : "disabled");
    }

    // --- private ---

    private void persistSpeechSetting(String component, boolean enabled) {
        try {
            if (!Files.exists(JINX_YML)) {
                return;
            }
            final var yamlMapper = new YAMLMapper();
            final var root = yamlMapper.readTree(JINX_YML.toFile());
            final var node = (ObjectNode) root.path("jinx").path("speech").path(component);
            node.put("enabled", enabled);
            yamlMapper.writerWithDefaultPrettyPrinter().writeValue(JINX_YML.toFile(), root);
        } catch (IOException e) {
            logger.warn("jinx://settings failed to persist {} enabled={}", component, enabled, e);
        }
    }

}
