#!/bin/bash

# ==========================================
# 1. Helper Functions
# ==========================================

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
  (text)            Send text directly as a chat message
  (stdin input)     Send piped text to the remote chat interface

EXAMPLES:
  # Send local text to the chat interface
  $0 "What day is it today?"

  # Send piped text
  echo "Hello" | $0

  # Get remote version info
  $0 --ip 192.168.1.50 --port 9000 version
EOF
}

generate_session() {
    local new_session
    new_session="S$(shuf -i 0-9 -n 32 | tr -d '\n')"
    echo "$new_session" > "$SESSION_FILE"
    echo "New session generated: $new_session"
    exit 0
}

get_session_id() {
    if [ ! -f "$SESSION_FILE" ]; then
        generate_session
    fi
    cat "$SESSION_FILE"
}

# ==========================================
# 2. Core Logic Abstraction (核心抽象)
# ==========================================

# 将发送消息的核心逻辑提取出来，避免重复代码
# 参数 $1: 要发送的消息内容
send_chat_message() {
    local message="$1"
    local SESSION_ID
    SESSION_ID=$(get_session_id)

    if [ -z "$message" ]; then
        echo "Error: Message is empty."
        exit 1
    fi

    # 核心改动：无论当前环境是什么编码，直接无脑转成 UTF-8 后发送给 curl
    # 加上 //IGNORE 参数，遇到极少数无法识别的非法字符会自动丢弃，防止转换中断
    if ! printf "%s" "$message" | iconv -f "$(locale charmap)" -t UTF-8//IGNORE | curl -fsS -X POST \
         -H 'Content-Type: text/plain; charset=utf-8' \
         --data-binary @- \
         "http://${IP}:${PORT}/api/chat/${SESSION_ID}"; then
        echo "Error: Failed to send message to remote API."
        exit 1
    fi

    # 换行，优化终端输出体验
    echo ""
}

# ==========================================
# 3. Global Configuration & Argument Parsing
# ==========================================

IP="127.0.0.1"
PORT="8080"
SESSION_FILE="$HOME/.jinx.session"

# 解析全局选项 (-i, -p)
if ! TEMP=$(getopt -o i:p:xh --long ip:,port:,debug,help -- "$@"); then
    echo "Error: Failed to parse arguments."
    usage
    exit 1
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
            exit 0
            ;;
        --)
            shift
            break
            ;;
        *)
            echo "Error: Unknown option '$1'"
            usage
            exit 1
            ;;
    esac
done

# 解析子命令
COMMAND="$1"
case "$COMMAND" in
    session)
        if [ -z "$2" ]; then
            get_session_id
            exit 0
        elif [ "$2" == "new" ]; then
            generate_session
        else
            echo "Error: Unknown session subcommand."
            usage
            exit 1
        fi
        ;;
    version)
        curl -s "http://${IP}:${PORT}/api/version"
        echo ""
        ;;
    help)
        usage
        exit 0
        ;;
    "")
        # 默认为 chat 模式（读取标准输入）
        RAW_INPUT=$(cat)
        send_chat_message "$RAW_INPUT"
        ;;
    *)
        # 未知命令模式：将其视为直接的聊天文本发送
        # 这样 ./jinx.sh "今天星期几?" 就能直接生效
        send_chat_message "$COMMAND"
        ;;
esac