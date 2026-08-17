package io.github.oldmanpushcart.jinx.core.mcp;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * MCP探测器
 */
public interface McpDetector {

    /**
     * @return 已探测到的MCP元数据
     */
    List<McpMeta> list();

    /**
     * 获取指定名称的MCP元数据
     *
     * @param name 名称
     * @return MCP元数据
     */
    Optional<McpMeta> get(String name);

    /**
     * 添加MCP
     *
     * @param mcp MCP元数据
     * @return 添加回调
     */
    CompletionStage<McpMeta> append(McpMeta mcp);

    /**
     * 移除MCP
     *
     * @param name MCP名称
     * @return 移除的MCP元数据
     */
    McpMeta remove(String name);

}
