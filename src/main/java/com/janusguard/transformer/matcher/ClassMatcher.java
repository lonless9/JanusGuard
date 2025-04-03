package com.janusguard.transformer.matcher;

/**
 * 类匹配器接口
 * 用于判断给定的类是否应该被转换
 */
public interface ClassMatcher {
    
    /**
     * 判断类是否匹配
     * 
     * @param className 类名
     * @param classLoader 类加载器
     * @return 如果类匹配则返回true
     */
    boolean matches(String className, ClassLoader classLoader);
} 