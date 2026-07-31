package io.github.oldmanpushcart.jinx.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolExecutionException;
import io.github.oldmanpushcart.jinx.core.mcp.McpConfig;

import java.util.List;
import java.util.Map;

/**
 * Jinx 配置管理工具包
 * <p>
 * 提供给 Agent 在对话中管理 MCP、查看功能开关的工具集。
 * 所有操作通过 {@link ConfigTools} 执行，遵循"先写文件，再刷内存"的双通道模式。
 * </p>
 */
public class JinxConfigToolkit implements Toolkit {

    private final ConfigTools configTools;

    private JinxConfigToolkit(ConfigTools configTools) {
        this.configTools = configTools;
    }

    public static JinxConfigToolkit create(ConfigTools configTools) {
        return new JinxConfigToolkit(configTools);
    }

    @Override
    public List<Tool> tools() {
        return List.of(
                mcpList(),
                mcpAdd(),
                mcpRemove(),
                mcpEnable(),
                mcpDisable(),
                skillList(),
                skillAdd(),
                skillRemove(),
                skillEnable(),
                skillDisable(),
                settingGet(),
                settingSet()
        );
    }

    // ==================== MCP 管理工具 ====================

    private FunctionTool mcpList() {
        return FunctionTool.newBuilder()
                .name("jinx_mcp_list")
                .description("""
                        列出当前所有已启用的 MCP 工具。
                        
                        【返回结果】
                        - mcps: MCP 工具列表，每项包含 name、type、enabled 等信息
                        - total: MCP 工具总数
                        
                        【使用场景】
                        - 查看当前接入了哪些 MCP 工具
                        - 确认某个 MCP 是否已启用
                        """)
                .supplier(() -> {
                    final var entries = configTools.mcpList();
                    final var mcps = entries.stream()
                            .map(e -> Map.of(
                                    "name", e.name(),
                                    "type", e.server().type().name().toLowerCase(),
                                    "enabled", e.enabled()
                            ))
                            .toList();
                    return Map.of(
                            "mcps", mcps,
                            "total", mcps.size()
                    );
                })
                .build();
    }

    private FunctionTool mcpAdd() {
        return FunctionTool.newBuilder()
                .name("jinx_mcp_add")
                .description("""
                        新增一个 MCP 工具接入。
                        
                        【参数说明】
                        - name: MCP 工具名称（唯一标识，如 amap、weather）
                        - type: 服务器类型，支持 streamable-http、sse、stdio
                        - base_url: 服务基础 URL（HTTP 类型必填）
                        - endpoint: 服务端点路径（HTTP 类型必填）
                        
                        【使用场景】
                        - 接入新的 MCP 工具服务
                        - 扩展 Agent 的工具能力
                        
                        【注意事项】
                        - name 不能与已有 MCP 重复
                        - HTTP 类型需要提供 base_url 和 endpoint
                        """)
                .parameterType(McpAddSpec.class)
                .<McpAddSpec>function((caller, spec) -> {
                    try {
                        final var type = switch (spec.type().toLowerCase()) {
                            case "streamable-http" -> McpConfig.Server.Type.STREAMABLE_HTTP;
                            case "sse" -> McpConfig.Server.Type.SSE;
                            default -> throw ToolExecutionException.callFailed(
                                    "jinx_mcp_add",
                                    "Unsupported type: " + spec.type(),
                                    "Use: streamable-http, sse, or stdio"
                            );
                        };
                        configTools.mcpAddHttp(spec.name(), type, spec.baseUrl(), spec.endpoint(), null, true);
                        return Map.of(
                                "success", true,
                                "message", "MCP '%s' added successfully".formatted(spec.name())
                        );
                    } catch (Exception ex) {
                        throw ToolExecutionException.callFailed(
                                "jinx_mcp_add",
                                ex.getMessage(),
                                "Check the parameters and try again"
                        );
                    }
                })
                .build();
    }

