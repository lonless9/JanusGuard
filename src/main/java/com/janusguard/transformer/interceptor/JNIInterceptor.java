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
 * JNI操作拦截器
 * 监控System.load/loadLibrary等JNI相关操作
 */
public class JNIInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(JNIInterceptor.class);
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
        SecurityEvent event = new SecurityEvent(SecurityEventType.JNI_OPERATION, className, methodName);
        event.setSeverity(SecurityEventSeverity.HIGH);
        
        // 添加JNI库加载信息
        if (args.length > 0 && args[0] instanceof String) {
            String libraryPath = (String) args[0];
            event.addData("libraryPath", libraryPath);
            
            // 检测可疑的JNI库加载
            if (isSuspiciousJNILoading(libraryPath, callStack)) {
                event.addData("suspiciousJNI", true);
                event.addData("reason", "可疑JNI库加载");
                logger.warn("检测到可疑JNI库加载: {}", libraryPath);
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
                    logger.error("处理JNI操作事件失败", e);
                }
            }
        }
    }
    
    /**
     * 判断是否为可疑的JNI库加载
     * 
     * @param libraryPath 库路径
     * @param callStack 调用堆栈
     * @return 如果可疑则返回true
     */
    private static boolean isSuspiciousJNILoading(String libraryPath, String callStack) {
        // 检测临时目录中的库
        if (libraryPath.contains("/tmp/") || 
            libraryPath.contains("\\Temp\\") || 
            libraryPath.contains("临时")) {
            return true;
        }
        
        // 检测可疑的文件名
        if (libraryPath.matches(".*[0-9a-f]{8,}.*") || 
            libraryPath.contains("hack") ||
            libraryPath.contains("exploit")) {
            return true;
        }
        
        // 检查调用链
        return callStack != null && (
                callStack.contains("reflect.") ||
                callStack.contains("URLClassLoader") ||
                callStack.contains("ScriptEngine") ||
                callStack.contains("eval"));
    }
} 