# MCP 管理

MCP（Model Context Protocol）用于为 Agent 接入外部工具服务。

## 命令

```bash
sh ./bin/jinx.sh mcp list                  # 列出所有已加载的 MCP 服务
sh ./bin/jinx.sh mcp detail <NAME>          # 查看指定 MCP 服务的详细信息
sh ./bin/jinx.sh mcp reload <NAME>          # 重新加载指定 MCP 服务
```

## 配置文件

路径：`{jinx.data}/mcp/{name}.mcp.json`

文件名须与 JSON 中 `name` 字段一致，修改配置后须执行 `reload`。

## 支持的传输类型

### stdio — 本地进程

```json
{
  "name": "服务名称",
  "type": "stdio",
  "cmd": "启动命令",
  "args": [
    "参数1",
    "参数2"
  ],
  "env": {
    "ENV_KEY": "ENV_VALUE"
  }
}
```

### sse — Server-Sent Events

```json
{
  "name": "服务名称",
  "type": "sse",
  "host": "https://example.com",
  "endpoint": "/sse",
  "headers": {
    "Authorization": "Bearer ${TOKEN}"
  }
}
```

### streamable-http — 流式 HTTP

```json
{
  "name": "服务名称",
  "type": "streamable-http",
  "host": "https://example.com",
  "endpoint": "/mcp",
  "headers": {
    "Authorization": "Bearer ${TOKEN}"
  }
}
```
