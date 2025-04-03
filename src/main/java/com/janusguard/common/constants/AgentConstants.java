package com.janusguard.common.constants;

/**
 * Agent常量类
 * 定义Agent使用的常量
 */
public final class AgentConstants {
    
    // 禁止实例化
    private AgentConstants() {
    }
    
    /**
     * Agent名称
     */
    public static final String AGENT_NAME = "JanusGuard";
    
    /**
     * Agent版本
     */
    public static final String AGENT_VERSION = "1.0.0";
    
    /**
     * 默认配置文件路径
     */
    public static final String DEFAULT_CONFIG_FILE = "janusguard-default.yaml";
    
    /**
     * 默认日志路径
     */
    public static final String DEFAULT_LOG_PATH = "./logs/janusguard.log";
    
    /**
     * 事件类型相关常量
     */
    public static final class EventTypes {
        // 命令执行
        public static final String COMMAND_EXECUTION = "command_execution";
        
        // 文件操作
        public static final String FILE_OPERATION = "file_operation";
        
        // 反射调用
        public static final String REFLECTION = "reflection";
        
        // 类加载
        public static final String CLASS_LOADING = "class_loading";
        
        // 网络操作
        public static final String NETWORK_OPERATION = "network_operation";
    }
    
    /**
     * 事件数据字段名常量
     */
    public static final class EventFields {
        // 通用字段
        public static final String EVENT_ID = "event_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String EVENT_TYPE = "event_type";
        public static final String CLASS_NAME = "class_name";
        public static final String METHOD_NAME = "method_name";
        public static final String THREAD_NAME = "thread_name";
        public static final String THREAD_ID = "thread_id";
        
        // 命令执行字段
        public static final String COMMAND = "command";
        public static final String COMMAND_ARGS = "command_args";
        
        // 文件操作字段
        public static final String FILE_PATH = "file_path";
        public static final String FILE_OPERATION = "file_operation";
        
        // 反射字段
        public static final String TARGET_CLASS = "target_class";
        public static final String TARGET_METHOD = "target_method";
        
        // 网络字段
        public static final String REMOTE_HOST = "remote_host";
        public static final String REMOTE_PORT = "remote_port";
        public static final String PROTOCOL = "protocol";
    }
    
    /**
     * 配置键常量
     */
    public static final class ConfigKeys {
        // Agent配置
        public static final String AGENT_ENABLED = "agent.enabled";
        public static final String AGENT_LOG_LEVEL = "agent.log-level";
        
        // 转换器配置
        public static final String TRANSFORMER_CACHE_SIZE = "transformer.cache-size";
        public static final String TRANSFORMER_EXCLUDED_PACKAGES = "transformer.excluded-packages";
        public static final String TRANSFORMER_INCLUDED_PACKAGES = "transformer.included-packages";
        
        // 监控点配置
        public static final String MONITOR_COMMAND_EXECUTION_ENABLED = "monitors.command-execution.enabled";
        public static final String MONITOR_FILE_OPERATIONS_ENABLED = "monitors.file-operations.enabled";
        public static final String MONITOR_REFLECTION_ENABLED = "monitors.reflection.enabled";
        
        // 事件处理配置
        public static final String EVENT_QUEUE_SIZE = "event-processing.queue-size";
        public static final String EVENT_BATCH_SIZE = "event-processing.batch-size";
        public static final String EVENT_FLUSH_INTERVAL = "event-processing.flush-interval-ms";
        
        // 上报配置
        public static final String REPORTING_ENABLED = "reporting.enabled";
        public static final String REPORTING_MODE = "reporting.mode";
        public static final String REPORTING_FILE_PATH = "reporting.file.path";
    }
} 