package com.janusguard.core.event;

/**
 * 安全事件严重级别枚举
 * 定义事件的严重程度
 */
public enum SecurityEventSeverity {
    
    /**
     * 高严重级别，表示严重安全威胁
     */
    HIGH(3),
    
    /**
     * 中严重级别，表示潜在安全风险
     */
    MEDIUM(2),
    
    /**
     * 低严重级别，表示需要注意但风险较低
     */
    LOW(1),
    
    /**
     * 信息级别，仅用于记录
     */
    INFO(0),
    
    /**
     * 未知级别，表示尚未分类
     */
    UNKNOWN(-1);
    
    private final int level;
    
    /**
     * 构造函数
     * 
     * @param level 严重级别的数值表示
     */
    SecurityEventSeverity(int level) {
        this.level = level;
    }
    
    /**
     * 获取严重级别的数值表示
     * 
     * @return 严重级别值
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * 判断是否为高于或等于指定级别
     * 
     * @param other 要比较的级别
     * @return 如果当前级别高于或等于指定级别则返回true
     */
    public boolean isAtLeast(SecurityEventSeverity other) {
        return this.level >= other.level;
    }
} 