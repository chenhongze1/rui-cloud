package com.rui.common.ratelimit.service.impl;

import com.rui.common.ratelimit.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的限流服务实现
 * 
 * @author rui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimitServiceImpl implements RateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private DefaultRedisScript<Long> rateLimitScript;
    private DefaultRedisScript<Long> getRemainingScript;
    private DefaultRedisScript<Long> getResetTimeScript;
    
    @PostConstruct
    public void init() {
        // 初始化限流脚本
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/ratelimit.lua")));
        rateLimitScript.setResultType(Long.class);
        
        // 初始化获取剩余次数脚本
        getRemainingScript = new DefaultRedisScript<>();
        getRemainingScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/get_remaining.lua")));
        getRemainingScript.setResultType(Long.class);
        
        // 初始化获取重置时间脚本
        getResetTimeScript = new DefaultRedisScript<>();
        getResetTimeScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/get_reset_time.lua")));
        getResetTimeScript.setResultType(Long.class);
    }
    
    @Override
    public boolean tryAcquire(String key, int count, int time, TimeUnit timeUnit) {
        try {
            String redisKey = buildKey(key);
            long timeInSeconds = timeUnit.toSeconds(time);
            
            // 执行Lua脚本进行限流判断
            Long result = redisTemplate.execute(rateLimitScript, 
                    Collections.singletonList(redisKey), 
                    count, timeInSeconds, System.currentTimeMillis());
            
            boolean allowed = result != null && result == 1;
            
            if (!allowed) {
                log.warn("限流触发: key={}, count={}, time={}s", key, count, timeInSeconds);
            }
            
            return allowed;
            
        } catch (Exception e) {
            log.error("限流检查异常: key={}", key, e);
            // 异常情况下允许通过，避免影响业务
            return true;
        }
    }
    
    @Override
    public long getRemaining(String key) {
        try {
            String redisKey = buildKey(key);
            Long result = redisTemplate.execute(getRemainingScript, 
                    Collections.singletonList(redisKey));
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取剩余次数异常: key={}", key, e);
            return 0;
        }
    }
    
    @Override
    public long getResetTime(String key) {
        try {
            String redisKey = buildKey(key);
            Long result = redisTemplate.execute(getResetTimeScript, 
                    Collections.singletonList(redisKey));
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取重置时间异常: key={}", key, e);
            return 0;
        }
    }
    
    @Override
    public void clear(String key) {
        try {
            String redisKey = buildKey(key);
            redisTemplate.delete(redisKey);
            log.debug("清除限流记录: key={}", key);
        } catch (Exception e) {
            log.error("清除限流记录异常: key={}", key, e);
        }
    }
    
    @Override
    public boolean isLimited(String key) {
        try {
            String redisKey = buildKey(key);
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            log.error("检查限流状态异常: key={}", key, e);
            return false;
        }
    }
    
    /**
     * 构建Redis key
     */
    private String buildKey(String key) {
        return "rate_limit:" + key;
    }
}