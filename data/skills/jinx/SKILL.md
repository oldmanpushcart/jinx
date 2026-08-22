---
name: jinx
description: Jinx AI Agent 系统管理技能。用于查看和修改系统配置（语音、日志等），管理 MCP 外部工具服务（stdio/sse/streamable-http）和 SKILL 行为编排指令，查看系统版本和会话信息，以及通过命令行与 Jinx 进行对话交互。当需要配置、调试、扩展 Jinx 系统能力时使用此技能。
license: MIT
---

# Jinx 系统管理

## 项目概览

Jinx 是一个基于 Java（Micronaut + DashScope4J）构建的本地 AI Agent 服务。通过 HTTP API 对外暴露对话、MCP/SKILL 管理、系统设置等接口，支持语音播报与拾音，并可通过 CLI 脚本（`jinx.sh`）进行交互和管理。

命令架构采用 **Cli 接口 + CliController 自动发现** 模式：每个命令是一个独立的 `@Singleton` Bean，实现 `Cli` 接口，由 `CliController` 自动收集和路由。新增命令只需新建类，无需修改现有代码。

## 目录结构

```
jinx/
├── bin/            # 命令行工具
├── conf/           # 配置文件
├── data/           # 运行时数据
├── logs/           # 日志输出
├── src/            # Java 源代码
├── work/           # 工作空间
├── libs/           # （发布包）FatJar
├── target/         # Maven 构建输出
├── release.sh      # 发布打包脚本
└── pom.xml         # Maven 项目配置
```

| 目录 | 用途 |
|---|---|
| `bin/` | 命令行工具。`jinx.sh` 是 CLI 客户端（发送对话、管理 MCP/SKILL、查看设置）；`jinxd.sh` 是守护进程管理（start/stop/restart/status） |
| `conf/` | 配置文件。`application.yml`（Micronaut 框架配置：端口、超时）；`jinx.yml`（应用配置：数据空间、DashScope、语音）；`logback.xml`（日志策略） |
| `data/` | 运行时数据。`mcp/`（MCP 服务配置 `{name}.mcp.json`）；`skills/`（SKILL 技能定义 `{name}/SKILL.md`）；`session/`（会话记录）；`PERSONA.md`（AI 角色设定档案） |
| `logs/` | 日志输出。`jinx.log`（主日志）；`dashscope4j.log`（DashScope SDK 日志）；按天滚动，保留 30 天 |
| `src/` | Java 源代码。`cli/`（Cli 接口与 CliController）；`controller/`（HTTP 接口层）；`core/`（基础设施：dashscope、speech、toolbox）；`extra/`（扩展命令模块，每个模块含领域代码 + `cli/` 子包的 Cli Bean） |
| `work/` | 工作空间，供 Agent 运行时使用的临时工作目录 |
| `libs/` | （仅发布包）存放 FatJar `jinx-{version}-all.jar`，由 `release.sh` 构建生成 |
| `target/` | Maven 构建输出目录 |

## 命令约定

- 命令行工具固定路径：`sh ./bin/jinx.sh`
- 命令分为**本地命令**（session、chat、help）和**远程命令**（其余所有，通过 `/api/cli/execute` 通用代理）
- 远程命令由 CliController 自动发现所有 Cli Bean 并路由，新增命令无需修改 `jinx.sh`
- `help` 命令会动态拉取服务端所有远程命令清单

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

## 人格管理

```bash
sh ./bin/jinx.sh persona                    # 查看当前人格内容
sh ./bin/jinx.sh persona reload             # 从文件重新加载人格
```

配置文件：`{jinx.data}/PERSONA.md`，修改后须 reload。

## 系统设置

```bash
sh ./bin/jinx.sh setting                       # 列出全部
sh ./bin/jinx.sh setting NAME                  # 查询
sh ./bin/jinx.sh setting NAME VALUE            # 修改
```

可写项：`jinx.speaker.enabled`（语音播报）、`jinx.catcher.enabled`（语音捕获），其余只读。

## 版本查询

```bash
sh ./bin/jinx.sh version
```

## 对话

```bash
sh ./bin/jinx.sh chat "你好"                  # 发送文本
echo "Hello" | sh ./bin/jinx.sh chat          # 管道输入
cat app.log | sh ./bin/jinx.sh chat "分析日志"  # 管道 + 文本拼接
```
