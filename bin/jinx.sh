#!/bin/bash

# --- Usage ---

usage() {
    cat << EOF
Usage: $0 [OPTIONS] [COMMAND]

A command-line tool for interacting with the remote Jinx server.

OPTIONS:
  -i IP           Specify target server IP (default: 127.0.0.1)
  -p PORT         Specify target server port (default: 8080)
  -s SESSION_ID   Specify session ID (auto-detected from local file if omitted)
  -x              Enable debug mode (equivalent to bash -x)
  -h              Show this help message

LOCAL COMMANDS:
  session           Show the current SESSION-ID
  session new       Generate a new SESSION-ID and save locally

EXAMPLES:
  $0 chat "What day is it today?"
  echo "Hello" | $0 chat
  cat app.log | $0 chat "分析以上日志"
  $0 version
  $0 mcp list
  $0 -s S1234567890 chat "Hello"
  $0 -i 192.168.1.50 -p 9000 version
EOF
}

# --- Session ---

generate_session() {
    local new_session="S"
    local chars="0123456789"
    for (( i=0; i<32; i++ )); do
        new_session+="${chars:RANDOM % ${#chars}:1}"
    done
    echo "$new_session" > "$SESSION_FILE"
    echo "$new_session"
}

get_session_id() {
    if [ ! -f "$SESSION_FILE" ]; then
        generate_session
    else
        cat "$SESSION_FILE"
    fi
}

# --- Remote ---

TMPDIR_EXEC=""

ensure_tmpdir() {
    if [ -z "$TMPDIR_EXEC" ]; then
        TMPDIR_EXEC=$(mktemp -d)
    fi
}

cleanup_tmpdir() {
    if [ -n "$TMPDIR_EXEC" ]; then
        rm -rf "$TMPDIR_EXEC"
        TMPDIR_EXEC=""
    fi
}

# 提交表单到远程执行端点
post_form() {
    local headers=()
    if [ -n "$SESSION_ID" ]; then
        headers+=(-H "X-Jinx-Session: $SESSION_ID")
    fi
    curl "$CURL_FLAGS" "${headers[@]}" "$@" "http://${IP}:${PORT}/api/cli/execute"
    local rc=$?
    echo ""
    return $rc
}

# 提交远程命令，参数逐个内联为表单字段上传
execute_remote() {
    local cmd="$1"; shift
    local data_args=(--data-urlencode "cmd=${cmd}")
    for arg in "$@"; do
        data_args+=(--data-urlencode "args=${arg}")
    done
    post_form "${data_args[@]}"
}

show_help() {
    usage
    echo ""
    if ! execute_remote help 2>/dev/null; then
        echo "(Could not fetch remote commands from server)"
    fi
    echo ""
}

# --- Config & Options ---

IP="127.0.0.1"
PORT="8080"
SESSION_FILE="$HOME/.jinx.session"
SESSION_ID=""
CURL_FLAGS=(-fsS)

while getopts "i:p:s:xh" opt; do
    case $opt in
        i) IP="$OPTARG" ;;
        p) PORT="$OPTARG" ;;
        s) SESSION_ID="$OPTARG" ;;
        x) set -x; CURL_FLAGS+=(-v) ;;
        h) show_help; exit 0 ;;
        \?) echo "Error: Unknown option '-$OPTARG'"; usage; exit 1 ;;
        :) echo "Error: Option '-$OPTARG' requires an argument."; usage; exit 1 ;;
    esac
done
shift $((OPTIND - 1))

# Resolve session ID: -s > local file
if [ -z "$SESSION_ID" ]; then
    SESSION_ID=$(get_session_id)
fi

# --- Command Routing ---

COMMAND="$1"

case "$COMMAND" in

    session)
        if [ -z "$2" ]; then
            echo "$SESSION_ID"
        elif [ "$2" = "new" ]; then
            SESSION_ID=$(generate_session)
            echo "New session generated: $SESSION_ID"
        else
            echo "Error: Unknown session subcommand."
            usage
            exit 1
        fi
        ;;

    "")
        usage
        exit 1
        ;;

    *)
        shift
        # STDIN 输入仅对 chat 命令生效：落盘保留原始字节（含末尾换行），经临时文件上传
        if [ "$COMMAND" = "chat" ] && [ ! -t 0 ]; then
            ensure_tmpdir
            stdin_file="$TMPDIR_EXEC/stdin"
            cat > "$stdin_file"
            if [ ! -s "$stdin_file" ] && [ $# -eq 0 ]; then
                cleanup_tmpdir
                echo "Error: Message is empty. (stdin 为空且未提供参数)" >&2
                exit 1
            fi
            if [ $# -gt 0 ]; then
                if [ -s "$stdin_file" ]; then
                    printf '\n%s' "$*" >> "$stdin_file"
                else
                    printf '%s' "$*" >> "$stdin_file"
                fi
            fi
            # MSYS 环境下转换为 Windows 路径，便于原生 curl 读取
            if command -v cygpath > /dev/null 2>&1; then
                stdin_file=$(cygpath -m "$stdin_file")
            fi
            post_form --data-urlencode "cmd=${COMMAND}" --data-urlencode "args@${stdin_file}"
            rc=$?
            cleanup_tmpdir
            exit $rc
        else
            execute_remote "$COMMAND" "$@"
        fi
        ;;
esac
