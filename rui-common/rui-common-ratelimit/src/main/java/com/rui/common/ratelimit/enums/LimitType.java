package com.rui.common.ratelimit.enums;

/**
 * 限流类型
 * 
 * @author rui
 */
public enum LimitType {
    
    /**
     * 默认策略全局限流
     */
    DEFAULT,
    
    /**
     * 根据请求者IP进行限流
     */
    IP,
    
    /**
     * 根据用户ID进行限流
     */
    USER,
    
    /**
     * 根据方法名进行限流
     */
    METHOD,
    
    /**
     * 根据参数进行限流
     */
    PARAM,
    
    /**
     * 自定义限流（根据key）
     */
    CUSTOM
}