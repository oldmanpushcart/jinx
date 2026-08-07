package io.github.oldmanpushcart.jinx.core2.mcp;

import io.github.oldmanpushcart.jinx.core2.CoreException;

public class McpException extends CoreException {

    private final String name;

    public McpException(String name, String code, String message, Throwable cause) {
        super(code, message, cause);
        this.name = name;
    }

    @Override
    public String getLocalizedMessage() {
        return "MCP error! code=%s;name=%s;message=%s".formatted(code(), name, getMessage());
    }

    public static McpException of(String name, String code, String message, Throwable cause) {
        return new McpException(name, code, message, cause);
    }

    public static McpException of(McpEntity mcp, String code, String message, Throwable cause) {
        return new McpException(mcp.name(), code, message, cause);
    }

    public static McpException of(McpEntity mcp, String code, String message) {
        return new McpException(mcp.name(), code, message, null);
    }

}
