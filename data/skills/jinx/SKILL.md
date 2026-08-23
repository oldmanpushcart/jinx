---
name: jinx
description: Jinx CLI 管理技能。配置系统设置、管理 MCP 外部工具服务、管理 SKILL 行为编排指令、管理 AI 人格、发送对话消息、查询版本、管理会话。当需要修改 Jinx 配置、添加 MCP 服务、编辑 SKILL、重新加载人格、进行对话交互时使用。
license: MIT
---

# Jinx CLI 使用指南

通过命令行工具 `jinx.sh` 管理 Jinx 系统的所有功能。

## 目录结构

配置文件所在目录，修改后须通过对应 `reload` 命令生效：

```
jinx/
├── bin/            # 命令行工具（jinx.sh、jinxd.sh）
├── conf/           # 配置文件（application.yml、jinx.yml、logback.xml）
├── data/           # 运行时数据
│   ├── mcp/        # MCP 服务配置：{name}.mcp.json
│   ├── skills/     # SKILL 技能定义：{name}/SKILL.md
│   ├── session/    # 会话记录
│   └── PERSONA.md  # AI 角色设定
├── logs/           # 日志（jinx.log、dashscope4j.log，按天滚动）
├── work/           # Agent 工作空间
└── libs/           # （发布包）FatJar
```

## 功能索引

各功能的详细文档位于 `references/` 目录下，按需查阅：

| 功能 | 详细文档 | 说明 |
|---|---|---|
| MCP 管理 | [references/MCP.md](references/MCP.md) | 管理 MCP 外部工具服务（stdio/sse/streamable-http） |
| SKILL 管理 | [references/SKILL-MANAGE.md](references/SKILL-MANAGE.md) | 管理 SKILL 行为编排指令 |
| 人格管理 | [references/PERSONA.md](references/PERSONA.md) | 管理 AI 角色设定 |
| 系统设置 | [references/SETTING.md](references/SETTING.md) | 查看和修改系统配置 |
| 对话 | [references/CHAT.md](references/CHAT.md) | 与 Agent 对话交互（含会话管理） |
| 版本查询 | [references/VERSION.md](references/VERSION.md) | 查看系统版本 |

## 命令行工具（jinx.sh）

固定路径：`sh ./bin/jinx.sh`

### 全局选项

| 选项 | 说明 |
|---|---|
| `-i IP` | 指定目标服务器 IP（默认 127.0.0.1） |
| `-p PORT` | 指定目标服务器端口（默认 8080） |
| `-s SESSION_ID` | 指定会话 ID（省略则自动从 `~/.jinx.session` 读取） |
| `-x` | 调试模式 |
| `-h` | 显示帮助信息 |

### 本地命令

本地命令由 `jinx.sh` 直接处理，不依赖远程服务：

```bash
sh ./bin/jinx.sh session                    # 查看当前 Session ID
sh ./bin/jinx.sh session new                # 生成新 Session ID
```

### 远程命令

远程命令通过服务端执行。可通过 `jinx.sh help` 动态获取所有远程命令清单：

```bash
sh ./bin/jinx.sh help
```