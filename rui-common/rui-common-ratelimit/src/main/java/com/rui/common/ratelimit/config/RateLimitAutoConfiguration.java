package com.rui.common.ratelimit.config;

import com.rui.common.ratelimit.aspect.RateLimitAspect;
import com.rui.common.ratelimit.properties.RateLimitProperties;
import com.rui.common.ratelimit.service.RateLimitService;
import com.rui.common.ratelimit.service.impl.RedisRateLimitServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 限流自动配置
 * 
 * @author rui
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "rui.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {
    
    private final RateLimitProperties rateLimitProperties;
    
    /**
     * 限流服务
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitService rateLimitService(RedisTemplate<String, Object> redisTemplate) {
        return new RedisRateLimitServiceImpl(redisTemplate);
    }
    
    /**
     * 限流切面
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(RateLimitService rateLimitService) {
        return new RateLimitAspect(rateLimitService);
    }
}