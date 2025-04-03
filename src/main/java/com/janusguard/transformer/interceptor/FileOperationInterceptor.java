package com.janusguard.transformer.interceptor;

import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.janusguard.common.constants.AgentConstants;
import com.janusguard.common.logging.LogUtils;
import com.janusguard.core.event.EventProcessor;
import com.janusguard.core.event.SecurityEvent;
import com.janusguard.core.event.SecurityEventType;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * 文件操作拦截器
 * 拦截FileInputStream, FileOutputStream等文件操作
 */
public class FileOperationInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(FileOperationInterceptor.class);
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
        String callStack = LogUtils.getStackTrace(2, 10); // 跳过拦截器框架的堆栈
        
        // 创建安全事件
        SecurityEvent event = new SecurityEvent(SecurityEventType.FILE_OPERATION, className, methodName);
        
        // 添加文件信息
        if (methodName.equals("<init>")) {
            // 构造函数通常接收文件路径或File对象
            if (args.length > 0) {
                if (args[0] instanceof String) {
                    event.addData(AgentConstants.EventFields.FILE_PATH, args[0]);
                    event.addData(AgentConstants.EventFields.FILE_OPERATION, "open");
                } else if (args[0] instanceof File) {
                    event.addData(AgentConstants.EventFields.FILE_PATH, ((File) args[0]).getAbsolutePath());
                    event.addData(AgentConstants.EventFields.FILE_OPERATION, "open");
                }
            }
        } else if (methodName.equals("read") || methodName.equals("readLine")) {
            // 读取操作
            event.addData(AgentConstants.EventFields.FILE_OPERATION, "read");
        } else if (methodName.equals("write")) {
            // 写入操作
            event.addData(AgentConstants.EventFields.FILE_OPERATION, "write");
        } else if (methodName.equals("delete")) {
            // 删除操作
            event.addData(AgentConstants.EventFields.FILE_OPERATION, "delete");
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
                    logger.error("Failed to process file operation event", e);
                }
            }
        }
    }
} 