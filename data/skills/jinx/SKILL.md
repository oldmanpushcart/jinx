---
name: jinx
description: Jinx 系统管理技能
license: MIT
---

# Jinx 系统管理

Jinx 通过两种机制扩展 AI 能力：

- **MCP**（外部工具服务）— 连接外部进程或 API，提供原子工具（如地图搜索、天气查询）
- **SKILL**（行为编排指令）— 用 Markdown 教 AI 如何组合工具和步骤完成复杂任务

两者互补：MCP 提供能力，SKILL 编排能力。

## 命令约定

- 命令行工具固定路径：`./bin/jinx.sh`
- 目录结构通过 `./bin/jinx.sh setting` 查看（`jinx.data`、`jinx.conf` 等）

## MCP 管理

```bash
./bin/jinx.sh mcp list
./bin/jinx.sh mcp detail NAME
./bin/jinx.sh mcp reload NAME
```

配置文件：`{jinx.data}/mcp/{name}.mcp.json`，文件名须与 JSON `name` 一致，修改后须 reload。

支持三种类型：

```json
// stdio — 本地进程
{
  "name": "服务名称",
  "type": "stdio",
  "cmd": "启动命令",
  "args": ["参数1", "参数2"],
  "env": {"ENV_KEY": "ENV_VALUE"}
}

// sse — Server-Sent Events
{
  "name": "服务名称",
  "type": "sse",
  "host": "https://example.com",
  "endpoint": "/sse",
  "headers": {"Authorization": "Bearer ${TOKEN}"}
}

// streamable-http — 流式 HTTP
{
  "name": "服务名称",
  "type": "streamable-http",
  "host": "https://example.com",
  "endpoint": "/mcp",
  "headers": {"Authorization": "Bearer ${TOKEN}"}
}
```

## SKILL 管理

```bash
./bin/jinx.sh skill list
./bin/jinx.sh skill detail NAME
./bin/jinx.sh skill reload NAME
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
./bin/jinx.sh setting                       # 列出全部
./bin/jinx.sh setting NAME                  # 查询
./bin/jinx.sh setting NAME VALUE            # 修改
```

可写项：`jinx.speaker.enable`（语音播报）、`jinx.catcher.enable`（语音捕获），其余只读。
