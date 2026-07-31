package io.github.oldmanpushcart.jinx.core;

import io.github.oldmanpushcart.jinx.core.mcp.McpConfig;
import io.github.oldmanpushcart.jinx.core.mcp.McpFileEntry;
import io.github.oldmanpushcart.jinx.core.mcp.McpFileStore;
import io.github.oldmanpushcart.jinx.core.mcp.McpRuntimeRegistry;
import io.github.oldmanpushcart.jinx.core.skill.SkillFileEntry;
import io.github.oldmanpushcart.jinx.core.skill.SkillFileStore;
import io.github.oldmanpushcart.jinx.core.skill.SkillRuntimeRegistry;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 配置管理工具
 * <p>
 * 封装所有配置管理操作，每个操作遵循"先写文件，再刷内存"的双通道模式。
 * 查询操作从内存注册表读取，不读文件。
 * </p>
 */
@Singleton
public class ConfigTools {

    private static final Logger logger = LoggerFactory.getLogger(ConfigTools.class);

    private final McpFileStore mcpFileStore;
    private final McpRuntimeRegistry mcpRegistry;
    private final SkillFileStore skillFileStore;
    private final SkillRuntimeRegistry skillRegistry;
    private final JinxSettings settings;

    public ConfigTools(McpFileStore mcpFileStore, McpRuntimeRegistry mcpRegistry,
                       SkillFileStore skillFileStore, SkillRuntimeRegistry skillRegistry,
                       JinxSettings settings) {
        this.mcpFileStore = mcpFileStore;
        this.mcpRegistry = mcpRegistry;
        this.skillFileStore = skillFileStore;
        this.skillRegistry = skillRegistry;
        this.settings = settings;
    }

    // ==================== MCP 管理 ====================

    /**
     * 新增 HTTP 类型的 MCP 工具
     *
     * @param name     MCP 工具名称
     * @param type     服务器类型（sse / streamable-http）
     * @param baseUrl  基础 URL
     * @param endpoint 端点路径
     * @param headers  请求头（可选）
     * @param enabled  是否启用
     */
    public void mcpAddHttp(String name, McpConfig.Server.Type type, String baseUrl,
                           String endpoint, Map<String, String> headers, boolean enabled) throws IOException {
        if (mcpFileStore.exists(name)) {
            throw new IllegalArgumentException("MCP '%s' already exists".formatted(name));
        }
        final var server = new McpConfig.HttpServer(type, new URL(baseUrl), endpoint, headers);
        final var entry = new McpFileEntry(name, enabled, server);

        // 1. 持久化到文件
        mcpFileStore.save(entry);
        // 2. 热加载到内存
        if (enabled) {
            mcpRegistry.register(entry);
        }
        logger.info("jinx://config-tools mcp added: {} (type={}, enabled={})", name, type, enabled);
    }

    /**
     * 新增 Stdio 类型的 MCP 工具
     *
     * @param name    MCP 工具名称
     * @param cmd     命令
     * @param args    参数列表
     * @param env     环境变量
     * @param enabled 是否启用
     */
    public void mcpAddStdio(String name, String cmd, java.util.List<String> args,
                            Map<String, String> env, boolean enabled) throws IOException {
        if (mcpFileStore.exists(name)) {
            throw new IllegalArgumentException("MCP '%s' already exists".formatted(name));
        }
        final var server = new McpConfig.StdioServer(McpConfig.Server.Type.STDIO, cmd, args, env);
        final var entry = new McpFileEntry(name, enabled, server);

        mcpFileStore.save(entry);
        if (enabled) {
            mcpRegistry.register(entry);
        }
        logger.info("jinx://config-tools mcp added: {} (type=stdio, enabled={})", name, enabled);
    }

    /**
     * 删除 MCP 工具
     *
     * @param name MCP 工具名称
     */
    public boolean mcpRemove(String name) throws IOException {
        // 1. 删除文件
        final var deleted = mcpFileStore.delete(name);
        // 2. 从内存注销
        mcpRegistry.unregister(name);
        logger.info("jinx://config-tools mcp removed: {} (deleted={})", name, deleted);
        return deleted;
    }

    /**
     * 启用 MCP 工具
     *
     * @param name MCP 工具名称
     */
    public void mcpEnable(String name) throws IOException {
        final var entry = mcpFileStore.loadAll().stream()
                .filter(e -> e.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("MCP '%s' not found in files".formatted(name)));

        final var updated = new McpFileEntry(entry.name(), true, entry.server());
        mcpFileStore.save(updated);
        mcpRegistry.register(updated);
        logger.info("jinx://config-tools mcp enabled: {}", name);
    }

