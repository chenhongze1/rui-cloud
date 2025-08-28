package com.rui.common.monitoring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 性能监控注解
 * 用于标记需要进行性能监控的方法
 * 整合原log模块的性能监控功能到monitoring模块
 *
 * @author rui
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PerformanceMonitored {

    /**
     * 操作描述
     */
    String operation() default "";

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 慢操作阈值（毫秒）
     * 0表示使用全局配置
     */
    long slowThreshold() default 0;

    /**
     * 是否包含方法参数信息
     */
    boolean includeParameters() default false;

    /**
     * 是否启用详细监控
     * 包括内存使用、线程信息等
     */
    boolean detailedMonitoring() default false;

    /**
     * 自定义标签
     */
    String[] tags() default {};

    /**
     * 监控级别
     */
    MonitoringLevel level() default MonitoringLevel.NORMAL;

    /**
     * 监控级别枚举
     */
    enum MonitoringLevel {
        /**
         * 基础监控：只记录执行时间和成功/失败状态
         */
        BASIC,
        
        /**
         * 普通监控：记录执行时间、成功/失败状态、慢操作检测
         */
        NORMAL,
        
        /**
         * 详细监控：记录所有信息，包括内存使用、线程信息等
         */
        DETAILED
    }
}