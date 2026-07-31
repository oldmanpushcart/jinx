package io.github.oldmanpushcart.jinx.core.skill;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 运行时注册表
 * <p>
 * 维护当前运行的 Skill 配置注册表，是查询的唯一数据源，也是热加载的入口。
 * 启动时从 {@link SkillFileStore} 加载，运行时通过 register/unregister 实现热加载。
 * </p>
 */
@Singleton
public class SkillRuntimeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SkillRuntimeRegistry.class);

    private final SkillFileStore fileStore;
    private final Map<String, SkillFileEntry> entries = new ConcurrentHashMap<>();

    public SkillRuntimeRegistry(SkillFileStore fileStore) {
        this.fileStore = fileStore;
    }

    /**
     * 启动时从文件加载所有已启用的 Skill 配置
     */
    @PostConstruct
    void init() {
        final var loaded = fileStore.loadAll();
        for (final var entry : loaded) {
            if (entry.enabled()) {
                entries.put(entry.name(), entry);
                logger.info("jinx://skill/registry loaded: {} (enabled)", entry.name());
            } else {
                logger.info("jinx://skill/registry skipped (disabled): {}", entry.name());
            }
        }
        logger.info("jinx://skill/registry initialized with {} entries", entries.size());
    }

    /**
     * 注册一个 Skill（存入内存）
     *
     * @param entry Skill 配置条目
     */
    public void register(SkillFileEntry entry) {
        entries.put(entry.name(), entry);
        logger.info("jinx://skill/registry registered: {}", entry.name());
    }

    /**
     * 注销一个 Skill（从内存移除）
     *
     * @param name Skill 名称
     * @return 被移除的条目（如果存在）
     */
    public Optional<SkillFileEntry> unregister(String name) {
        final var removed = entries.remove(name);
        if (removed != null) {
            logger.info("jinx://skill/registry unregistered: {}", name);
        }
        return Optional.ofNullable(removed);
    }

    /**
     * 获取指定 Skill 条目
     *
     * @param name Skill 名称
     * @return Skill 配置条目（如果存在）
     */
    public Optional<SkillFileEntry> get(String name) {
        return Optional.ofNullable(entries.get(name));
    }

    /**
     * 返回所有已注册 Skill 的不可变视图
     *
     * @return Skill 配置条目集合
     */
    public Collection<SkillFileEntry> listAll() {
        return Collections.unmodifiableCollection(entries.values());
    }

    /**
     * 查询指定 Skill 是否启用
     *
     * @param name Skill 名称
     * @return 是否启用
     */
    public boolean isEnabled(String name) {
        final var entry = entries.get(name);
        return entry != null && entry.enabled();
    }

}
