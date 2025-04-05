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