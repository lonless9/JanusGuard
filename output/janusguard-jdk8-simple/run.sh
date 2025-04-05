#!/bin/bash

# JanusGuard JDK 8兼容版启动脚本

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
AGENT_JAR="$SCRIPT_DIR/lib/janusguard-jdk8-simple.jar"

# 检查Agent JAR是否存在
if [ ! -f "$AGENT_JAR" ]; then
  echo "错误: Agent JAR文件不存在: $AGENT_JAR"
  exit 1
fi

# 处理参数
APPLICATION_JAR=""
JAVA_OPTS=""

while [[ $# -gt 0 ]]; do
  case $1 in
    -jar)
      APPLICATION_JAR="$2"
      shift
      shift
      ;;
    *)
      JAVA_OPTS="$JAVA_OPTS $1"
      shift
      ;;
  esac
done

if [ -z "$APPLICATION_JAR" ]; then
  echo "使用方法: $0 [JVM选项] -jar 应用JAR文件 [应用参数]"
  echo "示例: $0 -Xmx512m -jar myapp.jar"
  exit 1
fi

# 运行应用，附加JanusGuard Agent
echo "启动应用，使用JanusGuard JDK 8兼容版..."
echo "Java版本: $(java -version 2>&1 | head -n 1)"
echo "Agent JAR: $AGENT_JAR"
echo "应用JAR: $APPLICATION_JAR"

java $JAVA_OPTS -javaagent:$AGENT_JAR -jar $APPLICATION_JAR 