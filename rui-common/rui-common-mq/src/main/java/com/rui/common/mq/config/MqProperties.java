package com.rui.common.mq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息队列配置属性
 * 
 * @author rui
 */
@Data
@ConfigurationProperties(prefix = "rui.mq")
public class MqProperties {
    
    /**
     * 是否启用消息队列
     */
    private Boolean enabled = true;
    
    /**
     * RocketMQ配置
     */
    private RocketMqConfig rocketmq = new RocketMqConfig();
    
    /**
     * 生产者配置
     */
    private ProducerConfig producer = new ProducerConfig();
    
    /**
     * 消费者配置
     */
    private ConsumerConfig consumer = new ConsumerConfig();
    
    /**
     * RocketMQ配置
     */
    @Data
    public static class RocketMqConfig {
        
        /**
         * NameServer地址
         */
        private String nameServer = "localhost:9876";
        
        /**
         * 访问密钥
         */
        private String accessKey;
        
        /**
         * 密钥
         */
        private String secretKey;
        
        /**
         * 安全令牌
         */
        private String securityToken;
        
        /**
         * 发送消息超时时间（毫秒）
         */
        private Integer sendMsgTimeout = 3000;
        
        /**
         * 消息最大大小（字节）
         */
        private Integer maxMessageSize = 4194304; // 4MB
        
        /**
         * 重试次数
         */
        private Integer retryTimesWhenSendFailed = 2;
        
        /**
         * 异步发送重试次数
         */
        private Integer retryTimesWhenSendAsyncFailed = 2;
        
        /**
         * 是否在内部发送失败时重试另一个broker
         */
        private Boolean retryAnotherBrokerWhenNotStoreOK = false;
    }
    
    /**
     * 生产者配置
     */
    @Data
    public static class ProducerConfig {
        
        /**
         * 生产者组
         */
        private String group = "default_producer_group";
        
        /**
         * 是否启用事务消息
         */
        private Boolean enableTransaction = false;
        
        /**
         * 事务检查器线程池大小
         */
        private Integer checkThreadPoolMinSize = 1;
        
        /**
         * 事务检查器线程池最大大小
         */
        private Integer checkThreadPoolMaxSize = 1;
        
        /**
         * 事务检查器队列大小
         */
        private Integer checkRequestHoldMax = 2000;
        
        /**
         * 压缩消息阈值（字节）
         */
        private Integer compressMsgBodyOverHowmuch = 4096;
    }
    
    /**
     * 消费者配置
     */
    @Data
    public static class ConsumerConfig {
        
        /**
         * 消费者组
         */
        private String group = "default_consumer_group";
        
        /**
         * 消费模式：CLUSTERING（集群模式）、BROADCASTING（广播模式）
         */
        private String messageModel = "CLUSTERING";
        
        /**
         * 消费类型：CONSUME_FROM_LAST_OFFSET、CONSUME_FROM_FIRST_OFFSET、CONSUME_FROM_TIMESTAMP
         */
        private String consumeFromWhere = "CONSUME_FROM_LAST_OFFSET";
        
        /**
         * 最大重试次数
         */
        private Integer maxReconsumeTimes = 16;
        
        /**
         * 消费超时时间（分钟）
         */
        private Long consumeTimeout = 15L;
        
        /**
         * 消费线程池最小大小
         */
        private Integer consumeThreadMin = 20;
        
        /**
         * 消费线程池最大大小
         */
        private Integer consumeThreadMax = 64;
        
        /**
         * 批量消费最大消息数
         */
        private Integer consumeMessageBatchMaxSize = 1;
        
        /**
         * 拉取消息批量大小
         */
        private Integer pullBatchSize = 32;
    }
}