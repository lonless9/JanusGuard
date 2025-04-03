package com.janusguard.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Agent配置管理类
 * 负责加载和解析配置文件
 */
public class AgentConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentConfig.class);
    
    private static final String DEFAULT_CONFIG_RESOURCE = "janusguard-default.yaml";
    private static final String CONFIG_PATH_KEY = "config";
    
    private Map<String, Object> configMap = new HashMap<>();
    private boolean initialized = false;
    
    /**
     * 初始化配置
     * 
     * @param agentArgs Agent启动参数
     */
    public void initialize(String agentArgs) {
        try {
            // 首先加载默认配置
            loadDefaultConfig();
            
            // 如果提供了命令行参数，尝试解析
            if (agentArgs != null && !agentArgs.trim().isEmpty()) {
                parseAgentArgs(agentArgs);
            }
            
            // 检查是否指定了外部配置文件
            String configPath = getConfigPath(agentArgs);
            if (configPath != null) {
                loadExternalConfig(configPath);
            }
            
            initialized = true;
            logger.info("Agent configuration initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize agent configuration", e);
            throw new RuntimeException("Configuration initialization failed", e);
        }
    }
    
    /**
     * 加载默认配置
     */
    private void loadDefaultConfig() {
        logger.info("Loading default configuration");
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (inputStream == null) {
                logger.warn("Default configuration file not found: {}", DEFAULT_CONFIG_RESOURCE);
                return;
            }
            
            Yaml yaml = new Yaml();
            configMap = yaml.load(inputStream);
            logger.debug("Loaded default configuration: {}", configMap);
        } catch (Exception e) {
            logger.error("Error loading default configuration", e);
            throw new RuntimeException("Failed to load default configuration", e);
        }
    }
    
    /**
     * 解析Agent启动参数
     * 
     * @param agentArgs 启动参数字符串
     */
    private void parseAgentArgs(String agentArgs) {
        logger.info("Parsing agent arguments: {}", agentArgs);
        Properties props = new Properties();
        
        for (String arg : agentArgs.split(",")) {
            String[] keyValue = arg.split("=", 2);
            if (keyValue.length == 2) {
                props.setProperty(keyValue[0].trim(), keyValue[1].trim());
                logger.debug("Parsed argument: {}={}", keyValue[0].trim(), keyValue[1].trim());
            } else {
                logger.warn("Invalid argument format: {}", arg);
            }
        }
        
        // 更新配置
        updateConfig(props);
    }
    
    /**
     * 获取配置文件路径
     * 
     * @param agentArgs 启动参数
     * @return 配置文件路径，如果未指定则返回null
     */
    private String getConfigPath(String agentArgs) {
        if (agentArgs == null || agentArgs.trim().isEmpty()) {
            return null;
        }
        
        for (String arg : agentArgs.split(",")) {
            String[] keyValue = arg.split("=", 2);
            if (keyValue.length == 2 && CONFIG_PATH_KEY.equals(keyValue[0].trim())) {
                return keyValue[1].trim();
            }
        }
        
        return null;
    }
    
    /**
     * 加载外部配置文件
     * 
     * @param configPath 配置文件路径
     */
    private void loadExternalConfig(String configPath) {
        logger.info("Loading external configuration from: {}", configPath);
        File configFile = Paths.get(configPath).toFile();
        
        if (!configFile.exists() || !configFile.isFile()) {
            logger.warn("External configuration file not found: {}", configPath);
            return;
        }
        
        try (InputStream inputStream = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> externalConfig = yaml.load(inputStream);
            
            // 合并配置
            mergeConfig(externalConfig);
            logger.info("External configuration loaded and merged successfully");
        } catch (Exception e) {
            logger.error("Error loading external configuration", e);
            throw new RuntimeException("Failed to load external configuration", e);
        }
    }
    
    /**
     * 更新配置
     * 
     * @param properties 要更新的属性
     */
    private void updateConfig(Properties properties) {
        for (String key : properties.stringPropertyNames()) {
            setNestedProperty(configMap, key, properties.getProperty(key));
        }
    }
    
    /**
     * 合并配置
     * 
     * @param newConfig 要合并的新配置
     */
    @SuppressWarnings("unchecked")
    private void mergeConfig(Map<String, Object> newConfig) {
        for (Map.Entry<String, Object> entry : newConfig.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map && configMap.containsKey(key) && configMap.get(key) instanceof Map) {
                // 递归合并子映射
                mergeConfig(
                    (Map<String, Object>) configMap.get(key),
                    (Map<String, Object>) value
                );
            } else {
                // 直接替换值
                configMap.put(key, value);
            }
        }
    }
    
    /**
     * 递归合并子配置
     * 
     * @param origMap 原始映射
     * @param newMap 新映射
     */
    @SuppressWarnings("unchecked")
    private void mergeConfig(Map<String, Object> origMap, Map<String, Object> newMap) {
        for (Map.Entry<String, Object> entry : newMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map && origMap.containsKey(key) && origMap.get(key) instanceof Map) {
                // 递归合并子映射
                mergeConfig(
                    (Map<String, Object>) origMap.get(key),
                    (Map<String, Object>) value
                );
            } else {
                // 直接替换值
                origMap.put(key, value);
            }
        }
    }
    
    /**
     * 设置嵌套属性
     * 
     * @param configMap 配置映射
     * @param key 属性键，可以是点分隔的嵌套路径
     * @param value 属性值
     */
    @SuppressWarnings("unchecked")
    private void setNestedProperty(Map<String, Object> configMap, String key, String value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = configMap;
        
        // 遍历路径直到倒数第二个部分
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            
            if (!current.containsKey(part) || !(current.get(part) instanceof Map)) {
                current.put(part, new HashMap<String, Object>());
            }
            
            current = (Map<String, Object>) current.get(part);
        }
        
        // 设置最后一个属性
        current.put(parts[parts.length - 1], value);
    }
    
    /**
     * 获取字符串配置项
     * 
     * @param path 配置路径，点分隔
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getString(String path, String defaultValue) {
        Object value = getNestedValue(path);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * 获取整数配置项
     * 
     * @param path 配置路径，点分隔
     * @param defaultValue 默认值
     * @return 配置值
     */
    public int getInt(String path, int defaultValue) {
        Object value = getNestedValue(path);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for path {}: {}", path, value);
            return defaultValue;
        }
    }
    
    /**
     * 获取布尔配置项
     * 
     * @param path 配置路径，点分隔
     * @param defaultValue 默认值
     * @return 配置值
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = getNestedValue(path);
        if (value == null) {
            return defaultValue;
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        return Boolean.parseBoolean(value.toString());
    }
    
    /**
     * 获取浮点数配置项
     * 
     * @param path 配置路径，点分隔
     * @param defaultValue 默认值
     * @return 配置值
     */
    public double getDouble(String path, double defaultValue) {
        Object value = getNestedValue(path);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Invalid double value for path {}: {}", path, value);
            return defaultValue;
        }
    }
    
    /**
     * 获取嵌套配置值
     * 
     * @param path 配置路径，点分隔
     * @return 配置值
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(String path) {
        if (!initialized) {
            logger.warn("Attempting to get configuration value before initialization");
            return null;
        }
        
        String[] parts = path.split("\\.");
        Map<String, Object> current = configMap;
        
        // 遍历路径直到倒数第二个部分
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            
            if (!current.containsKey(part) || !(current.get(part) instanceof Map)) {
                return null;
            }
            
            current = (Map<String, Object>) current.get(part);
        }
        
        // 获取最后一个属性
        return current.get(parts[parts.length - 1]);
    }
    
    /**
     * 获取整个配置映射
     * 
     * @return 配置映射
     */
    public Map<String, Object> getConfigMap() {
        return new HashMap<>(configMap);
    }
} 