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
