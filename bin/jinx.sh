#!/bin/bash

# ==========================================
# 1. Helper Functions
# ==========================================

# 优化：usage 只负责打印，不再直接退出
usage() {
    cat << EOF
Usage: $0 [OPTIONS] [COMMAND]

A command-line tool for interacting with the remote API.

OPTIONS:
  -i, --ip IP       Specify target server IP (default: 127.0.0.1)
  -p, --port PORT   Specify target server port (default: 8080)
  -x, --debug       Enable debug mode (equivalent to bash -x)

COMMANDS:
  session           Show the current SESSION-ID
  session new       Generate a new SESSION-ID and save locally
  version           Request remote API to get version info
  help              Show this help message
  (stdin input)     Send text to the remote chat interface

EXAMPLES:
  # Send local text to the chat interface
  echo "Hello" | $0

  # Get remote version info
  $0 --ip 192.168.1.50 --port 9000 version
EOF
}

# 生成新的 session
generate_session() {
    local new_session
    new_session="SESSION-$(shuf -i 0-9 -n 32 | tr -d '\n')"
    echo "$new_session" > "$SESSION_FILE"
    echo "New session generated: $new_session"
    exit 0
}

# 获取 session
get_session_id() {
    if [ ! -f "$SESSION_FILE" ]; then
        generate_session
    fi
    cat "$SESSION_FILE"
}

# ==========================================
# 2. Global Configuration & Argument Parsing
# ==========================================

IP="127.0.0.1"
PORT="8080"
SESSION_FILE="$HOME/.jinx.session"

# 解析全局选项 (-i, -p)
if ! TEMP=$(getopt -o i:p:xh --long ip:,port:,debug,help -- "$@"); then
    echo "Error: Failed to parse arguments."
    usage
    exit 1  # 解析失败，异常退出
fi

eval set -- "$TEMP"

while true; do
    case "$1" in
        -i | --ip)
            IP="$2"
            shift 2
            ;;
        -p | --port)
            PORT="$2"
            shift 2
            ;;
        -x | --debug)
            set -x
            shift
            ;;
        -h | --help)
            usage
            exit 0  # 用户主动查看帮助，正常退出
            ;;
        --)
            shift
            break
            ;;
        *)
            echo "Error: Unknown option '$1'"
            usage
            exit 1  # 遇到未知选项，异常退出
            ;;
    esac
done

# 解析子命令
COMMAND="$1"
case "$COMMAND" in
    session)
        # 如果没有第二个参数，默认显示当前 session
        if [ -z "$2" ]; then
            get_session_id
            exit 0
        elif [ "$2" == "new" ]; then
            generate_session
        else
            echo "Error: Unknown session subcommand."
            usage
            exit 1  # 子命令错误，异常退出
        fi
        ;;
    version)
        curl -s "http://${IP}:${PORT}/api/version"
        echo ""
        ;;
    help)
        usage
        exit 0  # 用户主动查看帮助，正常退出
        ;;
    "")
        # 默认为 chat 模式（读取标准输入）
        SESSION_ID=$(get_session_id)

        # 读取原始输入
        RAW_INPUT=$(cat)

        if [ -z "$RAW_INPUT" ]; then
            echo "Error: Standard input is empty."
            exit 1
        fi

        # 核心改动：无论当前环境是什么编码，直接无脑转成 UTF-8 后发送给 curl
        # 加上 //IGNORE 参数，遇到极少数无法识别的非法字符会自动丢弃，防止转换中断
        if ! printf "%s" "$RAW_INPUT" | iconv -f "$(locale charmap)" -t UTF-8//IGNORE | curl -fsS -X POST \
             -H 'Content-Type: text/plain; charset=utf-8' \
             --data-binary @- \
             "http://${IP}:${PORT}/api/chat/${SESSION_ID}"; then
            echo "Error: Failed to send message to remote API."
            exit 1
        fi

        # 换行，优化终端输出体验
        echo ""
        ;;
    *)
        echo "Error: Unknown command '$COMMAND'."
        usage
        exit 1  # 未知命令，异常退出
        ;;
esac