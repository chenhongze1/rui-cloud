package com.rui.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 简单日志工具类
 * 
 * @author rui
 * @since 1.0.0
 */
public class LogUtil {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 获取Logger实例
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * 记录信息日志
     */
    public static void info(Class<?> clazz, String message) {
        getLogger(clazz).info(message);
    }
    
    /**
     * 记录信息日志（带参数）
     */
    public static void info(Class<?> clazz, String message, Object... args) {
        getLogger(clazz).info(message, args);
    }
    
    /**
     * 记录错误日志
     */
    public static void error(Class<?> clazz, String message) {
        getLogger(clazz).error(message);
    }
    
    /**
     * 记录错误日志（带异常）
     */
    public static void error(Class<?> clazz, String message, Throwable throwable) {
        getLogger(clazz).error(message, throwable);
    }
    
    /**
     * 记录警告日志
     */
    public static void warn(Class<?> clazz, String message) {
        getLogger(clazz).warn(message);
    }
    
    /**
     * 记录调试日志
     */
    public static void debug(Class<?> clazz, String message) {
        getLogger(clazz).debug(message);
    }
    
    /**
     * 设置追踪ID
     */
    public static void setTraceId(String traceId) {
        MDC.put("traceId", traceId);
    }
    
    /**
     * 生成并设置追踪ID
     */
    public static String generateAndSetTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        setTraceId(traceId);
        return traceId;
    }
    
    /**
     * 获取追踪ID
     */
    public static String getTraceId() {
        return MDC.get("traceId");
    }
    
    /**
     * 清除追踪ID
     */
    public static void clearTraceId() {
        MDC.remove("traceId");
    }
    
    /**
     * 清除所有MDC
     */
    public static void clearMDC() {
        MDC.clear();
    }
    
    /**
     * 记录业务日志
     */
    public static void business(Class<?> clazz, String operation, String message) {
        Logger logger = getLogger(clazz);
        String timestamp = LocalDateTime.now().format(FORMATTER);
        logger.info("[BUSINESS] [{}] [{}] {}", timestamp, operation, message);
    }
    
    /**
     * 记录性能日志
     */
    public static void performance(Class<?> clazz, String method, long duration) {
        Logger logger = getLogger(clazz);
        logger.info("[PERFORMANCE] Method: {}, Duration: {}ms", method, duration);
    }
}