package com.rui.common.lock.service.impl;

import com.rui.common.lock.enums.LockType;
import com.rui.common.lock.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 基于Redisson的分布式锁服务实现
 * 
 * @author rui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonDistributedLockServiceImpl implements DistributedLockService {
    
    private final RedissonClient redissonClient;
    
    @Override
    public RLock getLock(String lockKey, LockType lockType) {
        switch (lockType) {
            case REENTRANT_LOCK:
                return redissonClient.getLock(lockKey);
            case FAIR_LOCK:
                return redissonClient.getFairLock(lockKey);
            case READ_LOCK:
                return redissonClient.getReadWriteLock(lockKey).readLock();
            case WRITE_LOCK:
                return redissonClient.getReadWriteLock(lockKey).writeLock();
            default:
                return redissonClient.getLock(lockKey);
        }
    }
    
    @Override
    public boolean tryLock(String lockKey, LockType lockType, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getLock(lockKey, lockType);
        try {
            if (waitTime == -1 && leaseTime == -1) {
                return lock.tryLock();
            } else if (waitTime != -1 && leaseTime == -1) {
                return lock.tryLock(waitTime, timeUnit);
            } else if (waitTime == -1 && leaseTime != -1) {
                lock.lock(leaseTime, timeUnit);
                return true;
            } else {
                return lock.tryLock(waitTime, leaseTime, timeUnit);
            }
        } catch (InterruptedException e) {
            log.error("获取分布式锁被中断: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.error("获取分布式锁异常: {}", lockKey, e);
            return false;
        }
    }
    
    @Override
    public void unlock(String lockKey, LockType lockType) {
        RLock lock = getLock(lockKey, lockType);
        unlock(lock);
    }
    
    @Override
    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
            } catch (Exception e) {
                log.error("释放分布式锁异常", e);
            }
        }
    }
    
    @Override
    public boolean isLocked(String lockKey, LockType lockType) {
        RLock lock = getLock(lockKey, lockType);
        return lock.isLocked();
    }
    
    @Override
    public boolean isHeldByCurrentThread(String lockKey, LockType lockType) {
        RLock lock = getLock(lockKey, lockType);
        return lock.isHeldByCurrentThread();
    }
    
    @Override
    public long remainTimeToLive(String lockKey, LockType lockType) {
        RLock lock = getLock(lockKey, lockType);
        return lock.remainTimeToLive();
    }
    
    /**
     * 获取红锁（多个Redis实例）
     * 
     * @param lockKey 锁key
     * @return 红锁对象
     */
    public RLock getRedLock(String lockKey) {
        // 这里可以配置多个Redis实例
        RLock lock1 = redissonClient.getLock(lockKey + ":1");
        RLock lock2 = redissonClient.getLock(lockKey + ":2");
        RLock lock3 = redissonClient.getLock(lockKey + ":3");
        return redissonClient.getRedLock(lock1, lock2, lock3);
    }
    
    /**
     * 获取信号量
     * 
     * @param semaphoreKey 信号量key
     * @return 信号量对象
     */
    public RSemaphore getSemaphore(String semaphoreKey) {
        return redissonClient.getSemaphore(semaphoreKey);
    }
    
    /**
     * 获取闭锁
     * 
     * @param countDownLatchKey 闭锁key
     * @return 闭锁对象
     */
    public RCountDownLatch getCountDownLatch(String countDownLatchKey) {
        return redissonClient.getCountDownLatch(countDownLatchKey);
    }
}