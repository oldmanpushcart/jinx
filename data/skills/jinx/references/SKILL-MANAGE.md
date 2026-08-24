# SKILL 管理

SKILL 用于定义 Agent 的行为编排指令，遵循 Agent Skills 开放规范。

## 命令

```bash
sh ./bin/jinx.sh skill list                 # 列出所有已加载的 SKILL
sh ./bin/jinx.sh skill detail <NAME>         # 查看指定 SKILL 的详细内容
sh ./bin/jinx.sh skill reload <NAME>         # 重新加载指定 SKILL
```

## 配置文件

路径：`{jinx.data}/skills/{name}/SKILL.md`

目录名须与 frontmatter 中 `name` 字段一致，修改后须执行 `reload`。

## 操作说明

SKILL 的增删改均通过操作配置文件完成，系统每 10 秒自动扫描变更，也可执行 `skill reload` 立即生效。

- **添加**：在 `{jinx.data}/skills/` 下创建 `{name}/` 目录并写入 `SKILL.md`，目录名须与 frontmatter 中 `name` 一致，然后执行 `skill reload <NAME>` 加载并验证。
- **修改**：直接编辑 `SKILL.md`（或目录内的其他资源文件），然后执行 `skill reload <NAME>`；改名等同删除旧 SKILL + 创建新 SKILL。
- **删除**：删除 `{jinx.data}/skills/{name}/` 整个目录，系统自动扫描移除；如需立即确认，执行 `skill list` 验证。

## SKILL.md 格式

```markdown
---
name: skill-name
description: 技能描述（用于 Agent 匹配，应包含功能关键词和触发场景）
license: MIT
---

# 技能标题

技能内容（Markdown 正文，Agent 激活后加载执行）...
```

## 目录结构约定

```
skill-name/
├── SKILL.md          # 必须：元数据 + 主指令
├── references/       # 可选：参考文档（按需加载）
├── scripts/          # 可选：可执行脚本
├── assets/           # 可选：模板、资源
└── ...
```
