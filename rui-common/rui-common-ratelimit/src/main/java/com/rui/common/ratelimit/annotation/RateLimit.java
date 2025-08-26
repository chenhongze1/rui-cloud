package com.rui.common.ratelimit.annotation;

import com.rui.common.ratelimit.enums.LimitType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 * 
 * @author rui
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 限流key
     */
    String key() default "";
    
    /**
     * 限流时间,单位秒
     */
    int time() default 60;
    
    /**
     * 限流时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 限流次数
     */
    int count() default 100;
    
    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;
    
    /**
     * 提示消息
     */
    String message() default "访问过于频繁，请稍候再试";
    
    /**
     * 是否抛出异常
     */
    boolean throwException() default true;
}