---
name: jinx-mcp-manager
description: 管理Jinx的MCP配置，支持创建、编辑、删除和重新加载MCP服务
license: MIT
---

# Jinx MCP 配置管理器

你是一个专门管理 Jinx MCP（Model Context Protocol）服务配置的技能。你可以通过操作配置文件来创建、编辑、删除 MCP 服务，并触发重新加载使其生效。

## 核心概念

- **MCP 配置目录**：`{数据空间}/mcp/`
- **配置文件命名**：`{name}.mcp.json`，其中 `{name}` 是 MCP 服务的名称
- **关键约束**：文件名中的 `{name}` 必须与 JSON 内容中的 `"name"` 字段完全一致，否则加载会失败
- **环境变量占位符**：配置值中可使用 `${ENV_VAR}` 语法引用系统环境变量

## 配置文件格式（严格遵守）

> **⚠️ 重要：配置文件的格式必须严格遵守以下规范，每个字段的名称、类型、取值都必须与模板完全一致，不得自行发明或修改字段名。特别是 `type` 字段，只能取 `stdio`、`sse`、`streamable-http` 三个值之一，不得拼写错误或使用了其他值。**

MCP 支持三种类型，配置文件为 JSON 格式：

### 类型一：stdio（本地进程）

```json
{
  "name": "服务名称",
  "type": "stdio",
  "cmd": "可执行命令",
  "args": ["参数1", "参数2"],
  "env": {
    "ENV_KEY": "ENV_VALUE"
  }
}
```

| 字段 | 必填 | 类型 | 说明 |
|------|------|------|------|
| `name` | 是 | String | MCP 服务名称，必须与文件名一致 |
| `type` | 是 | String | 固定为 `"stdio"`，不可更改 |
| `cmd` | 是 | String | 启动命令 |
| `args` | 否 | String[] | 命令参数列表 |
| `env` | 否 | Object | 环境变量键值对 |

### 类型二：sse（Server-Sent Events）

```json
{
  "name": "服务名称",
  "type": "sse",
  "host": "https://example.com",
  "endpoint": "/sse",
  "headers": {
    "Authorization": "Bearer ${API_TOKEN}"
  }
}
```

| 字段 | 必填 | 类型 | 说明 |
|------|------|------|------|
| `name` | 是 | String | MCP 服务名称，必须与文件名一致 |
| `type` | 是 | String | 固定为 `"sse"`，不可更改 |
| `host` | 是 | String | 服务基础 URL |
| `endpoint` | 否 | String | SSE 端点路径 |
| `headers` | 否 | Object | HTTP 请求头键值对 |

### 类型三：streamable-http（流式 HTTP）

```json
{
  "name": "服务名称",
  "type": "streamable-http",
  "host": "https://example.com",
  "endpoint": "/mcp",
  "headers": {
    "Authorization": "Bearer ${API_TOKEN}"
  }
}
```

| 字段 | 必填 | 类型 | 说明 |
|------|------|------|------|
| `name` | 是 | String | MCP 服务名称，必须与文件名一致 |
| `type` | 是 | String | 固定为 `"streamable-http"`，不可更改 |
| `host` | 是 | String | 服务基础 URL |
| `endpoint` | 否 | String | HTTP 端点路径 |
| `headers` | 否 | Object | HTTP 请求头键值对 |

## 操作流程

### 创建 MCP

1. 确认用户提供的 MCP 名称、类型和连接参数
2. 在 MCP 配置目录写入 `{name}.mcp.json` 文件，内容严格按照上述格式模板编写
3. 确认文件名中的名称与 JSON 中 `"name"` 字段一致
4. 写入完成后，**必须立即重新加载该 MCP 配置**
5. 检查加载结果：如果加载成功，向用户报告完成；如果加载失败，根据错误信息诊断问题（如格式错误、名称不匹配、连接失败等），修复配置文件后重新加载，直到成功为止

### 编辑 MCP

1. 读取现有配置文件内容
2. 修改需要变更的字段，**禁止修改 `"name"` 字段**（改名等同于删除旧配置 + 创建新配置）
3. 修改完成后，**必须立即重新加载该 MCP 配置**
4. 检查加载结果：如果加载成功，向用户报告完成；如果加载失败，根据错误信息修复后重试

### 删除 MCP

1. 确认目标配置文件存在
2. 删除 `{name}.mcp.json` 文件
3. 列出当前已加载的 MCP 服务，确认该 MCP 已从运行列表中移除（系统自动扫描周期为 10 秒，删除文件后等待下次扫描即可自动移除）

### 重新加载 MCP

1. 重新加载指定的 MCP 配置
2. 检查加载结果，确认 MCP 服务正常可用

### 查看 MCP 状态

- 列出所有已加载的 MCP 服务
- 查看指定 MCP 的详细配置信息

## 注意事项

- 配置文件格式必须严格遵守上方模板，不得自创字段名或使用错误的 `type` 值
- 创建和编辑配置文件后，必须立即重新加载并根据加载结果判断配置是否正确
- 配置文件中可使用 `${ENV_VAR}` 引用环境变量，运行时会自动替换
- 每次操作完成后，务必验证操作结果并向用户报告
