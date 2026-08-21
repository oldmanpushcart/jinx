---
name: jinx
description: Jinx AI Agent 系统管理技能。用于查看和修改系统配置（语音、日志等），管理 MCP 外部工具服务（stdio/sse/streamable-http）和 SKILL 行为编排指令，查看系统版本和会话信息，以及通过命令行与 Jinx 进行对话交互。当需要配置、调试、扩展 Jinx 系统能力时使用此技能。
license: MIT
---

# Jinx 系统管理

## 项目概览

Jinx 是一个基于 Java（Micronaut + DashScope4J）构建的本地 AI Agent 服务。通过 HTTP API 对外暴露对话、MCP/SKILL 管理、系统设置等接口，支持语音播报与拾音，并可通过 CLI 脚本（`jinx.sh`）进行交互和管理。

## 目录结构

```
jinx/
├── bin/            # 命令行工具
├── conf/           # 配置文件
├── data/           # 运行时数据
├── logs/           # 日志输出
├── work/           # 工作空间
```

| 目录 | 用途 |
|---|---|
| `bin/` | 命令行工具。`jinx.sh` 是 CLI 客户端（发送对话、管理 MCP/SKILL、查看设置）；`jinxd.sh` 是守护进程管理（start/stop/restart/status） |
| `conf/` | 配置文件。`application.yml`（Micronaut 框架配置：端口、超时）；`jinx.yml`（应用配置：数据空间、DashScope、语音）；`logback.xml`（日志策略） |
| `data/` | 运行时数据。`mcp/`（MCP 服务配置 `{name}.mcp.json`）；`skills/`（SKILL 技能定义 `{name}/SKILL.md`）；`session/`（会话记录）；`PERSONA.md`（AI 角色设定档案） |
| `logs/` | 日志输出。`jinx.log`（主日志）；`dashscope4j.log`（DashScope SDK 日志）；按天滚动，保留 30 天 |
| `work/` | 工作空间，供 Agent 运行时使用的临时工作目录 |

## 命令约定

- 命令行工具固定路径：`sh ./bin/jinx.sh`
- 目录结构通过 `sh ./bin/jinx.sh setting` 查看（`jinx.data`、`jinx.conf` 等）

## MCP 管理

```bash
sh ./bin/jinx.sh mcp list
sh ./bin/jinx.sh mcp detail NAME
sh ./bin/jinx.sh mcp reload NAME
```

配置文件：`{jinx.data}/mcp/{name}.mcp.json`，文件名须与 JSON `name` 一致，修改后须 reload。

支持三种类型：

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

## SKILL 管理

```bash
sh ./bin/jinx.sh skill list
sh ./bin/jinx.sh skill detail NAME
sh ./bin/jinx.sh skill reload NAME
```

配置文件：`{jinx.data}/skills/{name}/SKILL.md`，目录名须与 frontmatter `name` 一致，修改后须 reload。

```markdown
---
name: skill-name
description: 技能描述
license: MIT
---

# 技能标题

技能内容...
```

## 系统设置

```bash
sh ./bin/jinx.sh setting                       # 列出全部
sh ./bin/jinx.sh setting NAME                  # 查询
sh ./bin/jinx.sh setting NAME VALUE            # 修改
```

可写项：`jinx.speaker.enable`（语音播报）、`jinx.catcher.enable`（语音捕获），其余只读。
