package com.rui.common.lock.enums;

/**
 * 锁类型枚举
 * 
 * @author rui
 */
public enum LockType {
    
    /**
     * 可重入锁
     */
    REENTRANT_LOCK,
    
    /**
     * 公平锁
     */
    FAIR_LOCK,
    
    /**
     * 读锁
     */
    READ_LOCK,
    
    /**
     * 写锁
     */
    WRITE_LOCK,
    
    /**
     * 红锁（多个Redis实例）
     */
    RED_LOCK,
    
    /**
     * 信号量
     */
    SEMAPHORE,
    
    /**
     * 闭锁
     */
    COUNT_DOWN_LATCH
}