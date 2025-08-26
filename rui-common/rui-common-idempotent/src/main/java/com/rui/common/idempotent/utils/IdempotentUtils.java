package com.rui.common.idempotent.utils;

import com.rui.common.core.utils.SpringUtils;
import com.rui.common.idempotent.service.IdempotentService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 幂等性工具类
 * 
 * @author rui
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IdempotentUtils {
    
    private static IdempotentService getIdempotentService() {
        return SpringUtils.getBean(IdempotentService.class);
    }
    
    /**
     * 检查是否重复请求
     * 
     * @param key 幂等性key
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return true: 重复请求, false: 非重复请求
     */
    public static boolean isDuplicate(String key, long expireTime, TimeUnit timeUnit) {
        return getIdempotentService().isDuplicate(key, expireTime, timeUnit);
    }
    
    /**
     * 检查是否重复请求（使用默认过期时间5分钟）
     * 
     * @param key 幂等性key
     * @return true: 重复请求, false: 非重复请求
     */
    public static boolean isDuplicate(String key) {
        return isDuplicate(key, 300, TimeUnit.SECONDS);
    }
    
    /**
     * 设置幂等性标识
     * 
     * @param key 幂等性key
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return 是否设置成功
     */
    public static boolean setIdempotent(String key, long expireTime, TimeUnit timeUnit) {
        return getIdempotentService().setIdempotent(key, expireTime, timeUnit);
    }
    
    /**
     * 设置幂等性标识（使用默认过期时间5分钟）
     * 
     * @param key 幂等性key
     * @return 是否设置成功
     */
    public static boolean setIdempotent(String key) {
        return setIdempotent(key, 300, TimeUnit.SECONDS);
    }
    
    /**
     * 删除幂等性标识
     * 
     * @param key 幂等性key
     */
    public static void deleteIdempotent(String key) {
        getIdempotentService().deleteIdempotent(key);
    }
    
    /**
     * 检查幂等性标识是否存在
     * 
     * @param key 幂等性key
     * @return 是否存在
     */
    public static boolean exists(String key) {
        return getIdempotentService().exists(key);
    }
    
    /**
     * 获取剩余过期时间
     * 
     * @param key 幂等性key
     * @return 剩余过期时间（秒）
     */
    public static long getExpireTime(String key) {
        return getIdempotentService().getExpireTime(key);
    }
    
    /**
     * 在幂等性保护下执行操作
     * 
     * @param key 幂等性key
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @param supplier 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws RuntimeException 如果检测到重复请求
     */
    public static <T> T executeWithIdempotent(String key, long expireTime, TimeUnit timeUnit, Supplier<T> supplier) {
        if (isDuplicate(key, expireTime, timeUnit)) {
            throw new RuntimeException("请勿重复操作");
        }
        
        try {
            return supplier.get();
        } catch (Exception e) {
            // 执行失败时删除幂等性标识，允许重试
            deleteIdempotent(key);
            throw e;
        }
    }
    
    /**
     * 在幂等性保护下执行操作（使用默认过期时间5分钟）
     * 
     * @param key 幂等性key
     * @param supplier 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws RuntimeException 如果检测到重复请求
     */
    public static <T> T executeWithIdempotent(String key, Supplier<T> supplier) {
        return executeWithIdempotent(key, 300, TimeUnit.SECONDS, supplier);
    }
    
    /**
     * 在幂等性保护下执行操作（无返回值）
     * 
     * @param key 幂等性key
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @param runnable 要执行的操作
     * @throws RuntimeException 如果检测到重复请求
     */
    public static void executeWithIdempotent(String key, long expireTime, TimeUnit timeUnit, Runnable runnable) {
        executeWithIdempotent(key, expireTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }
    
    /**
     * 在幂等性保护下执行操作（无返回值，使用默认过期时间5分钟）
     * 
     * @param key 幂等性key
     * @param runnable 要执行的操作
     * @throws RuntimeException 如果检测到重复请求
     */
    public static void executeWithIdempotent(String key, Runnable runnable) {
        executeWithIdempotent(key, 300, TimeUnit.SECONDS, runnable);
    }
}