    private FunctionTool mcpRemove() {
        return FunctionTool.newBuilder()
                .name("jinx_mcp_remove")
                .description("""
                        删除一个 MCP 工具。
                        
                        【参数说明】
                        - name: 要删除的 MCP 工具名称
                        
                        【使用场景】
                        - 移除不再需要的 MCP 工具
                        """)
                .parameterType(McpNameSpec.class)
                .<McpNameSpec>function((caller, spec) -> {
                    try {
                        final var deleted = configTools.mcpRemove(spec.name());
                        return Map.of(
                                "success", deleted,
                                "message", deleted
                                        ? "MCP '%s' removed successfully".formatted(spec.name())
                                        : "MCP '%s' not found".formatted(spec.name())
                        );
                    } catch (Exception ex) {
                        throw ToolExecutionException.callFailed(
                                "jinx_mcp_remove",
                                ex.getMessage(),
                                "Check the MCP name and try again"
                        );
                    }
                })
                .build();
    }

    private FunctionTool mcpEnable() {
        return FunctionTool.newBuilder()
                .name("jinx_mcp_enable")
                .description("""
                        启用一个已禁用的 MCP 工具。
                        
                        【参数说明】
                        - name: 要启用的 MCP 工具名称
                        """)
                .parameterType(McpNameSpec.class)
                .<McpNameSpec>function((caller, spec) -> {
                    try {
                        configTools.mcpEnable(spec.name());
                        return Map.of(
                                "success", true,
                                "message", "MCP '%s' enabled successfully".formatted(spec.name())
                        );
                    } catch (Exception ex) {
                        throw ToolExecutionException.callFailed(
                                "jinx_mcp_enable",
                                ex.getMessage(),
                                "Check the MCP name and try again"
                        );
                    }
                })
                .build();
    }

    private FunctionTool mcpDisable() {
        return FunctionTool.newBuilder()
                .name("jinx_mcp_disable")
                .description("""
                        禁用一个 MCP 工具（不删除配置，仅停止使用）。
                        
                        【参数说明】
                        - name: 要禁用的 MCP 工具名称
                        """)
                .parameterType(McpNameSpec.class)
                .<McpNameSpec>function((caller, spec) -> {
                    try {
                        configTools.mcpDisable(spec.name());
                        return Map.of(
                                "success", true,
                                "message", "MCP '%s' disabled successfully".formatted(spec.name())
                        );
                    } catch (Exception ex) {
                        throw ToolExecutionException.callFailed(
                                "jinx_mcp_disable",
                                ex.getMessage(),
                                "Check the MCP name and try again"
                        );
                    }
                })
                .build();
    }

    // ==================== Skill 管理工具 ====================

    private FunctionTool skillList() {
        return FunctionTool.newBuilder()
                .name("jinx_skill_list")
                .description("""
                        列出当前所有已启用的 Skill 技能。
                        
                        【返回结果】
                        - skills: Skill 列表，每项包含 name、description、enabled 等信息
                        - total: Skill 总数
                        
                        【使用场景】
                        - 查看当前有哪些技能可用
                        - 确认某个 Skill 是否已启用
                        """)
                .supplier(() -> {
                    final var entries = configTools.skillList();
                    final var skills = entries.stream()
                            .map(e -> Map.of(
                                    "name", e.name(),
                                    "description", e.description() != null ? e.description() : "",
                                    "enabled", e.enabled()
                            ))
                            .toList();
                    return Map.of(
                            "skills", skills,
                            "total", skills.size()
                    );
                })
                .build();
    }

    private FunctionTool skillAdd() {
        return FunctionTool.newBuilder()
                .name("jinx_skill_add")
                .description("""
                        新增一个 Skill 技能。
                        
                        【参数说明】
                        - name: Skill 名称（唯一标识，如 code-review、translator）
                        - description: Skill 的简短描述
                        - content: Skill 的 Markdown 内容（完整的技能定义）
                        
                        【使用场景】
                        - 创建新的 Agent 技能
                        - 扩展 Agent 的能力
                        
                        【注意事项】
                        - name 不能与已有 Skill 重复
                        - content 是完整的 Markdown 格式技能定义
                        """)
                .parameterType(SkillAddSpec.class)
                .<SkillAddSpec>function((caller, spec) -> {
                    try {
                        configTools.skillAdd(spec.name(), spec.description(), spec.content(), true);
                        return Map.of(
                                "success", true,
                                "message", "Skill '%s' added successfully".formatted(spec.name())
                        );
                    } catch (Exception ex) {
                        throw ToolExecutionException.callFailed(
                                "jinx_skill_add",
                                ex.getMessage(),
                                "Check the parameters and try again"
                        );
                    }
                })
                .build();
    }

