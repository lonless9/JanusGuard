package com.janusguard.core.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 安全事件类
 * 表示一次安全监控捕获的事件
 */
public class SecurityEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final long timestamp;
    private long processedTimestamp;
    private final SecurityEventType type;
    private SecurityEventSeverity severity;
    private final String className;
    private final String methodName;
    private final String threadName;
    private final long threadId;
    private String callStackTrace;
    private final Map<String, Object> data;
    
    /**
     * 构造函数
     * 
     * @param type 事件类型
     * @param className 触发事件的类名
     * @param methodName 触发事件的方法名
     */
    public SecurityEvent(SecurityEventType type, String className, String methodName) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.severity = SecurityEventSeverity.UNKNOWN;
        this.className = className;
        this.methodName = methodName;
        
        Thread currentThread = Thread.currentThread();
        this.threadName = currentThread.getName();
        this.threadId = currentThread.getId();
        
        this.data = new HashMap<>();
    }
    
    /**
     * 添加事件数据
     * 
     * @param key 数据键
     * @param value 数据值
     */
    public void addData(String key, Object value) {
        if (key != null) {
            this.data.put(key, value);
        }
    }
    
    /**
     * 设置调用堆栈
     * 
     * @param callStackTrace 调用堆栈
     */
    public void setCallStackTrace(String callStackTrace) {
        this.callStackTrace = callStackTrace;
    }
    
    /**
     * 设置事件处理时间戳
     * 
     * @param processedTimestamp 处理时间戳
     */
    public void setProcessedTimestamp(long processedTimestamp) {
        this.processedTimestamp = processedTimestamp;
    }
    
    /**
     * 设置事件严重级别
     * 
     * @param severity 严重级别
     */
    public void setSeverity(SecurityEventSeverity severity) {
        this.severity = severity;
    }
    
    /**
     * 获取事件ID
     * 
     * @return 事件ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取事件时间戳
     * 
     * @return 事件时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取事件处理时间戳
     * 
     * @return 事件处理时间戳
     */
    public long getProcessedTimestamp() {
        return processedTimestamp;
    }
    
    /**
     * 获取事件类型
     * 
     * @return 事件类型
     */
    public SecurityEventType getType() {
        return type;
    }
    
    /**
     * 获取事件严重级别
     * 
     * @return 事件严重级别
     */
    public SecurityEventSeverity getSeverity() {
        return severity;
    }
    
    /**
     * 获取类名
     * 
     * @return 类名
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * 获取方法名
     * 
     * @return 方法名
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * 获取线程名
     * 
     * @return 线程名
     */
    public String getThreadName() {
        return threadName;
    }
    
    /**
     * 获取线程ID
     * 
     * @return 线程ID
     */
    public long getThreadId() {
        return threadId;
    }
    
    /**
     * 获取调用堆栈
     * 
     * @return 调用堆栈
     */
    public String getCallStackTrace() {
        return callStackTrace;
    }
    
    /**
     * 获取事件数据
     * 
     * @return 事件数据
     */
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }
    
    /**
     * 获取特定事件数据
     * 
     * @param key 数据键
     * @return 数据值
     */
    public Object getData(String key) {
        return data.get(key);
    }
    
    @Override
    public String toString() {
        return "SecurityEvent{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", severity=" + severity +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", threadName='" + threadName + '\'' +
                ", timestamp=" + timestamp +
                ", dataKeys=" + data.keySet() +
                '}';
    }
} 