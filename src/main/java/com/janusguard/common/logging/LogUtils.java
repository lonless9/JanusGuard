package com.janusguard.common.logging;

import org.slf4j.Logger;

/**
 * 日志工具类
 * 提供一些常用的日志操作方法
 */
public final class LogUtils {
    
    // 禁止实例化
    private LogUtils() {
    }
    
    /**
     * 记录debug级别日志
     * 
     * @param logger 日志记录器
     * @param message 日志消息
     */
    public static void debug(Logger logger, String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }
    
    /**
     * 记录带异常的debug级别日志
     * 
     * @param logger 日志记录器
     * @param message 日志消息
     * @param t 异常
     */
    public static void debug(Logger logger, String message, Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, t);
        }
    }
    
    /**
     * 记录带参数的debug级别日志
     * 
     * @param logger 日志记录器
     * @param format 格式化字符串
     * @param args 参数
     */
    public static void debug(Logger logger, String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, args);
        }
    }
    
    /**
     * 记录info级别日志
     * 
     * @param logger 日志记录器
     * @param message 日志消息
     */
    public static void info(Logger logger, String message) {
        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }
    
    /**
     * 记录带异常的info级别日志
     * 
     * @param logger 日志记录器
     * @param message 日志消息
     * @param t 异常
     */
    public static void info(Logger logger, String message, Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.info(message, t);
        }
    }
    
    /**
     * 记录带参数的info级别日志
     * 
     * @param logger 日志记录器
     * @param format 格式化字符串
     * @param args 参数
     */
    public static void info(Logger logger, String format, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(format, args);
        }
    }
    
    /**
     * 记录warn级别日志
     * 
     * @param logger 日志记录器
     * @param message 日志消息
     */
    public static void warn(Logger logger, String message) {
        logger.warn(message);
    }
    
    /**
     * 记录带异常的warn级别日志
     * 
     * @param logger 日志记录器
     * @param message 日志消息
     * @param t 异常
     */
    public static void warn(Logger logger, String message, Throwable t) {
        logger.warn(message, t);
    }
    
    /**
     * 记录带参数的warn级别日志
     * 
     * @param logger 日志记录器
     * @param format 格式化字符串
     * @param args 参数
     */
    public static void warn(Logger logger, String format, Object... args) {
        logger.warn(format, args);
    }
    
    /**
     * 记录error级别日志
     * 
     * @param logger 日志记录器
     * @param message 日志消息
     */
    public static void error(Logger logger, String message) {
        logger.error(message);
    }
    
    /**
     * 记录带异常的error级别日志
     * 
     * @param logger 日志记录器
     * @param message 日志消息
     * @param t 异常
     */
    public static void error(Logger logger, String message, Throwable t) {
        logger.error(message, t);
    }
    
    /**
     * 记录带参数的error级别日志
     * 
     * @param logger 日志记录器
     * @param format 格式化字符串
     * @param args 参数
     */
    public static void error(Logger logger, String format, Object... args) {
        logger.error(format, args);
    }
    
    /**
     * 获取当前线程的调用堆栈字符串
     * 
     * @param skipDepth 要跳过的栈帧深度
     * @param maxDepth 最大栈帧深度
     * @return 格式化后的调用堆栈字符串
     */
    public static String getStackTrace(int skipDepth, int maxDepth) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        
        int startIndex = Math.min(skipDepth, stackTrace.length - 1);
        int endIndex = Math.min(startIndex + maxDepth, stackTrace.length);
        
        for (int i = startIndex; i < endIndex; i++) {
            StackTraceElement element = stackTrace[i];
            sb.append("\n    at ")
              .append(element.getClassName())
              .append(".")
              .append(element.getMethodName())
              .append("(")
              .append(element.getFileName())
              .append(":")
              .append(element.getLineNumber())
              .append(")");
        }
        
        return sb.toString();
    }
} 