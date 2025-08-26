package com.rui.common.idempotent.config;

import com.rui.common.idempotent.aspect.IdempotentAspect;
import com.rui.common.idempotent.service.IdempotentService;
import com.rui.common.idempotent.service.impl.RedisIdempotentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 幂等性自动配置
 * 
 * @author rui
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(IdempotentProperties.class)
@ConditionalOnProperty(prefix = "rui.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotentAutoConfiguration {
    
    /**
     * 幂等性服务
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentService idempotentService(StringRedisTemplate stringRedisTemplate) {
        log.info("[幂等性模块] 初始化幂等性服务");
        return new RedisIdempotentServiceImpl(stringRedisTemplate);
    }
    
    /**
     * 幂等性切面
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(IdempotentService idempotentService) {
        log.info("[幂等性模块] 初始化幂等性切面");
        return new IdempotentAspect(idempotentService);
    }
}