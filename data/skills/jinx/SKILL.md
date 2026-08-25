---
name: jinx
description: |
  Jinx CLI 管理技能。
  当需要修改 Jinx 配置、添加 MCP 服务、编辑 SKILL、重新加载人格、进行对话交互、管理定时任务时使用。
  
  通过 jinx.sh 命令行工具管理 Jinx 系统，涵盖以下能力：
  - 系统设置：查看和修改系统配置
    - 开启 / 关闭 音频输入
    - 开启 / 关闭 语音播报
  - MCP 管理：管理MCP
  - SKILL 管理：管理SKILL
  - 人格管理：管理 AI 角色设定。
  - 对话交互：与 Agent 进行对话（含会话管理）
  - 定时任务：创建和管理定时调度任务。当定时、周期性的目标时可以激活。
    - 3分钟后叫醒我
    - 每周五整理好报告并发到我邮箱
  - 版本查询：查看系统版本
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
│   ├── cron/       # 定时任务配置：{name}.cron.json
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
| 定时任务 | [references/CRON.md](references/CRON.md) | 管理定时调度任务（定时/定期执行） |
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