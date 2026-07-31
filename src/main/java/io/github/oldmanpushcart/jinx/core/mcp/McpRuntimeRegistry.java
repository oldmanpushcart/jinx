package io.github.oldmanpushcart.jinx.core.mcp;

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
 * MCP 运行时注册表
 * <p>
 * 维护当前运行的 MCP 配置注册表，是查询的唯一数据源，也是热加载的入口。
 * 启动时从 {@link McpFileStore} 加载，运行时通过 register/unregister 实现热加载。
 * </p>
 */
@Singleton
public class McpRuntimeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(McpRuntimeRegistry.class);

    private final McpFileStore fileStore;
    private final Map<String, McpFileEntry> entries = new ConcurrentHashMap<>();

    public McpRuntimeRegistry(McpFileStore fileStore) {
        this.fileStore = fileStore;
    }

    /**
     * 启动时从文件加载所有已启用的 MCP 配置
     */
    @PostConstruct
    void init() {
        final var loaded = fileStore.loadAll();
        for (final var entry : loaded) {
            if (entry.enabled()) {
                entries.put(entry.name(), entry);
                logger.info("jinx://mcp/registry loaded: {} ({})", entry.name(), entry.server().type());
            } else {
                logger.info("jinx://mcp/registry skipped (disabled): {}", entry.name());
            }
        }
        logger.info("jinx://mcp/registry initialized with {} entries", entries.size());
    }

    /**
     * 注册一个 MCP（存入内存）
     *
     * @param entry MCP 配置条目
     */
    public void register(McpFileEntry entry) {
        entries.put(entry.name(), entry);
        logger.info("jinx://mcp/registry registered: {}", entry.name());
    }

    /**
     * 注销一个 MCP（从内存移除）
     *
     * @param name MCP 工具名称
     * @return 被移除的条目（如果存在）
     */
    public Optional<McpFileEntry> unregister(String name) {
        final var removed = entries.remove(name);
        if (removed != null) {
            logger.info("jinx://mcp/registry unregistered: {}", name);
        }
        return Optional.ofNullable(removed);
    }

    /**
     * 获取指定 MCP 条目
     *
     * @param name MCP 工具名称
     * @return MCP 配置条目（如果存在）
     */
    public Optional<McpFileEntry> get(String name) {
        return Optional.ofNullable(entries.get(name));
    }

    /**
     * 返回所有已注册 MCP 的不可变视图
     *
     * @return MCP 配置条目集合
     */
    public Collection<McpFileEntry> listAll() {
        return Collections.unmodifiableCollection(entries.values());
    }

    /**
     * 查询指定 MCP 是否启用
     *
     * @param name MCP 工具名称
     * @return 是否启用
     */
    public boolean isEnabled(String name) {
        final var entry = entries.get(name);
        return entry != null && entry.enabled();
    }

}