    /**
     * 禁用 MCP 工具
     *
     * @param name MCP 工具名称
     */
    public void mcpDisable(String name) throws IOException {
        final var entry = mcpFileStore.loadAll().stream()
                .filter(e -> e.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("MCP '%s' not found in files".formatted(name)));

        final var updated = new McpFileEntry(entry.name(), false, entry.server());
        mcpFileStore.save(updated);
        mcpRegistry.unregister(name);
        logger.info("jinx://config-tools mcp disabled: {}", name);
    }

    /**
     * 列出所有已注册的 MCP 工具
     *
     * @return MCP 配置条目集合
     */
    public Collection<McpFileEntry> mcpList() {
        return mcpRegistry.listAll();
    }

    /**
     * 查询指定 MCP 工具
     *
     * @param name MCP 工具名称
     * @return MCP 配置条目（如果存在）
     */
    public Optional<McpFileEntry> mcpGet(String name) {
        return mcpRegistry.get(name);
    }

    // ==================== Skill 管理 ====================

    /**
     * 新增一个 Skill
     *
     * @param name        Skill 名称
     * @param description Skill 描述
     * @param content     Skill 的 Markdown 内容
     * @param enabled     是否启用
     */
    public void skillAdd(String name, String description, String content, boolean enabled) throws IOException {
        if (skillFileStore.exists(name)) {
            throw new IllegalArgumentException("Skill '%s' already exists".formatted(name));
        }
        final var entry = new SkillFileEntry(name, enabled, description, content);

        // 1. 持久化到文件（配置文件 + 技能内容文件）
        skillFileStore.save(entry);
        // 2. 热加载到内存
        if (enabled) {
            skillRegistry.register(entry);
        }
        logger.info("jinx://config-tools skill added: {} (enabled={})", name, enabled);
    }

    /**
     * 删除 Skill
     *
     * @param name Skill 名称
     */
    public boolean skillRemove(String name) throws IOException {
        // 1. 删除文件
        final var deleted = skillFileStore.delete(name);
        // 2. 从内存注销
        skillRegistry.unregister(name);
        logger.info("jinx://config-tools skill removed: {} (deleted={})", name, deleted);
        return deleted;
    }

    /**
     * 启用 Skill
     *
     * @param name Skill 名称
     */
    public void skillEnable(String name) throws IOException {
        final var entry = skillFileStore.loadAll().stream()
                .filter(e -> e.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Skill '%s' not found in files".formatted(name)));

        final var updated = new SkillFileEntry(entry.name(), true, entry.description(), entry.content());
        skillFileStore.save(updated);
        skillRegistry.register(updated);
        logger.info("jinx://config-tools skill enabled: {}", name);
    }

    /**
     * 禁用 Skill
     *
     * @param name Skill 名称
     */
    public void skillDisable(String name) throws IOException {
        final var entry = skillFileStore.loadAll().stream()
                .filter(e -> e.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Skill '%s' not found in files".formatted(name)));

        final var updated = new SkillFileEntry(entry.name(), false, entry.description(), entry.content());
        skillFileStore.save(updated);
        skillRegistry.unregister(name);
        logger.info("jinx://config-tools skill disabled: {}", name);
    }

    /**
     * 列出所有已注册的 Skill
     *
     * @return Skill 配置条目集合
     */
    public Collection<SkillFileEntry> skillList() {
        return skillRegistry.listAll();
    }

    /**
     * 查询指定 Skill
     *
     * @param name Skill 名称
     * @return Skill 配置条目（如果存在）
     */
    public Optional<SkillFileEntry> skillGet(String name) {
        return skillRegistry.get(name);
    }

    // ==================== 功能开关管理 ====================

    /**
     * 查询语音播报开关
     *
     * @return 是否启用
     */
    public boolean isSpeakerEnabled() {
        return settings.isSpeakerEnabled();
    }

    /**
     * 设置语音播报开关
     *
     * @param enabled 是否启用
     */
    public void setSpeakerEnabled(boolean enabled) {
        settings.setSpeakerEnabled(enabled);
    }

    /**
     * 查询语音输入开关
     *
     * @return 是否启用
     */
    public boolean isCatcherEnabled() {
        return settings.isCatcherEnabled();
    }

    /**
     * 设置语音输入开关
     *
     * @param enabled 是否启用
     */
    public void setCatcherEnabled(boolean enabled) {
        settings.setCatcherEnabled(enabled);
    }

}
