package io.github.oldmanpushcart.jinx.extra.mcp;

import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.core.detector.Detector;

import java.nio.file.Path;

/**
 * MCP探测器
 */
public interface McpDetector extends Detector<McpMeta> {

    Path MCP_DIR = Constants.DATA.resolve("mcp").normalize().toAbsolutePath();

}
