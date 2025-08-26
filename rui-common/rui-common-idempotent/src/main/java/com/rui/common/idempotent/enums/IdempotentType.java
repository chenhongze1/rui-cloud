package com.rui.common.idempotent.enums;

/**
 * 幂等性类型
 * 
 * @author rui
 */
public enum IdempotentType {
    
    /**
     * SpEL表达式
     */
    SPEL,
    
    /**
     * 请求头
     */
    HEADER,
    
    /**
     * 请求参数
     */
    PARAM,
    
    /**
     * 方法参数
     */
    METHOD_PARAM,
    
    /**
     * 用户ID
     */
    USER_ID,
    
    /**
     * IP地址
     */
    IP,
    
    /**
     * Token
     */
    TOKEN
}