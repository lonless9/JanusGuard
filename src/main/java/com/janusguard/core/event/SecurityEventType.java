package com.janusguard.core.event;

/**
 * 安全事件类型枚举
 * 定义可能的事件类型
 */
public enum SecurityEventType {
    
    /**
     * 命令执行，例如Runtime.exec或ProcessBuilder执行
     */
    COMMAND_EXECUTION,
    
    /**
     * 反射调用，例如Method.invoke
     */
    REFLECTION,
    
    /**
     * 类加载，例如ClassLoader.defineClass
     */
    CLASS_LOADING,
    
    /**
     * 文件操作，例如读写文件
     */
    FILE_OPERATION,
    
    /**
     * 网络操作，例如建立连接
     */
    NETWORK_OPERATION,
    
    /**
     * SQL执行，例如JDBC查询
     */
    SQL_EXECUTION,
    
    /**
     * 序列化，例如readObject/writeObject
     */
    SERIALIZATION,
    
    /**
     * 加密操作，例如密码处理
     */
    CRYPTO_OPERATION,
    
    /**
     * 系统属性访问
     */
    SYSTEM_PROPERTY_ACCESS,
    
    /**
     * JNI操作，例如本地库加载
     */
    JNI_OPERATION,
    
    /**
     * JVM内存操作，例如Unsafe类操作
     */
    JVM_MEMORY_OPERATION,
    
    /**
     * 动态代理创建
     */
    DYNAMIC_PROXY,
    
    /**
     * 未知类型
     */
    UNKNOWN
} 