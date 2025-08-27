package com.rui.common.tracing.annotation;

import io.opentelemetry.api.trace.SpanKind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 链路追踪注解
 * 用于标记需要自动追踪的方法
 *
 * @author rui
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Traced {

    /**
     * Span名称，如果为空则使用方法名
     */
    String value() default "";

    /**
     * Span类型
     */
    SpanKind kind() default SpanKind.INTERNAL;

    /**
     * 是否记录参数
     */
    boolean recordParameters() default false;

    /**
     * 是否记录返回值
     */
    boolean recordReturnValue() default false;

    /**
     * 是否记录异常
     */
    boolean recordException() default true;

    /**
     * 自定义属性
     */
    String[] attributes() default {};

    /**
     * 操作类型（用于业务分类）
     */
    String operationType() default "";

    /**
     * 服务名称（用于业务分类）
     */
    String serviceName() default "";
}