#!/bin/bash

# Jinux 操作系统启动脚本（Maven 版）

echo "=========================================="
echo "  Jinux Operating System - Build & Run"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

# 设置 JAVA_HOME 为 Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
echo "[INFO] Using Java: $JAVA_HOME"
echo ""

echo "[BUILD] Compiling with Maven..."
mvn -q compile

if [ $? -eq 0 ]; then
    echo "[BUILD] Compilation successful!"
else
    echo "[BUILD] Compilation failed!"
    exit 1
fi

echo ""
echo "[RUN] Starting Jinux..."
echo ""

# 运行 Jinux
mvn -q exec:java

echo ""
echo "[EXIT] Jinux terminated"
