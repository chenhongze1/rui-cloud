package com.rui.common.mq.config;

import com.rui.common.mq.service.MessageProducerService;
import com.rui.common.mq.service.impl.MessageProducerServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 消息队列自动配置
 * 
 * @author rui
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MqProperties.class)
@ConditionalOnProperty(prefix = "rui.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.apache.rocketmq.spring.core.RocketMQTemplate")
public class MqAutoConfiguration {
    
    /**
     * 消息生产者服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RocketMQTemplate.class)
    public MessageProducerService messageProducerService(RocketMQTemplate rocketMQTemplate) {
        log.info("[MQ模块] 初始化消息生产者服务");
        return new MessageProducerServiceImpl(rocketMQTemplate);
    }
}