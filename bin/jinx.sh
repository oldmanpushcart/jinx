#!/bin/bash

# 定义脚本的使用说明
usage() {
    echo "Usage: $0 [-h|--help] [-v|--version]"
    exit 0
}

# 显示版本信息
show_version() {
    echo "MyScript version 1.0.0"
    exit 0
}

# 使用 getopt 来解析参数
# -o 指定短参数（h和v，后面加冒号:表示该选项需要接参数，这里不需要所以不加）
# -l 指定长参数（help和version）
# -- "$@" 表示传入脚本的所有原始参数
# 注意：如果你的长参数需要接值，写成 --name:
TEMP=$(getopt -o hv --long help,version -- "$@")

# 检查参数解析是否出错
if [ $? -ne 0 ]; then
    echo "Error: 参数解析失败"
    usage
fi

# 将解析后的参数重新赋值给位置参数 $1, $2...
eval set -- "$TEMP"

# 使用 while 循环和 case 语句来逐个处理参数
while true; do
    case "$1" in
        -h | --help)
            usage
            shift # 处理完一个参数后，向左移动一位
            ;;
        -v | --version)
            show_version
            shift
            ;;
        --)
            shift
            break # 遇到 -- 表示选项参数结束，跳出循环
            ;;
        *)
            echo "Error: 未知参数 '$1'"
            usage
            ;;
    esac
done

# 参数处理完毕后，执行你的主要业务逻辑
echo "参数处理完毕，开始执行主程序..."