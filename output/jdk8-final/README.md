# JanusGuard JDK 8兼容版

这是JanusGuard的JDK 8兼容版本，专为需要在Java 8环境中运行的应用程序设计。

## 功能特点

这个简化版JanusGuard提供以下关键功能：

1. **类加载监控** - 跟踪应用程序加载的所有非系统类
2. **反射调用监控** - 监控可能危险的反射操作，如访问Runtime.exec()
3. **文件操作监控** - 监控文件系统读写和删除操作
4. **命令执行监控** - 监控对Runtime.exec()和ProcessBuilder的调用
5. **危险操作阻止** - 可选启用的阻止模式，拦截潜在危险操作
6. **黑白名单支持** - 可配置的文件和命令黑白名单
7. **详细日志记录** - 记录所有监控事件和安全告警
8. **完全JDK 8兼容** - 无需任何JDK 9+特性

## 使用方法

### 基本用法

将JanusGuard作为Java Agent挂载到您的Java应用程序：

```bash
java -javaagent:janusguard-jdk8-simple.jar -jar your-application.jar
```

### 使用配置文件

指定配置文件：

```bash
java -javaagent:janusguard-jdk8-simple.jar=config=janusguard-config.properties -jar your-application.jar
```

### 指定日志文件

自定义日志文件位置：

```bash
java -javaagent:janusguard-jdk8-simple.jar=log=custom-janusguard.log -jar your-application.jar
```

### 同时指定配置和日志

```bash
java -javaagent:janusguard-jdk8-simple.jar=config=janusguard-config.properties,log=custom-janusguard.log -jar your-application.jar
```

## 配置选项

`janusguard-config.properties`文件支持以下配置选项：

```properties
# 基本设置
agent.name=JanusGuard
agent.version=1.0.0-JDK8
agent.enabled=true

# 日志设置
log.level=FINE          # 日志级别：SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST
log.file=janusguard.log # 日志文件位置
log.detailed=true       # 是否记录详细信息

# 监控设置
monitor.classloading=true       # 是否监控类加载
monitor.reflection=true         # 是否监控反射调用
monitor.fileoperations=true     # 是否监控文件操作
monitor.commandexecution=true   # 是否监控命令执行

# 安全设置
security.blocking-mode=false    # 是否启用阻止模式
security.alert-threshold=medium # 告警阈值：low, medium, high

# 危险操作黑名单
security.dangerous-commands=rm -rf,format,del /  # 危险命令模式
security.dangerous-files=/etc/passwd,/etc/shadow # 危险文件模式

# 白名单设置
security.whitelist-commands=ls,dir,git status    # 白名单命令
security.whitelist-files=/tmp/safe.txt           # 白名单文件
```

## 查看日志

默认情况下，JanusGuard会将监控日志写入到当前目录下的`janusguard-jdk8.log`文件中。您可以使用以下命令查看日志：

```bash
tail -f janusguard-jdk8.log
```

或指定自定义的日志文件位置：

```bash
tail -f custom-janusguard.log
```

## 故障排除

如果遇到问题：

1. 确保使用Java 8运行应用程序
2. 检查日志文件中的错误信息
3. 确保Agent的JAR文件路径正确
4. 尝试调整日志级别为FINE或FINEST以获取更多信息 