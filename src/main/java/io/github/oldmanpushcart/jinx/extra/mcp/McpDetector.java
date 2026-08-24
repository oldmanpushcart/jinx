package io.github.oldmanpushcart.jinx.extra.mcp;

import io.github.oldmanpushcart.jinx.Constants;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * MCP探测器
 */
public interface McpDetector {

    Path MCP_DIR = Constants.DATA.resolve("mcp").normalize().toAbsolutePath();

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
     * 重加载指定的MCP配置
     *
     * @param name 名称
     * @return 重新加载后的MCP元数据
     */
    CompletionStage<McpMeta> reload(String name);

}
