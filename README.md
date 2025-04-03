# 🛡️ JanusGuard

<div align="center">
  <img src="https://img.shields.io/badge/Java-8%2B-orange" alt="Java 8+">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License">
  <img src="https://img.shields.io/badge/Status-Alpha-yellow" alt="Status">
  <br>
  <strong>Java应用运行时安全监控系统</strong>
  <p>基于Java Agent和字节码转换技术的应用安全守护方案</p>
</div>

## 📋 概述

JanusGuard是一个Java应用运行时安全监控系统，通过Java Agent技术和字节码转换实现对应用行为的监控和安全风险检测，旨在提供一种低侵入、可配置的安全防护方案。

## 🔍 核心理念

- **📊 低侵入监控:** 最小化性能开销，对应用透明
- **🧠 智能分析:** 结合上下文和行为分析进行安全判断
- **⚙️ 动态调整:** 支持运行时调整监控范围和策略
- **🔗 事件关联:** 分析多个事件间关系以发现复杂攻击模式 
- **🧩 扩展性设计:** 支持自定义监控点和分析方法

## ✨ 功能特点

- **🔄 无代码修改:** 无需更改应用源码，直接通过JVM参数加载
- **⚡ 高效执行:** 采用轻量级探针设计和高性能队列处理
- **🔧 灵活配置:** 支持多种配置方式和策略调整
- **🚀 简便部署:** 支持启动时加载或运行时动态附加
- **🔎 全面监控:** 覆盖常见安全敏感操作点

## ✅ 已实现功能

目前版本已完成的功能：

- **🏗️ 核心框架:** 
  - Agent生命周期管理
  - 配置加载与验证
  - 组件初始化与终止

- **👁️ 监控点:** 
  - 命令执行 (`Runtime.exec`, `ProcessBuilder.start`)
  - 文件操作 (`FileInputStream`, `FileOutputStream`, `RandomAccessFile`)
  - 反射调用 (`Method.invoke`)

- **📊 事件处理:** 
  - 事件收集与过滤
  - 异步队列处理
  - 本地存储记录

- **⚙️ 配置系统:** 
  - YAML配置文件支持
  - 命令行参数设置

## 🏛️ 架构设计

```mermaid
graph TD
    A[目标Java应用] -- JVM启动/运行时加载 --> B(JanusGuard Agent);
    B -- 使用 java.lang.instrument --> C{JVM};
    C -- ClassFileTransformer --> D[字节码转换引擎];
    D -- 配置驱动 --> E[目标类/方法选择];
    D -- Byte Buddy --> F[字节码增强];
    F -- 植入 --> G[监控探针];
    G -- 事件产生 --> H[事件处理模块];
    H -- 过滤/处理 --> I[高性能事件队列];
    I -- 异步上报 --> J{数据上报服务};
    K[配置管理] -- 加载/更新 --> B;
    K -- 策略下发 --> H;
    J -- 数据流 --> L[分析平台];
    J -- 控制指令 --> K;
    L -- 告警生成 --> M[安全团队];
    L -- 模型训练 --> N[行为基线];
    N -- 更新规则 --> J;

    classDef main fill:#f9f,stroke:#333,stroke-width:1px
    classDef engine fill:#ccf,stroke:#333,stroke-width:1px
    classDef monitor fill:#ccf,stroke:#333,stroke-width:1px
    classDef proc fill:#fcc,stroke:#333,stroke-width:1px
    classDef report fill:#ff9,stroke:#333,stroke-width:1px
    classDef backend fill:#9cf,stroke:#333,stroke-width:1px
    
    class B main
    class D,F engine
    class G monitor
    class H proc
    class J report
    class L,N backend
```

## 🚀 快速开始

### 📥 安装

