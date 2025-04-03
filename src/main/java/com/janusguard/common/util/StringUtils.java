package com.janusguard.common.util;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 * 提供一些常用的字符串操作方法
 */
public final class StringUtils {
    
    // 禁止实例化
    private StringUtils() {
    }
    
    /**
     * 检查字符串是否为空或null
     * 
     * @param str 要检查的字符串
     * @return 如果字符串为null或空则返回true
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * 检查字符串是否不为空且不为null
     * 
     * @param str 要检查的字符串
     * @return 如果字符串不为null且不为空则返回true
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 检查字符串是否为空白、空或null
     * 
     * @param str 要检查的字符串
     * @return 如果字符串为null、空或仅包含空白字符则返回true
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 检查字符串是否不为空白、不为空且不为null
     * 
     * @param str 要检查的字符串
     * @return 如果字符串不为null、不为空且不仅包含空白字符则返回true
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * 将字符串数组转换为逗号分隔的字符串
     * 
     * @param array 字符串数组
     * @return 逗号分隔的字符串
     */
    public static String join(String[] array) {
        return join(array, ",");
    }
    
    /**
     * 将字符串数组转换为指定分隔符分隔的字符串
     * 
     * @param array 字符串数组
     * @param delimiter 分隔符
     * @return 指定分隔符分隔的字符串
     */
    public static String join(String[] array, String delimiter) {
        if (array == null || array.length == 0) {
            return "";
        }
        
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            if (array[i] != null) {
                builder.append(array[i]);
            }
        }
        
        return builder.toString();
    }
    
    /**
     * 将集合转换为逗号分隔的字符串
     * 
     * @param collection 集合
     * @return 逗号分隔的字符串
     */
    public static String join(Collection<?> collection) {
        return join(collection, ",");
    }
    
    /**
     * 将集合转换为指定分隔符分隔的字符串
     * 
     * @param collection 集合
     * @param delimiter 分隔符
     * @return 指定分隔符分隔的字符串
     */
    public static String join(Collection<?> collection, String delimiter) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        
        for (Object item : collection) {
            if (!first) {
                builder.append(delimiter);
            }
            if (item != null) {
                builder.append(item.toString());
            }
            first = false;
        }
        
        return builder.toString();
    }
    
    /**
     * 获取字符串的非null值
     * 
     * @param str 可能为null的字符串
     * @return 原字符串或空字符串（如果为null）
     */
    public static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }
    
    /**
     * 安全截取字符串
     * 
     * @param str 要截取的字符串
     * @param maxLength 最大长度
     * @return 截取后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        
        if (str.length() <= maxLength) {
            return str;
        }
        
        return str.substring(0, maxLength) + "...";
    }
    
    /**
     * 判断字符串是否匹配给定的正则表达式模式
     * 
     * @param str 要检查的字符串
     * @param regex 正则表达式模式
     * @return 如果匹配则返回true
     */
    public static boolean matches(String str, String regex) {
        if (str == null || regex == null) {
            return false;
        }
        
        return Pattern.matches(regex, str);
    }
    
    /**
     * 将驼峰式命名的字符串转换为下划线命名的字符串
     * 
     * @param camelCase 驼峰式命名的字符串
     * @return 下划线命名的字符串
     */
    public static String camelToSnake(String camelCase) {
        if (camelCase == null) {
            return null;
        }
        
        String regex = "([a-z])([A-Z])";
        String replacement = "$1_$2";
        
        return camelCase.replaceAll(regex, replacement).toLowerCase();
    }
    
    /**
     * 将下划线命名的字符串转换为驼峰式命名的字符串
     * 
     * @param snakeCase 下划线命名的字符串
     * @return 驼峰式命名的字符串
     */
    public static String snakeToCamel(String snakeCase) {
        if (snakeCase == null) {
            return null;
        }
        
        StringBuilder builder = new StringBuilder();
        boolean nextUpper = false;
        
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    builder.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    builder.append(c);
                }
            }
        }
        
        return builder.toString();
    }
} 