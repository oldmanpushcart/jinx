# 对话

通过命令行与 Jinx Agent 进行对话交互，支持文本输入和管道输入。

## 命令

```bash
./bin/jinx.sh chat "你好"                # 发送文本消息
echo "Hello" | ./bin/jinx.sh chat        # 管道输入
cat app.log | ./bin/jinx.sh chat "分析日志"  # 管道 + 文本拼接
```

## 会话管理

对话通过 Session ID 维护上下文，默认自动从本地文件（`~/.jinx.session`）读取。

```bash
./bin/jinx.sh -s <SESSION_ID> chat "..." # 指定 Session ID
./bin/jinx.sh session                    # 查看当前 Session ID
./bin/jinx.sh session new                # 生成新 Session ID
```
