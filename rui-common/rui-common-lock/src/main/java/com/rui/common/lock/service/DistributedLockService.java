package com.rui.common.lock.service;

import com.rui.common.lock.enums.LockType;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁服务接口
 * 
 * @author rui
 */
public interface DistributedLockService {
    
    /**
     * 获取锁
     * 
     * @param lockKey 锁key
     * @param lockType 锁类型
     * @return 锁对象
     */
    RLock getLock(String lockKey, LockType lockType);
    
    /**
     * 尝试获取锁
     * 
     * @param lockKey 锁key
     * @param lockType 锁类型
     * @param waitTime 等待时间
     * @param leaseTime 锁定时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey, LockType lockType, long waitTime, long leaseTime, TimeUnit timeUnit);
    
    /**
     * 释放锁
     * 
     * @param lockKey 锁key
     * @param lockType 锁类型
     */
    void unlock(String lockKey, LockType lockType);
    
    /**
     * 释放锁
     * 
     * @param lock 锁对象
     */
    void unlock(RLock lock);
    
    /**
     * 检查锁是否被持有
     * 
     * @param lockKey 锁key
     * @param lockType 锁类型
     * @return 是否被持有
     */
    boolean isLocked(String lockKey, LockType lockType);
    
    /**
     * 检查当前线程是否持有锁
     * 
     * @param lockKey 锁key
     * @param lockType 锁类型
     * @return 是否持有
     */
    boolean isHeldByCurrentThread(String lockKey, LockType lockType);
    
    /**
     * 获取锁的剩余时间
     * 
     * @param lockKey 锁key
     * @param lockType 锁类型
     * @return 剩余时间（毫秒）
     */
    long remainTimeToLive(String lockKey, LockType lockType);
}