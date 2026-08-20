#!/bin/bash

# ==========================================
# 1. Helper Functions
# ==========================================

usage() {
    cat << EOF
Usage: $0 [OPTIONS] [COMMAND]

A command-line tool for interacting with the remote API.

OPTIONS:
  -i IP       Specify target server IP (default: 127.0.0.1)
  -p PORT     Specify target server port (default: 8080)
  -x          Enable debug mode (equivalent to bash -x)
  -h          Show this help message

COMMANDS:
  session           Show the current SESSION-ID
  session new       Generate a new SESSION-ID and save locally
  version           Request remote API to get version
  info              Request remote API to get info
  help              Show this help message
  (text)            Send text directly as a chat message
  (stdin input)     Send piped text to the remote chat interface

EXAMPLES:
  # Send local text to the chat interface
  $0 "What day is it today?"

  # Send piped text
  echo "Hello" | $0

  # Merge piped text with command text (管道内容 + 命令内容)
  cat app.log | $0 "分析以上日志"

  # Get remote version info
  $0 -i 192.168.1.50 -p 9000 version
EOF
}

generate_session() {
    local new_session="S"
    local chars="0123456789"
    for (( i=0; i<32; i++ )); do
        new_session+="${chars:RANDOM % ${#chars}:1}"
    done

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
# 2. Core Logic Abstraction
# ==========================================

send_chat_message() {
    local message="$1"
    local SESSION_ID
    SESSION_ID=$(get_session_id)

    if [ -z "$message" ]; then
        echo "Error: Message is empty."
        exit 1
    fi

    # 无论当前环境是什么编码，直接无脑转成 UTF-8 后发送给 curl
    if ! printf "%s" "$message" | iconv -f "$(locale charmap)" -t UTF-8//IGNORE | curl "$CURL_FLAGS" -X POST \
         -H 'Content-Type: text/plain; charset=utf-8' \
         --data-binary @- \
         "http://${IP}:${PORT}/api/chat/${SESSION_ID}"; then
        echo "Error: Failed to send message to remote API."
        exit 1
    fi

    echo ""
}

# ==========================================
# 3. Global Configuration & Argument Parsing
# ==========================================

IP="127.0.0.1"
PORT="8080"
SESSION_FILE="$HOME/.jinx.session"
CURL_FLAGS=(-fsS)

# 使用内置 getopts 解析短选项（Mac/Linux 完美兼容）
while getopts "i:p:xh" opt; do
    case $opt in
        i) IP="$OPTARG" ;;
        p) PORT="$OPTARG" ;;
        x)
          set -x
          CURL_FLAGS+=(-v)
          ;;
        h) usage; exit 0 ;;
        \?) echo "Error: Unknown option '-$OPTARG'"; usage; exit 1 ;;
        :) echo "Error: Option '-$OPTARG' requires an argument."; usage; exit 1 ;;
    esac
done

# 移除已解析的选项参数，使 $1 重新指向第一个非选项参数（即 COMMAND）
shift $((OPTIND - 1))

# ==========================================
# 4. 核心改动：合并管道输入与命令参数
# ==========================================

COMMAND="$1"

# 1. 尝试读取管道（标准输入）内容
# 使用 -t 0 判断是否有管道输入，避免在没有管道时脚本卡住等待输入
if [ -t 0 ]; then
    STDIN_CONTENT=""
else
    STDIN_CONTENT=$(cat)
fi

# 2. 解析子命令
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
        curl -s "http://${IP}:${PORT}/api/version" && echo ""
        ;;
    info)
        curl -s "http://${IP}:${PORT}/api/info" && echo ""
        ;;
    help)
        usage
        exit 0
        ;;
    "")
        # 没有命令参数，仅发送管道内容
        send_chat_message "$STDIN_CONTENT"
        ;;
    *)
        # 有命令参数（如 "总共多少行?"）
        # 判断是否有管道内容，进行智能拼接
        if [ -n "$STDIN_CONTENT" ]; then
            # 既有管道又有命令，中间加换行符拼接
            FINAL_MESSAGE="${STDIN_CONTENT}"$'\n'"${COMMAND}"
        else
            # 只有命令参数，没有管道
            FINAL_MESSAGE="$COMMAND"
        fi
        send_chat_message "$FINAL_MESSAGE"
        ;;
esac