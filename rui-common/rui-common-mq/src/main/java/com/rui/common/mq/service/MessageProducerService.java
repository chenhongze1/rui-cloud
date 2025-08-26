package com.rui.common.mq.service;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.util.Map;

/**
 * 消息生产者服务接口
 * 
 * @author rui
 */
public interface MessageProducerService {
    
    /**
     * 发送同步消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @return 发送结果
     */
    SendResult sendSyncMessage(String topic, Object message);
    
    /**
     * 发送同步消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     * @return 发送结果
     */
    SendResult sendSyncMessage(String topic, String tag, Object message);
    
    /**
     * 发送同步消息（带标签和键）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param key 消息键
     * @param message 消息内容
     * @return 发送结果
     */
    SendResult sendSyncMessage(String topic, String tag, String key, Object message);
    
    /**
     * 发送异步消息
     * 
     * @param topic 主题
     * @param message 消息内容
     */
    void sendAsyncMessage(String topic, Object message);
    
    /**
     * 发送异步消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     */
    void sendAsyncMessage(String topic, String tag, Object message);
    
    /**
     * 发送异步消息（带标签和键）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param key 消息键
     * @param message 消息内容
     */
    void sendAsyncMessage(String topic, String tag, String key, Object message);
    
    /**
     * 发送单向消息
     * 
     * @param topic 主题
     * @param message 消息内容
     */
    void sendOneWayMessage(String topic, Object message);
    
    /**
     * 发送单向消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     */
    void sendOneWayMessage(String topic, String tag, Object message);
    
    /**
     * 发送单向消息（带标签和键）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param key 消息键
     * @param message 消息内容
     */
    void sendOneWayMessage(String topic, String tag, String key, Object message);
    
    /**
     * 发送延时消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param delayLevel 延时级别（1-18）
     * @return 发送结果
     */
    SendResult sendDelayMessage(String topic, Object message, int delayLevel);
    
    /**
     * 发送延时消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     * @param delayLevel 延时级别（1-18）
     * @return 发送结果
     */
    SendResult sendDelayMessage(String topic, String tag, Object message, int delayLevel);
    
    /**
     * 发送延时消息（带标签和键）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param key 消息键
     * @param message 消息内容
     * @param delayLevel 延时级别（1-18）
     * @return 发送结果
     */
    SendResult sendDelayMessage(String topic, String tag, String key, Object message, int delayLevel);
    
    /**
     * 发送顺序消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param hashKey 哈希键（用于选择队列）
     * @return 发送结果
     */
    SendResult sendOrderlyMessage(String topic, Object message, String hashKey);
    
    /**
     * 发送顺序消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     * @param hashKey 哈希键（用于选择队列）
     * @return 发送结果
     */
    SendResult sendOrderlyMessage(String topic, String tag, Object message, String hashKey);
    
    /**
     * 发送事务消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param arg 事务参数
     * @return 发送结果
     */
    SendResult sendTransactionMessage(String topic, Object message, Object arg);
    
    /**
     * 发送事务消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     * @param arg 事务参数
     * @return 发送结果
     */
    SendResult sendTransactionMessage(String topic, String tag, Object message, Object arg);
    
    /**
     * 发送带属性的消息
     * 
     * @param topic 主题
     * @param tag 标签
     * @param key 消息键
     * @param message 消息内容
     * @param properties 消息属性
     * @return 发送结果
     */
    SendResult sendMessageWithProperties(String topic, String tag, String key, Object message, Map<String, String> properties);
    
    /**
     * 获取RocketMQ模板
     * 
     * @return RocketMQ模板
     */
    RocketMQTemplate getRocketMQTemplate();
}