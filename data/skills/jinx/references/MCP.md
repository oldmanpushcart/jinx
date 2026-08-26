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

文件名须与 JSON 中 `name` 字段一致。

**文件编辑硬性要求：**
1. 文件必须采用 **UTF-8** 编码；
2. 编辑完成后**必须执行 `mcp reload <NAME>` 强制生效**（系统虽有 10 秒自动扫描，但不得依赖其代替 reload）。

## 操作说明

MCP 的增删改均通过操作配置文件完成，每次编辑后必须执行 `mcp reload <NAME>` 强制生效。

- **添加**：在 `{jinx.data}/mcp/` 下写入 `{name}.mcp.json`，格式严格按下方传输类型模板，文件名须与 JSON 中 `name` 一致，然后执行 `mcp reload <NAME>` 加载并验证。
- **修改**：直接编辑配置文件（禁止修改 `name` 字段），然后执行 `mcp reload <NAME>`；改名等同删除旧 MCP + 创建新 MCP。
- **删除**：删除 `{jinx.data}/mcp/{name}.mcp.json` 文件，系统自动扫描移除；如需立即确认，执行 `mcp list` 验证。
- **环境变量**：配置值中可用 `${ENV_VAR}` 语法引用系统环境变量，加载时自动替换。

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
