# 提示词管理

提示词（Prompts）是常驻植入会话上下文的静态系统消息（不支持变量替换），在 每次智能体对话 阶段植入。

## 命令

```bash
./bin/jinx.sh prompts                             # 列出全部提示词
./bin/jinx.sh prompts reload <NAME>               # 重新加载指定提示词
./bin/jinx.sh prompts detail <NAME>               # 查看指定提示词内容
```

## 配置文件

目录：`{jinx.data}/prompts/{name}.md`（文件名去掉 `.md` 即提示词名称）。

写入方式：直接对文件进行文件操作（编辑后归并去重，保持内容精炼）。

**文件编辑硬性要求：**

1. 文件必须采用 **UTF-8** 编码；
2. 编辑完成后 **必须执行 `prompts reload <NAME>` 强制生效**（系统虽有 10 秒自动探测，但不得依赖其代替 reload）。
