package com.rui.common.idempotent.service.impl;

import com.rui.common.idempotent.service.IdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的幂等性服务实现
 * 
 * @author rui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisIdempotentServiceImpl implements IdempotentService {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    private DefaultRedisScript<Long> setIfAbsentScript;
    
    @PostConstruct
    public void init() {
        // 初始化Lua脚本
        setIfAbsentScript = new DefaultRedisScript<>();
        setIfAbsentScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/set_if_absent.lua")));
        setIfAbsentScript.setResultType(Long.class);
    }
    
    @Override
    public boolean isDuplicate(String key, long expireTime, TimeUnit timeUnit) {
        try {
            // 使用Lua脚本原子性地检查并设置
            Long result = stringRedisTemplate.execute(
                setIfAbsentScript,
                Collections.singletonList(key),
                String.valueOf(timeUnit.toSeconds(expireTime))
            );
            // 返回值为1表示设置成功（非重复），0表示已存在（重复）
            return result != null && result == 0;
        } catch (Exception e) {
            log.error("检查幂等性失败, key: {}", key, e);
            // 异常情况下认为是重复请求，保证安全性
            return true;
        }
    }
    
    @Override
    public boolean setIdempotent(String key, long expireTime, TimeUnit timeUnit) {
        try {
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(
                key, 
                String.valueOf(System.currentTimeMillis()), 
                expireTime, 
                timeUnit
            );
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("设置幂等性标识失败, key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public void deleteIdempotent(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.error("删除幂等性标识失败, key: {}", key, e);
        }
    }
    
    @Override
    public boolean exists(String key) {
        try {
            Boolean result = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查幂等性标识是否存在失败, key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public long getExpireTime(String key) {
        try {
            Long expireTime = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expireTime != null ? expireTime : -1;
        } catch (Exception e) {
            log.error("获取幂等性标识过期时间失败, key: {}", key, e);
            return -1;
        }
    }
}