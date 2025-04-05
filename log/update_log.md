# JanusGuard 更新日志

## 2023年4月4日 - 增强内存木马监控功能

### 新增功能

增加了针对Java内存木马注入方法的专用监控点，包括：

1. **类加载监控**
   - 监控 ClassLoader.defineClass 系列方法
   - 检测可疑的动态类加载行为
   - 分析加载类的来源和特征

2. **Unsafe内存操作监控**
   - 监控 sun.misc.Unsafe 类的关键方法
   - 重点监控 putAddress、putObject、allocateInstance 等高危操作
   - 识别可能的内存直接操作风险

3. **动态代理监控**
   - 监控 java.lang.reflect.Proxy.newProxyInstance 方法
   - 检测可疑的动态代理创建
   - 分析代理接口和调用上下文

4. **JNI操作监控**
   - 监控 System.load/loadLibrary 方法
   - 检测可疑的本地库加载
   - 分析库文件路径和调用堆栈

### 技术实现

1. 添加了四个新的拦截器类：
   - ClassLoaderInterceptor
   - UnsafeInterceptor
   - DynamicProxyInterceptor
   - JNIInterceptor

2. 在 SecurityEventType 中新增事件类型：
   - JVM_MEMORY_OPERATION
   - DYNAMIC_PROXY
   - JNI_OPERATION

3. 扩展了 ClassTransformer 的 initializeTransformRules 方法，增加新的监控规则

4. 更新了 EventProcessor 的事件处理逻辑，支持新监控点的事件分析

5. 在 AgentConstants 中增加了内存木马检测相关常量定义

### 安全增强价值

1. 大幅提高对内存木马攻击的检测能力
2. 能够识别多种常见的Java内存注入技术
3. 通过详细记录可疑操作，支持事后取证分析
4. 可配置的检测规则，支持按需开启监控

### 后续优化方向

1. 优化检测算法，降低误报率
2. 增加更多内存马特征识别能力
3. 实现可视化告警和实时拦截功能
4. 添加内存木马行为关联分析能力 

## 2023年5月20日 - 增强命令执行监控功能与应用集成

### 问题修复

1. **修复命令执行监控失效问题**
   - 解决了无法成功拦截和监控命令执行（特别是"whoami"命令）的问题
   - 修复了字节码转换机制未能正确注入监控代码的缺陷
   - 增强了对Runtime.exec()和ProcessBuilder.start()方法调用的监控能力

### 新增功能

1. **多层命令执行监控机制**
   - 引入SecurityManager监控层，拦截所有命令执行
   - 添加RuntimeExecHook直接监控Runtime.exec调用
   - 实现公共API接口，支持应用程序手动集成监控

2. **应用集成辅助工具**
   - 新增JanusGuardHelper辅助类，提供简单的命令执行API
   - 自动检测JanusGuard是否已安装
   - 支持多种集成方式，提高兼容性和灵活性

3. **危险命令检测增强**
   - 优化命令危险性检测逻辑，添加针对特定命令的直接检测
   - 增加对命令注入特征和网络连接命令的检测
   - 支持正则表达式和简单字符串匹配两种模式

4. **应用级集成示例**
   - 提供LoginController示例类，展示真实场景中的集成
   - 演示了三种安全修复方案的实现
   - 包含最佳实践指南，帮助开发者避免命令注入漏洞

### 技术实现

1. **轻量级监控架构**
   - 不依赖外部字节码库（如ByteBuddy），提高兼容性
   - 使用Java内置的安全管理器机制监控命令执行
   - 采用系统属性通知机制，支持运行时检测

2. **日志记录增强**
   - 改进日志记录系统，支持异步日志处理
   - 记录详细的调用堆栈，便于追踪命令来源
   - 提供命令执行统计信息，帮助安全分析

3. **兼容性优化**
   - 完全兼容JDK 8环境，无需高版本Java特性
   - 支持与现有应用无缝集成
   - 提供降级策略，确保核心监控功能始终可用

### 使用说明

1. **基本用法**
   ```bash
   java -javaagent:janusguard-jdk8-simple.jar -jar your-application.jar
   ```

2. **配置文件支持**
   ```bash
   java -javaagent:janusguard-jdk8-simple.jar=config=janusguard-config.properties -jar your-application.jar
   ```

3. **应用程序集成**
   - 使用JanusGuardHelper.execCommand()替代Runtime.exec()
   - 检查System.getProperty("janusguard.installed")确认Agent已加载
   - 直接调用SimpleJdk8Agent.beforeCommandExecution()进行手动监控

### 未来规划

1. 进一步提高字节码转换能力，不依赖SecurityManager
2. 提供更多防御模式，支持自定义命令过滤规则
3. 增加Web控制台，实时监控和管理安全事件
4. 开发与主流框架（Spring、Jakarta EE等）的集成模块 