    private FunctionTool skillRemove() {
        return FunctionTool.newBuilder()
                .name("jinx_skill_remove")
                .description("""
                        删除一个 Skill 技能。
                        
                        【参数说明】
                        - name: 要删除的 Skill 名称
                        
                        【使用场景】
                        - 移除不再需要的技能
                        """)
                .parameterType(SkillNameSpec.class)
                .<SkillNameSpec>function((caller, spec) -> {
                    try {
                        final var deleted = configTools.skillRemove(spec.name());
                        return Map.of(
                                "success", deleted,
                                "message", deleted
                                        ? "Skill '%s' removed successfully".formatted(spec.name())
                                        : "Skill '%s' not found".formatted(spec.name())
                        );
                    } catch (Exception ex) {
                        throw ToolExecutionException.callFailed(
                                "jinx_skill_remove",
                                ex.getMessage(),
                                "Check the skill name and try again"
                        );
                    }
                })
                .build();
    }

    private FunctionTool skillEnable() {
        return FunctionTool.newBuilder()
                .name("jinx_skill_enable")
                .description("""
                        启用一个已禁用的 Skill 技能。
                        
                        【参数说明】
                        - name: 要启用的 Skill 名称
                        """)
                .parameterType(SkillNameSpec.class)
                .<SkillNameSpec>function((caller, spec) -> {
                    try {
                        configTools.skillEnable(spec.name());
                        return Map.of(
                                "success", true,
                                "message", "Skill '%s' enabled successfully".formatted(spec.name())
                        );
                    } catch (Exception ex) {
                        throw ToolExecutionException.callFailed(
                                "jinx_skill_enable",
                                ex.getMessage(),
                                "Check the skill name and try again"
                        );
                    }
                })
                .build();
    }

    private FunctionTool skillDisable() {
        return FunctionTool.newBuilder()
                .name("jinx_skill_disable")
                .description("""
                        禁用一个 Skill 技能（不删除配置，仅停止使用）。
                        
                        【参数说明】
                        - name: 要禁用的 Skill 名称
                        """)
                .parameterType(SkillNameSpec.class)
                .<SkillNameSpec>function((caller, spec) -> {
                    try {
                        configTools.skillDisable(spec.name());
                        return Map.of(
                                "success", true,
                                "message", "Skill '%s' disabled successfully".formatted(spec.name())
                        );
                    } catch (Exception ex) {
                        throw ToolExecutionException.callFailed(
                                "jinx_skill_disable",
                                ex.getMessage(),
                                "Check the skill name and try again"
                        );
                    }
                })
                .build();
    }

    // ==================== 功能开关工具 ====================

    private FunctionTool settingGet() {
        return FunctionTool.newBuilder()
                .name("jinx_setting_get")
                .description("""
                        查询 Jinx 的运行时设置项。
                        
                        【参数说明】
                        - key: 设置项名称，支持：
                          * speech.speaker.enabled - 语音播报开关
                          * speech.catcher.enabled - 语音输入开关
                          * all - 查询所有设置
                          
                        【返回结果】
                        - key: 设置项名称
                        - value: 当前值
                        """)
                .parameterType(SettingKeySpec.class)
                .<SettingKeySpec>function((caller, spec) -> {
                    return switch (spec.key()) {
                        case "speech.speaker.enabled" -> Map.of(
                                "key", spec.key(),
                                "value", configTools.isSpeakerEnabled()
                        );
                        case "speech.catcher.enabled" -> Map.of(
                                "key", spec.key(),
                                "value", configTools.isCatcherEnabled()
                        );
                        case "all" -> Map.of(
                                "settings", Map.of(
                                        "speech.speaker.enabled", configTools.isSpeakerEnabled(),
                                        "speech.catcher.enabled", configTools.isCatcherEnabled()
                                )
                        );
                        default -> throw ToolExecutionException.callFailed(
                                "jinx_setting_get",
                                "Unknown setting key: " + spec.key(),
                                "Valid keys: speech.speaker.enabled, speech.catcher.enabled, all"
                        );
                    };
                })
                .build();
    }

