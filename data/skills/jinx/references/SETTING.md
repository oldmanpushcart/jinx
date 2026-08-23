# 系统设置

通过命令行查看和修改 Jinx 运行时配置。

## 命令

```bash
sh ./bin/jinx.sh setting                    # 列出所有配置项
sh ./bin/jinx.sh setting <NAME>             # 查询指定配置项
sh ./bin/jinx.sh setting <NAME> <VALUE>     # 修改配置项
```

## 可写配置项

| 配置项 | 说明 | 值类型 |
|---|---|---|
| `jinx.speaker.enabled` | 语音播报开关 | `true` / `false` |
| `jinx.catcher.enabled` | 语音捕获开关 | `true` / `false` |

其余配置项为只读。
