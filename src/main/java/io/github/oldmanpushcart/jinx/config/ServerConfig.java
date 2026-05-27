package io.github.oldmanpushcart.jinx.config;

public record ServerConfig(
    int port,
    String contextPath,
    String host,
    int maxThreads
) {
    public ServerConfig {
        if (port <= 0) port = 8080;
        if (contextPath == null || contextPath.isEmpty()) contextPath = "/api/v1/agent";
        if (host == null || host.isEmpty()) host = "0.0.0.0";
        if (maxThreads <= 0) maxThreads = 200;
    }
}
