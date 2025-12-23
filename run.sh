#!/bin/bash

# Jinux 操作系统启动脚本（Maven 版）

echo "=========================================="
echo "  Jinux Operating System - Build & Run"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

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
