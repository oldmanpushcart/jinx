package io.github.oldmanpushcart.jinx.core.mcp;

/**
 * MCP 配置文件条目
 *
 * @param name    MCP 工具名称（与文件名对应，如 amap.mcp.json → amap）
 * @param enabled 是否启用
 * @param server  MCP 服务器配置
 */
public record McpFileEntry(
        String name,
        boolean enabled,
        McpConfig.Server server
) {
}
