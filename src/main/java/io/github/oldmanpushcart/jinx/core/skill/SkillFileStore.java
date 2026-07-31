package io.github.oldmanpushcart.jinx.core.skill;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Skill 配置文件存储
 * <p>
 * 负责扫描 conf/skills/ 目录下的 .skill.json 文件，完成文件的读取、写入、删除。
 * 同时管理 skills 目录下的 .md 技能文件的写入。
 * </p>
 */
@Singleton
public class SkillFileStore {

    private static final Logger logger = LoggerFactory.getLogger(SkillFileStore.class);
    private static final String SKILL_FILE_SUFFIX = ".skill.json";

    private final SkillConfig config;
    private final ObjectMapper mapper;

    public SkillFileStore(SkillConfig config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    /**
     * 加载所有 Skill 配置条目
     *
     * @return Skill 配置条目列表（按名称排序）
     */
    public List<SkillFileEntry> loadAll() {
        final var dir = config.configDirectory();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (final var stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(SKILL_FILE_SUFFIX))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(this::readEntry)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            logger.error("jinx://skill/file-store list directory failed: {}", dir, e);
            return List.of();
        }
    }

    /**
     * 保存 Skill 配置条目
     * <p>
     * 同时写入配置文件（conf/skills/）和技能内容文件（skills/）。
     * </p>
     *
     * @param entry Skill 配置条目
     */
    public void save(SkillFileEntry entry) throws IOException {
        // 1. 写入配置文件
        final var configDir = config.configDirectory();
        Files.createDirectories(configDir);
        final var configFile = configDir.resolve(entry.name() + SKILL_FILE_SUFFIX);
        final var map = toMap(entry);
        mapper.writerWithDefaultPrettyPrinter().writeValue(configFile.toFile(), map);
        logger.info("jinx://skill/file-store saved config: {}", configFile);

        // 2. 写入技能内容文件到第一个 skills 目录
        if (entry.content() != null && !entry.content().isEmpty()) {
            writeSkillContent(entry.name(), entry.content());
        }
    }

    /**
     * 删除 Skill 配置和技能文件
     *
     * @param name Skill 名称
     * @return 是否成功删除
     */
    public boolean delete(String name) throws IOException {
        boolean deleted = false;

        // 1. 删除配置文件
        final var configFile = config.configDirectory().resolve(name + SKILL_FILE_SUFFIX);
        if (Files.exists(configFile)) {
            Files.delete(configFile);
            logger.info("jinx://skill/file-store deleted config: {}", configFile);
            deleted = true;
        }

        // 2. 删除技能内容文件
        deleteSkillContent(name);

        return deleted;
    }

    /**
     * 判断 Skill 配置文件是否存在
     *
     * @param name Skill 名称
     * @return 是否存在
     */
    public boolean exists(String name) {
        final var file = config.configDirectory().resolve(name + SKILL_FILE_SUFFIX);
        return Files.exists(file);
    }

    // --- private ---

    private SkillFileEntry readEntry(Path file) {
        try {
            final var map = mapper.readValue(file.toFile(), new TypeReference<Map<String, Object>>() {});
            final var name = (String) map.get("name");
            final var enabled = (Boolean) map.getOrDefault("enabled", true);
            final var description = (String) map.getOrDefault("description", "");
            final var content = (String) map.getOrDefault("content", "");
            return new SkillFileEntry(
                    name != null ? name : nameFromFile(file),
                    enabled,
                    description,
                    content
            );
        } catch (Exception e) {
            logger.warn("jinx://skill/file-store read failed: {}", file, e);
            return null;
        }
    }

    private Map<String, Object> toMap(SkillFileEntry entry) {
        final var map = new LinkedHashMap<String, Object>();
        map.put("name", entry.name());
        map.put("enabled", entry.enabled());
        if (entry.description() != null && !entry.description().isEmpty()) {
            map.put("description", entry.description());
        }
        if (entry.content() != null && !entry.content().isEmpty()) {
            map.put("content", entry.content());
        }
        return map;
    }

    private String nameFromFile(Path file) {
        final var fileName = file.getFileName().toString();
        return fileName.substring(0, fileName.length() - SKILL_FILE_SUFFIX.length());
    }

    private void writeSkillContent(String name, String content) throws IOException {
        final var directories = config.directories();
        if (directories == null || directories.isEmpty()) {
            return;
        }
        final var skillDir = directories.get(0);
        Files.createDirectories(skillDir);
        final var skillFile = skillDir.resolve(name + ".md");
        Files.writeString(skillFile, content);
        logger.info("jinx://skill/file-store saved content: {}", skillFile);
    }

    private void deleteSkillContent(String name) throws IOException {
        final var directories = config.directories();
        if (directories == null || directories.isEmpty()) {
            return;
        }
        for (final var skillDir : directories) {
            final var skillFile = skillDir.resolve(name + ".md");
            if (Files.exists(skillFile)) {
                Files.delete(skillFile);
                logger.info("jinx://skill/file-store deleted content: {}", skillFile);
            }
        }
    }

}
