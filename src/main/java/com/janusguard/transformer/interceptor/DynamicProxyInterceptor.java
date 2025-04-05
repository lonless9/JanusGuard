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
 * 动态代理拦截器
 * 监控java.lang.reflect.Proxy的动态代理创建
 */
public class DynamicProxyInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicProxyInterceptor.class);
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
        String callStack = LogUtils.getStackTrace(2, 12);
        
        // 创建安全事件
        SecurityEvent event = new SecurityEvent(SecurityEventType.DYNAMIC_PROXY, className, methodName);
        event.setSeverity(SecurityEventSeverity.MEDIUM);
        
        // 添加动态代理创建信息
        if (args.length > 1 && args[0] instanceof ClassLoader && args[1] instanceof Class[]) {
            ClassLoader loader = (ClassLoader) args[0];
            Class<?>[] interfaces = (Class<?>[]) args[1];
            
            event.addData("classLoader", loader.getClass().getName());
            
            StringBuilder ifaceNames = new StringBuilder();
            for (Class<?> iface : interfaces) {
                if (ifaceNames.length() > 0) {
                    ifaceNames.append(", ");
                }
                ifaceNames.append(iface.getName());
            }
            event.addData("interfaces", ifaceNames.toString());
            
            // 检测可疑的代理创建
            if (isSuspiciousProxyCreation(callStack, interfaces)) {
                event.addData("suspiciousProxy", true);
                event.addData("reason", "可疑动态代理创建");
                logger.warn("检测到可疑动态代理创建, 接口: {}", ifaceNames);
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
            
            if (result != null) {
                event.addData("proxyClass", result.getClass().getName());
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
                    logger.error("处理动态代理事件失败", e);
                }
            }
        }
    }
    
    /**
     * 判断是否为可疑的代理创建
     * 
     * @param callStack 调用堆栈
     * @param interfaces 代理接口
     * @return 如果可疑则返回true
     */
    private static boolean isSuspiciousProxyCreation(String callStack, Class<?>[] interfaces) {
        // 检查是否包含敏感接口
        for (Class<?> iface : interfaces) {
            String name = iface.getName();
            if (name.contains("java.rmi") || 
                name.startsWith("javax.management") ||
                name.contains("Instruction") ||
                name.contains("ClassLoader")) {
                return true;
            }
        }
        
        // 检查调用链是否可疑
        return callStack != null && (
                callStack.contains("exploit") ||
                callStack.contains("payload") ||
                callStack.contains("gadget") ||
                callStack.contains("deserialize"));
    }
} 