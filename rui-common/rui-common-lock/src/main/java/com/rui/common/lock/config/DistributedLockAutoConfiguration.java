package com.rui.common.lock.config;

import com.rui.common.lock.aspect.DistributedLockAspect;
import com.rui.common.lock.service.DistributedLockService;
import com.rui.common.lock.service.impl.RedissonDistributedLockServiceImpl;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 分布式锁自动配置
 * 
 * @author rui
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@ConditionalOnBean(RedissonClient.class)
public class DistributedLockAutoConfiguration {
    
    /**
     * 分布式锁服务
     */
    @Bean
    @ConditionalOnMissingBean
    public DistributedLockService distributedLockService(RedissonClient redissonClient) {
        return new RedissonDistributedLockServiceImpl(redissonClient);
    }
    
    /**
     * 分布式锁切面
     */
    @Bean
    @ConditionalOnMissingBean
    public DistributedLockAspect distributedLockAspect(DistributedLockService distributedLockService) {
        return new DistributedLockAspect(distributedLockService);
    }
}