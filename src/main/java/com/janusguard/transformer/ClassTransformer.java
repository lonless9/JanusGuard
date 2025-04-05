package com.janusguard.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.janusguard.agent.AgentConfig;
import com.janusguard.core.event.EventProcessor;
import com.janusguard.transformer.interceptor.CommandExecutionInterceptor;
import com.janusguard.transformer.interceptor.FileOperationInterceptor;
import com.janusguard.transformer.interceptor.ReflectionInterceptor;
import com.janusguard.transformer.interceptor.ClassLoaderInterceptor;
import com.janusguard.transformer.interceptor.UnsafeInterceptor;
import com.janusguard.transformer.interceptor.DynamicProxyInterceptor;
import com.janusguard.transformer.interceptor.JNIInterceptor;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

/**
 * 字节码转换器
 * 使用ByteBuddy库来实现字节码转换，植入安全监控探针
 */
public class ClassTransformer implements ClassFileTransformer {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassTransformer.class);
    
    private final AgentConfig config;
    private final EventProcessor eventProcessor;
    private final Instrumentation instrumentation;
    
    // 类转换缓存：类名 -> 是否已转换
    private final ConcurrentMap<String, Boolean> transformCache = new ConcurrentHashMap<>();
    
    // 排除的包前缀
    private final Set<String> excludedPackages = new HashSet<>();
    
    // 包含的包前缀
    private final Set<String> includedPackages = new HashSet<>();
    
    // ByteBuddy Agent Builder
    private final AgentBuilder agentBuilder;
    
    /**
     * 构造函数
     * 
     * @param config Agent配置
     * @param eventProcessor 事件处理器
     */
    public ClassTransformer(AgentConfig config, EventProcessor eventProcessor, Instrumentation instrumentation) {
        this.config = config;
        this.eventProcessor = eventProcessor;
        this.instrumentation = instrumentation;
        
        // 初始化排除包列表
        String[] excluded = config.getString("transformer.excluded-packages", "java.lang,sun.,com.sun.,com.janusguard.").split(",");
        excludedPackages.addAll(Arrays.asList(excluded));
        
        // 初始化包含包列表
        String[] included = config.getString("transformer.included-packages", "").split(",");
        for (String pkg : included) {
            if (!pkg.trim().isEmpty()) {
                includedPackages.add(pkg.trim());
            }
        }
        
        // 设置拦截器的事件处理器
        CommandExecutionInterceptor.setEventProcessor(eventProcessor);
        FileOperationInterceptor.setEventProcessor(eventProcessor);
        ReflectionInterceptor.setEventProcessor(eventProcessor);
        // 设置新拦截器的事件处理器
        ClassLoaderInterceptor.setEventProcessor(eventProcessor);
        UnsafeInterceptor.setEventProcessor(eventProcessor);
        DynamicProxyInterceptor.setEventProcessor(eventProcessor);
        JNIInterceptor.setEventProcessor(eventProcessor);
        
        // 创建ByteBuddy AgentBuilder
        agentBuilder = new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onTransformation(TypeDescription typeDescription, 
                                                 ClassLoader classLoader, 
                                                 JavaModule module,
                                                 boolean loaded,
                                                 DynamicType dynamicType) {
                        logger.debug("Transformed class: {}", typeDescription.getName());
                    }
                    
                    @Override
                    public void onError(String typeName, 
                                       ClassLoader classLoader, 
                                       JavaModule module,
                                       boolean loaded, 
                                       Throwable throwable) {
                        logger.error("Error transforming class {}: {}", typeName, throwable.getMessage(), throwable);
                    }
                    
                    @Override
                    public void onIgnored(TypeDescription typeDescription, 
                                         ClassLoader classLoader, 
                                         JavaModule module,
                                         boolean loaded) {
                        logger.trace("Ignored class: {}", typeDescription.getName());
                    }
                });
        
        // 配置基本转换规则
        initializeTransformRules();
    }
    
    /**
     * 初始化转换规则
     */
    private void initializeTransformRules() {
        logger.info("Initializing class transform rules");
        
        AgentBuilder localBuilder = agentBuilder;
        
        // 监控Runtime.exec方法
        if (config.getBoolean("monitors.command-execution.enabled", true)) {
            logger.info("Command execution monitoring enabled");
            
            // 为Runtime.exec添加转换
            localBuilder = localBuilder.type(ElementMatchers.named("java.lang.Runtime"))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.method(ElementMatchers.named("exec"))
                               .intercept(MethodDelegation.to(CommandExecutionInterceptor.class))
                    );
            
            // 为ProcessBuilder.start添加转换
            localBuilder = localBuilder.type(ElementMatchers.named("java.lang.ProcessBuilder"))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.method(ElementMatchers.named("start"))
                               .intercept(MethodDelegation.to(CommandExecutionInterceptor.class))
                    );
        }
        
        // 监控文件操作
        if (config.getBoolean("monitors.file-operations.enabled", true)) {
            logger.info("File operations monitoring enabled");
            
            // 为FileInputStream添加转换
            localBuilder = localBuilder.type(ElementMatchers.named("java.io.FileInputStream"))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.constructor(ElementMatchers.any())
                               .intercept(MethodDelegation.to(FileOperationInterceptor.class))
                               .method(ElementMatchers.named("read"))
                               .intercept(MethodDelegation.to(FileOperationInterceptor.class))
                    );
            
            // 为FileOutputStream添加转换
            localBuilder = localBuilder.type(ElementMatchers.named("java.io.FileOutputStream"))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.constructor(ElementMatchers.any())
                               .intercept(MethodDelegation.to(FileOperationInterceptor.class))
                               .method(ElementMatchers.named("write"))
                               .intercept(MethodDelegation.to(FileOperationInterceptor.class))
                    );
            
            // 为RandomAccessFile添加转换
            localBuilder = localBuilder.type(ElementMatchers.named("java.io.RandomAccessFile"))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.constructor(ElementMatchers.any())
                               .intercept(MethodDelegation.to(FileOperationInterceptor.class))
                               .method(ElementMatchers.nameStartsWith("read").or(ElementMatchers.nameStartsWith("write")))
                               .intercept(MethodDelegation.to(FileOperationInterceptor.class))
                    );
        }
        
        // 监控反射调用
        if (config.getBoolean("monitors.reflection.enabled", true)) {
            logger.info("Reflection monitoring enabled");
            
            // 为Method.invoke添加转换
            localBuilder = localBuilder.type(ElementMatchers.named("java.lang.reflect.Method"))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.method(ElementMatchers.named("invoke"))
                               .intercept(MethodDelegation.to(ReflectionInterceptor.class))
                    );
        }
        
        // 监控ClassLoader.defineClass - 内存木马注入检测
        if (config.getBoolean("monitors.memory-trojan.class-loading.enabled", true)) {
            logger.info("Memory trojan class loading monitoring enabled");
            
            localBuilder = localBuilder.type(ElementMatchers.isSubTypeOf(ClassLoader.class))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.method(ElementMatchers.nameStartsWith("defineClass"))
                               .intercept(MethodDelegation.to(ClassLoaderInterceptor.class))
                    );
        }
        
        // 监控Unsafe内存操作 - 内存木马注入检测
        if (config.getBoolean("monitors.memory-trojan.unsafe.enabled", true)) {
            logger.info("Memory trojan Unsafe operations monitoring enabled");
            
            localBuilder = localBuilder.type(ElementMatchers.named("sun.misc.Unsafe"))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.method(ElementMatchers.anyOf(
                                ElementMatchers.named("putAddress"),
                                ElementMatchers.named("putObject"),
                                ElementMatchers.named("allocateInstance"),
                                ElementMatchers.named("defineClass"),
                                ElementMatchers.named("defineAnonymousClass"),
                                ElementMatchers.named("allocateMemory"),
                                ElementMatchers.named("copyMemory")
                            ))
                            .intercept(MethodDelegation.to(UnsafeInterceptor.class))
                    );
        }
        
        // 监控动态代理 - 内存木马注入检测
        if (config.getBoolean("monitors.memory-trojan.dynamic-proxy.enabled", true)) {
            logger.info("Memory trojan dynamic proxy monitoring enabled");
            
            localBuilder = localBuilder.type(ElementMatchers.named("java.lang.reflect.Proxy"))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.method(ElementMatchers.named("newProxyInstance"))
                               .intercept(MethodDelegation.to(DynamicProxyInterceptor.class))
                    );
        }
        
        // 监控JNI操作 - 内存木马注入检测
        if (config.getBoolean("monitors.memory-trojan.jni.enabled", true)) {
            logger.info("Memory trojan JNI operations monitoring enabled");
            
            localBuilder = localBuilder.type(ElementMatchers.named("java.lang.System"))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.method(ElementMatchers.named("load").or(ElementMatchers.named("loadLibrary")))
                               .intercept(MethodDelegation.to(JNIInterceptor.class))
                    );
            
            localBuilder = localBuilder.type(ElementMatchers.named("java.lang.Runtime"))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> 
                        builder.method(ElementMatchers.named("load").or(ElementMatchers.named("loadLibrary")))
                               .intercept(MethodDelegation.to(JNIInterceptor.class))
                    );
        }
        
        // 安装转换器
        try {
            localBuilder.installOn(instrumentation);
            logger.info("ByteBuddy transformers installed successfully");
        } catch (Exception e) {
            logger.error("Failed to install ByteBuddy transformers", e);
        }
    }
    
    /**
     * 实现ClassFileTransformer接口的transform方法
     */
    @Override
    public byte[] transform(ClassLoader loader, 
                           String className, 
                           Class<?> classBeingRedefined, 
                           ProtectionDomain protectionDomain, 
                           byte[] classfileBuffer) throws IllegalClassFormatException {
        
        // 转换类名格式
        String normalizedClassName = className.replace('/', '.');
        
        // 检查缓存
        if (transformCache.containsKey(normalizedClassName)) {
            return null; // 已经处理过的类不再处理
        }
        
        // 检查是否应该排除
        if (shouldExclude(normalizedClassName)) {
            transformCache.put(normalizedClassName, false);
            return null;
        }
        
        // 检查是否在包含列表中或者包含列表为空
        if (!includedPackages.isEmpty() && !shouldInclude(normalizedClassName)) {
            transformCache.put(normalizedClassName, false);
            return null;
        }
        
        try {
            // 类转换逻辑已经由ByteBuddy处理，这里只做记录
            logger.debug("Class examined: {}", normalizedClassName);
            transformCache.put(normalizedClassName, true);
            return null;
        } catch (Exception e) {
            logger.error("Error during class transformation: {}", normalizedClassName, e);
            return null;
        }
    }
    
    /**
     * 判断是否应该排除类
     * 
     * @param className 类名
     * @return 如果应该排除则返回true
     */
    private boolean shouldExclude(String className) {
        // 排除所有在排除列表中的类
        for (String excludedPrefix : excludedPackages) {
            if (className.startsWith(excludedPrefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否应该包含类
     * 
     * @param className 类名
     * @return 如果应该包含则返回true
     */
    private boolean shouldInclude(String className) {
        // 包含所有在包含列表中的类
        for (String includedPrefix : includedPackages) {
            if (className.startsWith(includedPrefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取转换缓存大小
     * 
     * @return 缓存中的类数量
     */
    public int getCacheSize() {
        return transformCache.size();
    }
    
    /**
     * 清除转换缓存
     */
    public void clearCache() {
        transformCache.clear();
    }
} 