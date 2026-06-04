#!/bin/bash

BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(dirname "$BIN_DIR")"

LIBS_DIR="$APP_HOME/libs"
LOGS_DIR="$APP_HOME/logs"
PID_FILE="$APP_HOME/jinx.pid"

# 自动查找 jar 包
JAR_FILE=$(find "$LIBS_DIR" -name "jinx-*-all.jar" | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "Error: jinx-*-all.jar not found in $LIBS_DIR"
    exit 1
fi

JAR_NAME=$(basename "$JAR_FILE")
mkdir -p "$LOGS_DIR"

# 检查进程是否存活的原生方法
is_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        # kill -0 不发送任何信号，只检查进程是否存在且有权限操作
        if kill -0 "$PID" 2>/dev/null; then
            return 0 # 进程存活
        else
            return 1 # 进程已死
        fi
    fi
    return 1
}

start() {
    if is_running; then
        PID=$(cat "$PID_FILE")
        echo "Service is already running (PID: $PID)."
        return 1
    else
        # 如果进程死了但 PID 文件还在，清理它
        if [ -f "$PID_FILE" ]; then
            echo "Detected stale PID file. Cleaning up..."
            rm -f "$PID_FILE"
        fi
    fi

    echo "Starting $JAR_NAME..."

    # 直接后台启动 Java 进程
    cd "$APP_HOME" || exit 1
    nohup java -jar "$JAR_FILE" > "$LOGS_DIR/console.log" 2>&1 &

    # 获取 Java 进程的真实 PID
    JAVA_PID=$!

    # 稍微等待一下，确保进程没有瞬间崩溃
    sleep 1

    # 检查进程是否还活着
    if kill -0 "$JAVA_PID" 2>/dev/null; then
        echo "$JAVA_PID" > "$PID_FILE"
        echo "Service started successfully (PID: $JAVA_PID)."
    else
        echo "Error: Service failed to start (check $LOGS_DIR/console.log for details)."
        return 1
    fi
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "Service is not running (PID file not found)."
        return 1
    fi

    PID=$(cat "$PID_FILE")

    # 再次确认进程是否真的活着
    if ! kill -0 "$PID" 2>/dev/null; then
        echo "Service is not running (Process $PID is dead). Cleaning up stale PID file."
        rm -f "$PID_FILE"
        return 1
    fi

    echo "Stopping service (PID: $PID)..."
    kill "$PID" 2>/dev/null

    WAIT_COUNT=0
    # 循环检查进程是否退出
    while kill -0 "$PID" 2>/dev/null; do
        sleep 1
        WAIT_COUNT=$((WAIT_COUNT + 1))
        if [ $WAIT_COUNT -ge 10 ]; then
            echo "Graceful shutdown timeout. Force killing service (PID: $PID)..."
            kill -9 "$PID" 2>/dev/null
            break
        fi
    done

    rm -f "$PID_FILE"
    echo "Service stopped."
}

status() {
    if is_running; then
        PID=$(cat "$PID_FILE")
        echo "Service is running (PID: $PID)."
    else
        if [ -f "$PID_FILE" ]; then
            echo "Service is not running (Stale PID file found)."
        else
            echo "Service is not running."
        fi
    fi
}

restart() {
    stop
    sleep 2
    start
}

show_help() {
    echo "Usage: $0 {start|stop|restart|status|help}"
}

case "$1" in
    start) start ;;
    stop) stop ;;
    restart) restart ;;
    status) status ;;
    help|*) show_help ;;
esac