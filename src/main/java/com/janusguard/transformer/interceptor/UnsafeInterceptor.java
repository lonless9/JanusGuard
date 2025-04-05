package com.janusguard.transformer.interceptor;

import java.lang.reflect.Method;
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
 * Unsafe操作拦截器
 * 监控sun.misc.Unsafe内存操作，用于检测内存木马注入
 */
public class UnsafeInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(UnsafeInterceptor.class);
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
        String callStack = LogUtils.getStackTrace(2, 15);
        
        // 创建安全事件
        SecurityEvent event = new SecurityEvent(SecurityEventType.JVM_MEMORY_OPERATION, className, methodName);
        event.setSeverity(SecurityEventSeverity.HIGH);
        
        // 添加Unsafe方法信息
        event.addData("unsafeMethod", methodName);
        if (args.length > 0) {
            event.addData("firstArgType", args[0] != null ? args[0].getClass().getName() : "null");
            
            // 检测特定的危险方法
            if (isDangerousUnsafeMethod(methodName)) {
                event.addData("dangerousOperation", true);
                event.addData("reason", "高危Unsafe内存操作");
                logger.warn("检测到高危Unsafe操作: {}", methodName);
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
                    logger.error("处理Unsafe操作事件失败", e);
                }
            }
        }
    }
    
    /**
     * 判断是否为危险的Unsafe方法
     * 
     * @param methodName 方法名
     * @return 如果是危险方法则返回true
     */
    private static boolean isDangerousUnsafeMethod(String methodName) {
        return methodName.equals("putAddress") || 
               methodName.equals("putObject") || 
               methodName.startsWith("defineClass") || 
               methodName.equals("allocateInstance") ||
               methodName.equals("defineAnonymousClass");
    }
} 