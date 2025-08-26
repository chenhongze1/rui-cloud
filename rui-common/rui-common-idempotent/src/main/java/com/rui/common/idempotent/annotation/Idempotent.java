package com.rui.common.idempotent.annotation;

import com.rui.common.idempotent.enums.IdempotentType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性注解
 * 
 * @author rui
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    
    /**
     * 幂等性key
     * 支持SpEL表达式
     */
    String key() default "";
    
    /**
     * 幂等性类型
     */
    IdempotentType type() default IdempotentType.SPEL;
    
    /**
     * 过期时间
     */
    long expireTime() default 300;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 提示信息
     */
    String message() default "请勿重复操作";
    
    /**
     * 是否删除key
     * true: 执行完成后删除key
     * false: 等待key过期
     */
    boolean delKey() default false;
}