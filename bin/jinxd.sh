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

start() {
    # 检查是否已经有存活的进程持有锁
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        # 尝试获取锁，如果失败(-n非阻塞模式下获取失败)，说明原进程还活着并持有锁
        if ! flock -n "$PID_FILE" -c "exit 0" 2>/dev/null; then
            echo "Service is already running (PID: $PID)."
            return 1
        else
            echo "Detected stale PID file. Cleaning up..."
            rm -f "$PID_FILE"
        fi
    fi

    echo "Starting $JAR_NAME..."

    # 核心改动：
    # 1. 使用 ( ... & ) 将 nohup 整体放入后台，因为 java 命令本身是阻塞的。
    # 2. 在子 shell 中先 cd 到 APP_HOME，确保 java 进程的工作目录正确。
    # 3. flock 包裹 java 进程。只要 java 阻塞运行，锁就一直被持有。
    (
        cd "$APP_HOME" || exit 1
        exec flock "$PID_FILE" nohup java -jar "$JAR_FILE" > "$LOGS_DIR/console.log" 2>&1
    ) &

    # 获取刚刚放入后台的子 shell 的 PID
    LAUNCHER_PID=$!
    # 稍微等待一下，让子 shell 成功获取到文件锁
    sleep 1

    # 核心校验：再次尝试获取锁。
    # 如果获取失败，证明锁正被我们的 java 进程死死按住，说明启动成功！
    if ! flock -n "$PID_FILE" -c "exit 0" 2>/dev/null; then
        echo "$LAUNCHER_PID" > "$PID_FILE"
        echo "Service started successfully (PID: $LAUNCHER_PID)."
    else
        echo "Error: Service failed to start (check $LOGS_DIR/console.log for details)."
        rm -f "$PID_FILE"
        return 1
    fi
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "Service is not running (PID file not found)."
        return 1
    fi

    PID=$(cat "$PID_FILE")
    # 尝试获取锁，如果成功，说明持有锁的进程已经挂了
    if flock -n "$PID_FILE" -c "exit 0" 2>/dev/null; then
        echo "Service is not running (Process $PID is dead). Cleaning up stale PID file."
        rm -f "$PID_FILE"
        return 1
    fi

    echo "Stopping service (PID: $PID)..."
    # 杀死启动器子 shell 的 PID，内部的 java 进程会随之收到信号并退出
    kill "$PID" 2>/dev/null

    WAIT_COUNT=0
    # 循环检查锁是否被释放
    while ! flock -n "$PID_FILE" -c "exit 0" 2>/dev/null; do
        sleep 1
        WAIT_COUNT=$((WAIT_COUNT + 1))
        if [ $WAIT_COUNT -ge 10 ]; then
            echo "Force killing service (PID: $PID)..."
            kill -9 "$PID" 2>/dev/null
            break
        fi
    done

    rm -f "$PID_FILE"
    echo "Service stopped."
}

status() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        # 尝试拿锁，拿不到说明进程活着
        if ! flock -n "$PID_FILE" -c "exit 0" 2>/dev/null; then
            echo "Service is running (PID: $PID)."
        else
            echo "Service is not running (Stale PID file found)."
        fi
    else
        echo "Service is not running."
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