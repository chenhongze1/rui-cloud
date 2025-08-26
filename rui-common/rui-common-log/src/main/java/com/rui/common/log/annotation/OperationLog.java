package com.rui.common.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 
 * @author rui
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    
    /**
     * 操作模块
     */
    String module() default "";
    
    /**
     * 操作类型
     */
    String type() default "";
    
    /**
     * 操作描述
     */
    String description() default "";
    
    /**
     * 是否记录请求参数
     */
    boolean includeArgs() default true;
    
    /**
     * 是否记录返回结果
     */
    boolean includeResult() default true;
    
    /**
     * 是否记录异常信息
     */
    boolean includeException() default true;
    
    /**
     * 是否保存到数据库
     */
    boolean saveToDatabase() default false;
    
    /**
     * 业务ID的SpEL表达式
     */
    String businessId() default "";
    
    /**
     * 操作人的SpEL表达式
     */
    String operator() default "";
}