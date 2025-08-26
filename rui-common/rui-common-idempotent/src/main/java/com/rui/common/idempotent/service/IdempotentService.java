package com.rui.common.idempotent.service;

import java.util.concurrent.TimeUnit;

/**
 * 幂等性服务接口
 * 
 * @author rui
 */
public interface IdempotentService {
    
    /**
     * 检查是否重复请求
     * 
     * @param key 幂等性key
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return true: 重复请求, false: 非重复请求
     */
    boolean isDuplicate(String key, long expireTime, TimeUnit timeUnit);
    
    /**
     * 设置幂等性标识
     * 
     * @param key 幂等性key
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return 是否设置成功
     */
    boolean setIdempotent(String key, long expireTime, TimeUnit timeUnit);
    
    /**
     * 删除幂等性标识
     * 
     * @param key 幂等性key
     */
    void deleteIdempotent(String key);
    
    /**
     * 检查幂等性标识是否存在
     * 
     * @param key 幂等性key
     * @return 是否存在
     */
    boolean exists(String key);
    
    /**
     * 获取剩余过期时间
     * 
     * @param key 幂等性key
     * @return 剩余过期时间（秒）
     */
    long getExpireTime(String key);
}