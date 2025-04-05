# JanusGuard JDK 8 兼容版

这是JanusGuard的JDK 8兼容版本，专为需要在Java 8环境下运行的应用程序设计。

## 特点

- 完全兼容JDK 8
- 轻量级设计，无外部依赖
- 基本的类加载监控功能
- 使用JDK内置日志系统

## 目录结构

```
janusguard-jdk8-simple/
├── lib/
│   └── janusguard-jdk8-simple.jar  # Java Agent JAR文件
├── README.md                       # 说明文档
└── run.sh                          # 启动脚本
```

## 使用方法

使用提供的run.sh脚本启动你的Java应用：

```
./run.sh [JVM选项] -jar 你的应用.jar [应用参数]
```

例如：

```
./run.sh -Xmx512m -jar springbootshiro.jar
```

## 日志查看

Agent日志将保存在应用程序运行目录下的`janusguard-jdk8.log`文件中。 