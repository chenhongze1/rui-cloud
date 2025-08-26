package com.rui.common.ratelimit.service;

import java.util.concurrent.TimeUnit;

/**
 * 限流服务接口
 * 
 * @author rui
 */
public interface RateLimitService {
    
    /**
     * 尝试获取令牌
     * 
     * @param key 限流key
     * @param count 限流次数
     * @param time 限流时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    boolean tryAcquire(String key, int count, int time, TimeUnit timeUnit);
    
    /**
     * 获取剩余次数
     * 
     * @param key 限流key
     * @return 剩余次数
     */
    long getRemaining(String key);
    
    /**
     * 获取重置时间
     * 
     * @param key 限流key
     * @return 重置时间（秒）
     */
    long getResetTime(String key);
    
    /**
     * 清除限流记录
     * 
     * @param key 限流key
     */
    void clear(String key);
    
    /**
     * 检查是否被限流
     * 
     * @param key 限流key
     * @return 是否被限流
     */
    boolean isLimited(String key);
}