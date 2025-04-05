package com.janusguard.transformer.interceptor;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.janusguard.common.constants.AgentConstants;
import com.janusguard.common.logging.LogUtils;
import com.janusguard.core.event.EventProcessor;
import com.janusguard.core.event.SecurityEvent;
import com.janusguard.core.event.SecurityEventType;
import com.janusguard.core.event.SecurityEventSeverity;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * 类加载拦截器
 * 监控ClassLoader.defineClass系列方法，用于检测内存木马注入
 */
public class ClassLoaderInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderInterceptor.class);
    private static EventProcessor eventProcessor;
    
    /**
     * 设置事件处理器
     * 
     * @param processor 事件处理器
     */
    public static void setEventProcessor(EventProcessor processor) {
        eventProcessor = processor;
    }
    
    /**
     * 拦截方法并记录事件
     * 
     * @param obj 目标对象
     * @param method 被拦截的方法
     * @param args 方法参数
     * @param callable 原始方法调用
     * @return 原始方法的返回值
     * @throws Exception 如果调用出错
     */
    @RuntimeType
    public static Object intercept(@This Object obj, 
                                  @Origin Method method,
                                  @AllArguments Object[] args,
                                  @SuperCall Callable<?> callable) throws Exception {
        String className = obj.getClass().getName();
        String methodName = method.getName();
        
        // 尝试获取堆栈
        String callStack = LogUtils.getStackTrace(2, 15); // 保留更多堆栈以便分析
        
        // 创建安全事件
        SecurityEvent event = new SecurityEvent(SecurityEventType.CLASS_LOADING, className, methodName);
        event.setSeverity(SecurityEventSeverity.HIGH); // 设置高风险级别
        
        // 添加类加载信息
        if (methodName.startsWith("defineClass")) {
            // 获取被加载的类名
            String definedClassName = null;
            if (args.length > 0 && args[0] instanceof String) {
                definedClassName = (String) args[0];
                event.addData(AgentConstants.EventFields.TARGET_CLASS, definedClassName);
            }
            
            // 如果提供了ProtectionDomain，记录它
            for (Object arg : args) {
                if (arg instanceof ProtectionDomain) {
                    ProtectionDomain domain = (ProtectionDomain) arg;
                    if (domain.getCodeSource() != null && domain.getCodeSource().getLocation() != null) {
                        event.addData("codeSource", domain.getCodeSource().getLocation().toString());
                    }
                    break;
                }
            }
            
            // 记录类加载器信息
            event.addData("classLoader", className);
            
            // 标记为可能的内存木马
            if (isMemoryTrojanSuspicious(callStack, definedClassName)) {
                event.addData("memoryTrojanSuspicious", true);
                event.addData("reason", "可疑类动态加载检测");
                logger.warn("内存木马可疑活动: {}", definedClassName);
            }
        }
        
        // 添加调用堆栈
        event.setCallStackTrace(callStack);
        
        // 记录事件前的时间
        long startTime = System.currentTimeMillis();
        
        // 调用原始方法
        Object result;
        try {
            result = callable.call();
            
            // 如果结果是Class类型，记录类信息
            if (result instanceof Class<?>) {
                Class<?> loadedClass = (Class<?>) result;
                event.addData("loadedClassName", loadedClass.getName());
                event.addData("classPackage", loadedClass.getPackage() != null ? loadedClass.getPackage().getName() : "null");
                
                // 检查类的修饰符，如果有特殊的访问特性，可能更可疑
                int modifiers = loadedClass.getModifiers();
                event.addData("classModifiers", modifiers);
            }
            
            // 添加执行成功标志
            event.addData("success", true);
            
            return result;
        } catch (Exception e) {
            // 添加异常信息
            event.addData("success", false);
            event.addData("exception", e.getClass().getName() + ": " + e.getMessage());
            
            // 重新抛出异常
            throw e;
        } finally {
            // 添加执行时间
            long endTime = System.currentTimeMillis();
            event.addData("executionTime", endTime - startTime);
            
            // 处理事件
            if (eventProcessor != null) {
                try {
                    eventProcessor.processEvent(event);
                } catch (Exception e) {
                    logger.error("处理类加载事件失败", e);
                }
            }
        }
    }
    
    /**
     * 检测可能的内存木马注入迹象
     * 
     * @param callStack 调用堆栈
     * @param className 被加载的类名
     * @return 如果可疑返回true
     */
    private static boolean isMemoryTrojanSuspicious(String callStack, String className) {
        // 可疑迹象1: 类名为null或空
        if (className == null || className.isEmpty()) {
            return true;
        }
        
        // 可疑迹象2: 不寻常的包名或类名命名
        if (className.contains("$$") || 
            className.startsWith("sun.") ||
            className.startsWith("jdk.internal") ||
            className.matches(".*[0-9]{5,}.*")) {
            return true;
        }
        
        // 可疑迹象3: 堆栈中包含可疑的调用方
        if (callStack != null && (
                callStack.contains("reflect.") ||
                callStack.contains("URLClassLoader") && !callStack.contains("loadClass") ||
                callStack.contains("DynamicProxyClass") ||
                callStack.contains("CGLIB") ||
                callStack.contains("asm.") ||
                callStack.contains("ByteBuddy") && !callStack.contains("janusguard"))) {
            return true;
        }
        
        return false;
    }
} 