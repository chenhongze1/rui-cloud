package com.rui.common.lock.annotation;

import com.rui.common.lock.enums.LockType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * 
 * @author rui
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    
    /**
     * 锁的名称
     */
    String name() default "";
    
    /**
     * 锁的key，支持SpEL表达式
     */
    String key() default "";
    
    /**
     * 锁类型
     */
    LockType lockType() default LockType.REENTRANT_LOCK;
    
    /**
     * 尝试加锁，最多等待时间
     */
    long waitTime() default -1;
    
    /**
     * 上锁以后xxx秒自动解锁
     */
    long leaseTime() default -1;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 加锁失败的提示信息
     */
    String failMessage() default "系统繁忙，请稍后再试";
    
    /**
     * 是否抛出异常，false则返回null
     */
    boolean throwException() default true;
}