从[Releases](https://github.com/your-repo/janusguard/releases)下载最新版本的JanusGuard Agent JAR文件。

### ▶️ 在应用启动时加载Agent

```bash
java -javaagent:/path/to/janusguard-agent-1.0.0.jar -jar your-application.jar
```

### ⚙️ 配置Agent

通过命令行参数或配置文件进行配置：

```bash
java -javaagent:/path/to/janusguard-agent-1.0.0.jar=config=/path/to/config.yaml -jar your-application.jar
```

## 📝 配置参考

JanusGuard支持以下配置选项：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `monitor.command` | 启用命令执行监控 | `true` |
| `monitor.file` | 启用文件操作监控 | `true` |
| `monitor.reflection` | 启用反射调用监控 | `true` |
| `sampling.rate` | 事件采样率 (0-100) | `100` |
| `report.mode` | 上报模式 (file/http/grpc) | `file` |
| `log.level` | 日志级别 | `INFO` |

详细配置说明请参考[配置文档](docs/configuration.md)。

## 🛠️ 构建项目

项目使用Gradle构建：

```bash
# 克隆仓库
git clone https://github.com/your-repo/janusguard.git
cd janusguard

# 构建项目
./gradlew build

# 构建不执行测试
./gradlew build -x test
```

构建产物位于`build/libs/`目录：
- `janusguard-agent-1.0.0-SNAPSHOT.jar` - 包含所有依赖的完整Agent包
- `jvm_agent-1.0.0-SNAPSHOT.jar` - 不包含依赖的基础包

## 📂 项目结构

```
jvm_agent/
├── src/                             # 源代码
│   ├── main/java/com/janusguard/
│   │   ├── agent/                   # Agent主体
│   │   ├── transformer/             # 字节码转换
│   │   ├── core/                    # 核心功能
│   │   ├── transport/               # 数据传输
│   │   ├── config/                  # 配置管理
│   │   └── common/                  # 公共工具
│   └── test/                        # 测试代码
├── examples/                        # 示例应用
├── docs/                            # 文档
└── build.gradle                     # 构建配置
```

## 📚 技术栈

JanusGuard基于以下核心技术：

- **🔄 [Byte Buddy](https://bytebuddy.net/)**: 高级字节码生成和操作库
- **📝 [SLF4J](http://www.slf4j.org/) + [Logback](http://logback.qos.ch/)**: 日志框架
- **⚡ [LMAX Disruptor](https://lmax-exchange.github.io/disruptor/)**: 高性能线程间消息传递库
- **🔄 [Jackson](https://github.com/FasterXML/jackson)**: JSON处理
- **🌐 [gRPC](https://grpc.io/)**: 高性能RPC框架(可选)

## 🌟 特色功能

1. **🔄 自适应监控:** 支持根据应用行为特征调整监控策略
2. **🧠 上下文感知:** 探针可识别所处执行环境，提供多维度分析数据
3. **🔗 本地事件关联:** 在Agent端关联相关事件，提前发现可疑行为
4. **🧩 规则表达式:** 支持灵活的规则定义和匹配方式
5. **🔄 双向通信:** 支持与控制中心双向通信，接收策略更新
6. **📊 性能优化:** 采用多种性能优化技术，降低监控开销

## 📅 开发路线

1. **🏁 阶段一: 核心框架与基础监控** ✓
   * 基础框架与探针实现
   * 核心监控点支持
   * 本地日志记录
   
2. **🚧 阶段二: 增强监控与性能优化** (进行中)
   * 网络流量监控
   * 动态类加载监控
   * 高性能事件处理
   * 规则引擎设计
   
3. **🔮 阶段三: 智能化与动态策略**
   * 双向通信机制
   * 配置热更新
   * 本地事件关联分析
   * 采样策略实现
   
4. **✨ 阶段四: 高级特性与生态整合**
   * 数据流跟踪
   * 分析平台对接
   * 兼容性增强
   * 完善文档与工具

详细开发计划请参考[项目进度](project_dev.md)。

## 🤝 贡献指南

我们欢迎各种形式的贡献，包括但不限于功能建议、代码提交、文档改进等。

详情请参阅[贡献指南](CONTRIBUTING.md)。

## 📄 许可证

[MIT License](LICENSE)

---

<div align="center">
  <sub>Built with ❤️ by the Shallow Ignore Team</sub>
</div> 