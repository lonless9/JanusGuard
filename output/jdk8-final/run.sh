#!/bin/bash

# JanusGuard JDK 8兼容版运行脚本

# 检查环境
if ! command -v java &> /dev/null; then
    echo "错误: 找不到Java，请确保已安装JDK 8"
    exit 1
fi

# 检查Java版本
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "检测到Java版本: $java_version"

# 检查jar文件
if [ ! -f "lib/janusguard-agent-jdk8-1.0.0-SNAPSHOT.jar" ]; then
    echo "错误: 找不到Agent JAR文件"
    exit 1
fi

# 检查配置文件
if [ ! -f "config/janusguard.yaml" ]; then
    echo "错误: 找不到配置文件"
    exit 1
fi

# 检查应用JAR
if [ -z "$1" ]; then
    echo "用法: $0 <应用JAR文件路径>"
    echo "例如: $0 /path/to/springbootshiro.jar"
    exit 1
fi

APP_JAR="$1"

if [ ! -f "$APP_JAR" ]; then
    echo "错误: 找不到应用JAR文件: $APP_JAR"
    exit 1
fi

echo "启动应用，使用JanusGuard JDK 8兼容版监控..."
java -javaagent:lib/janusguard-agent-jdk8-1.0.0-SNAPSHOT.jar=config=config/janusguard.yaml -jar "$APP_JAR" 