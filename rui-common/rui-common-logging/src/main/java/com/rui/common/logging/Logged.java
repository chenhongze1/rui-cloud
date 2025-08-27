package com.rui.common.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志记录注解
 * 用于标记需要自动记录日志的方法
 *
 * @author rui
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Logged {

    /**
     * 日志类型
     */
    LogType type() default LogType.BUSINESS;

    /**
     * 操作描述
     */
    String operation() default "";

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 是否记录参数
     */
    boolean logParameters() default false;

    /**
     * 是否记录返回值
     */
    boolean logReturnValue() default false;

    /**
     * 是否记录执行时间
     */
    boolean logExecutionTime() default true;

    /**
     * 是否记录异常
     */
    boolean logException() default true;

    /**
     * 慢操作阈值（毫秒）
     */
    long slowThreshold() default 1000;

    /**
     * 日志级别
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * 自定义属性
     */
    String[] attributes() default {};

    /**
     * 是否记录敏感信息
     */
    boolean includeSensitive() default false;

    /**
     * 日志类型枚举
     */
    enum LogType {
        /**
         * 业务日志
         */
        BUSINESS,
        
        /**
         * 审计日志
         */
        AUDIT,
        
        /**
         * 性能日志
         */
        PERFORMANCE,
        
        /**
         * 安全日志
         */
        SECURITY,
        
        /**
         * 普通日志
         */
        GENERAL
    }

    /**
     * 日志级别枚举
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}