#!/bin/bash

echo "Starting release process..."

# 1. 检查必要的文件和目录是否存在
if [ ! -f "pom.xml" ]; then
    echo "Error: pom.xml not found in current directory!"
    exit 1
fi

if [ ! -d "conf" ]; then
    echo "Error: conf directory not found!"
    exit 1
fi

if [ ! -d "bin" ]; then
    echo "Error: bin directory not found!"
    exit 1
fi

# 2. 使用 Maven 命令提取当前项目的版本号
echo "Extracting version from pom.xml using Maven..."
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

# 直接检查变量是否为空或包含错误信息
if [ -z "$VERSION" ] || [[ "$VERSION" == *"ERROR"* ]]; then
    echo "Error: Failed to extract version from pom.xml!"
    exit 1
fi

echo "Extracted version: $VERSION"

# 定义相关的变量
JAR_NAME="jinx-${VERSION}-all.jar"
RELEASE_DIR="release-${VERSION}"
TARGET_JAR="target/${JAR_NAME}"

# 3. 执行 Maven 打包 (优化点：直接用 if ! 检查 mvn 命令)
echo "Running mvn clean package..."
if ! mvn clean package -DskipTests; then
    echo "Error: Maven build failed!"
    exit 1
fi

# 4. 检查生成的 jar 包是否存在
if [ ! -f "$TARGET_JAR" ]; then
    echo "Error: Jar file not found at $TARGET_JAR!"
    exit 1
fi

# 5. 创建 release 目录结构
echo "Creating release directory structure..."

# 如果目录已存在，先删除
if [ -d "$RELEASE_DIR" ]; then
    echo "Warning: Directory $RELEASE_DIR already exists. Removing it..."
    rm -rf "$RELEASE_DIR"
fi

# 优化点：直接用 if 检查 mkdir 命令
if ! mkdir -p "$RELEASE_DIR"/{skills,data,logs}; then
    echo "Error: Failed to create release directories!"
    exit 1
fi

# 6. 复制文件和目录
echo "Copying files to release directory..."

# 优化点：直接用 if 检查 cp 命令
if ! cp -r bin "$RELEASE_DIR/"; then
    echo "Error: Failed to copy bin directory!"
    exit 1
fi

if ! cp -r conf "$RELEASE_DIR/"; then
    echo "Error: Failed to copy conf directory!"
    exit 1
fi

# 复制 jar 包到 libs 目录
if ! mkdir -p "$RELEASE_DIR/libs" || ! cp "$TARGET_JAR" "$RELEASE_DIR/libs/"; then
    echo "Error: Failed to copy jar file to libs!"
    exit 1
fi

# 7. 完成提示
echo "========================================"
echo "Release package created successfully!"
echo "Location: ./$RELEASE_DIR"
echo "========================================"