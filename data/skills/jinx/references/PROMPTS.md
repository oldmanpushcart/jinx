# 提示词管理

提示词（Prompts）是常驻植入会话上下文的静态系统消息（不支持变量替换），按阶段分目录管理：

- **preparation**：在 PreparationHook 阶段植入（人格、用户档案等）。
- **interaction**：在 InteractionHook 阶段植入。

## 与 MEMORY 的边界

用户档案类提示词只记录"你是谁"——称呼、职业、年龄、家庭、偏好、习惯等长期稳定的个人信息；
对话内容、发生过的事件、一次性请求属于 MEMORY / 会话历史，不得写入。

当用户主动告知个人信息或要求"记住我…"时，先判断是否符合记录范围，不符合则不要写入。

## 命令

```bash
./bin/jinx.sh prompts                             # 列出全部提示词
./bin/jinx.sh prompts <PHASE>                     # 列出指定阶段提示词（preparation|interaction）
./bin/jinx.sh prompts <PHASE> reload <NAME>       # 重新加载指定提示词
./bin/jinx.sh prompts <PHASE> detail <NAME>       # 查看指定提示词内容
```

## 配置文件

目录：`{jinx.data}/prompts/{phase}/{name}.md`（文件名去掉 `.md` 即提示词名称）。

内置提示词：

| 提示词 | 路径 | 说明 |
|---|---|---|
| 人格 | `prompts/preparation/persona.md` | AI 角色设定 |
| 用户档案 | `prompts/preparation/user.md` | 用户稳定画像信息 |

写入方式：直接对文件进行文件操作（编辑后归并去重，保持内容精炼）。

**文件编辑硬性要求：**
1. 文件必须采用 **UTF-8** 编码；
2. 编辑完成后**必须执行 `prompts <PHASE> reload <NAME>` 强制生效**（系统虽有 10 秒自动探测，但不得依赖其代替 reload）。