    private FunctionTool settingSet() {
        return FunctionTool.newBuilder()
                .name("jinx_setting_set")
                .description("""
                        修改 Jinx 的运行时设置项（立即生效并持久化）。
                        
                        【参数说明】
                        - key: 设置项名称，支持：
                          * speech.speaker.enabled - 语音播报开关
                          * speech.catcher.enabled - 语音输入开关
                        - value: 设置值（true/false）
                        
                        【使用场景】
                        - 开启或关闭语音播报
                        - 开启或关闭语音输入
                        """)
                .parameterType(SettingSetSpec.class)
                .<SettingSetSpec>function((caller, spec) -> {
                    return switch (spec.key()) {
                        case "speech.speaker.enabled" -> {
                            configTools.setSpeakerEnabled(spec.value());
                            yield Map.of(
                                    "success", true,
                                    "key", spec.key(),
                                    "value", spec.value(),
                                    "message", "语音播报已%s".formatted(spec.value() ? "开启" : "关闭")
                            );
                        }
                        case "speech.catcher.enabled" -> {
                            configTools.setCatcherEnabled(spec.value());
                            yield Map.of(
                                    "success", true,
                                    "key", spec.key(),
                                    "value", spec.value(),
                                    "message", "语音输入已%s".formatted(spec.value() ? "开启" : "关闭")
                            );
                        }
                        default -> throw ToolExecutionException.callFailed(
                                "jinx_setting_set",
                                "Unknown setting key: " + spec.key(),
                                "Valid keys: speech.speaker.enabled, speech.catcher.enabled"
                        );
                    };
                })
                .build();
    }

    // ==================== 参数类型定义 ====================

    record McpAddSpec(
            @JsonPropertyDescription("MCP 工具名称（唯一标识）")
            @JsonProperty(value = "name", required = true)
            String name,

            @JsonPropertyDescription("服务器类型: streamable-http, sse, stdio")
            @JsonProperty(value = "type", required = true)
            String type,

            @JsonPropertyDescription("服务基础 URL（HTTP 类型必填）")
            @JsonProperty("base_url")
            String baseUrl,

            @JsonPropertyDescription("服务端点路径（HTTP 类型必填）")
            @JsonProperty("endpoint")
            String endpoint
    ) {}

    record McpNameSpec(
            @JsonPropertyDescription("MCP 工具名称")
            @JsonProperty(value = "name", required = true)
            String name
    ) {}

    record SettingKeySpec(
            @JsonPropertyDescription("设置项名称: speech.speaker.enabled, speech.catcher.enabled, all")
            @JsonProperty(value = "key", required = true)
            String key
    ) {}

    record SettingSetSpec(
            @JsonPropertyDescription("设置项名称: speech.speaker.enabled, speech.catcher.enabled")
            @JsonProperty(value = "key", required = true)
            String key,

            @JsonPropertyDescription("设置值: true 或 false")
            @JsonProperty(value = "value", required = true)
            boolean value
    ) {}

    record SkillAddSpec(
            @JsonPropertyDescription("Skill 名称（唯一标识）")
            @JsonProperty(value = "name", required = true)
            String name,

            @JsonPropertyDescription("Skill 的简短描述")
            @JsonProperty(value = "description", required = true)
            String description,

            @JsonPropertyDescription("Skill 的 Markdown 内容（完整的技能定义）")
            @JsonProperty(value = "content", required = true)
            String content
    ) {}

    record SkillNameSpec(
            @JsonPropertyDescription("Skill 名称")
            @JsonProperty(value = "name", required = true)
            String name
    ) {}

}
