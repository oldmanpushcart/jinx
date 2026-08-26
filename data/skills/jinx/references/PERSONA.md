# 人格管理

人格（Persona）定义 Agent 的角色设定和行为准则。

## 命令

```bash
sh ./bin/jinx.sh persona                    # 查看当前人格内容
sh ./bin/jinx.sh persona reload             # 从文件重新加载人格
```

## 配置文件

路径：`{jinx.data}/PERSONA.md`

**文件编辑硬性要求：**
1. 文件必须采用 **UTF-8** 编码；
2. 编辑完成后**必须执行 `persona reload` 强制生效**（系统虽有 10 秒自动探测，但不得依赖其代替 reload）。
