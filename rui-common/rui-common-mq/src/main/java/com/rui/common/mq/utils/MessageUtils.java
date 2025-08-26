package com.rui.common.mq.utils;

import com.rui.common.core.utils.SpringUtils;
import com.rui.common.mq.service.MessageProducerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 消息工具类
 * 
 * @author rui
 */
@Slf4j
public class MessageUtils {
    
    private static MessageProducerService messageProducerService;
    
    /**
     * 获取消息生产者服务
     * 
     * @return 消息生产者服务
     */
    private static MessageProducerService getMessageProducerService() {
        if (messageProducerService == null) {
            messageProducerService = SpringUtils.getBean(MessageProducerService.class);
        }
        return messageProducerService;
    }
    
    /**
     * 发送同步消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @return 发送结果
     */
    public static SendResult sendSyncMessage(String topic, Object message) {
        return getMessageProducerService().sendSyncMessage(topic, message);
    }
    
    /**
     * 发送同步消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     * @return 发送结果
     */
    public static SendResult sendSyncMessage(String topic, String tag, Object message) {
        return getMessageProducerService().sendSyncMessage(topic, tag, message);
    }
    
    /**
     * 发送同步消息（带标签和键）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param key 消息键
     * @param message 消息内容
     * @return 发送结果
     */
    public static SendResult sendSyncMessage(String topic, String tag, String key, Object message) {
        return getMessageProducerService().sendSyncMessage(topic, tag, key, message);
    }
    
    /**
     * 发送异步消息
     * 
     * @param topic 主题
     * @param message 消息内容
     */
    public static void sendAsyncMessage(String topic, Object message) {
        getMessageProducerService().sendAsyncMessage(topic, message);
    }
    
    /**
     * 发送异步消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     */
    public static void sendAsyncMessage(String topic, String tag, Object message) {
        getMessageProducerService().sendAsyncMessage(topic, tag, message);
    }
    
    /**
     * 发送异步消息（带标签和键）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param key 消息键
     * @param message 消息内容
     */
    public static void sendAsyncMessage(String topic, String tag, String key, Object message) {
        getMessageProducerService().sendAsyncMessage(topic, tag, key, message);
    }
    
    /**
     * 发送单向消息
     * 
     * @param topic 主题
     * @param message 消息内容
     */
    public static void sendOneWayMessage(String topic, Object message) {
        getMessageProducerService().sendOneWayMessage(topic, message);
    }
    
    /**
     * 发送单向消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     */
    public static void sendOneWayMessage(String topic, String tag, Object message) {
        getMessageProducerService().sendOneWayMessage(topic, tag, message);
    }
    
    /**
     * 发送单向消息（带标签和键）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param key 消息键
     * @param message 消息内容
     */
    public static void sendOneWayMessage(String topic, String tag, String key, Object message) {
        getMessageProducerService().sendOneWayMessage(topic, tag, key, message);
    }
    
    /**
     * 发送延时消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param delayLevel 延时级别（1-18）
     * @return 发送结果
     */
    public static SendResult sendDelayMessage(String topic, Object message, int delayLevel) {
        return getMessageProducerService().sendDelayMessage(topic, message, delayLevel);
    }
    
    /**
     * 发送延时消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     * @param delayLevel 延时级别（1-18）
     * @return 发送结果
     */
    public static SendResult sendDelayMessage(String topic, String tag, Object message, int delayLevel) {
        return getMessageProducerService().sendDelayMessage(topic, tag, message, delayLevel);
    }
    
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
    public static SendResult sendDelayMessage(String topic, String tag, String key, Object message, int delayLevel) {
        return getMessageProducerService().sendDelayMessage(topic, tag, key, message, delayLevel);
    }
    
    /**
     * 发送顺序消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param hashKey 哈希键（用于选择队列）
     * @return 发送结果
     */
    public static SendResult sendOrderlyMessage(String topic, Object message, String hashKey) {
        return getMessageProducerService().sendOrderlyMessage(topic, message, hashKey);
    }
    
    /**
     * 发送顺序消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     * @param hashKey 哈希键（用于选择队列）
     * @return 发送结果
     */
    public static SendResult sendOrderlyMessage(String topic, String tag, Object message, String hashKey) {
        return getMessageProducerService().sendOrderlyMessage(topic, tag, message, hashKey);
    }
    
    /**
     * 发送事务消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param arg 事务参数
     * @return 发送结果
     */
    public static SendResult sendTransactionMessage(String topic, Object message, Object arg) {
        return getMessageProducerService().sendTransactionMessage(topic, message, arg);
    }
    
    /**
     * 发送事务消息（带标签）
     * 
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     * @param arg 事务参数
     * @return 发送结果
     */
    public static SendResult sendTransactionMessage(String topic, String tag, Object message, Object arg) {
        return getMessageProducerService().sendTransactionMessage(topic, tag, message, arg);
    }
    
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
    public static SendResult sendMessageWithProperties(String topic, String tag, String key, Object message, Map<String, String> properties) {
        return getMessageProducerService().sendMessageWithProperties(topic, tag, key, message, properties);
    }
    
    /**
     * 构建消息键
     * 
     * @param prefix 前缀
     * @param businessId 业务ID
     * @return 消息键
     */
    public static String buildMessageKey(String prefix, String businessId) {
        if (StringUtils.hasText(prefix) && StringUtils.hasText(businessId)) {
            return prefix + ":" + businessId;
        }
        return businessId;
    }
    
    /**
     * 构建消息键（带时间戳）
     * 
     * @param prefix 前缀
     * @param businessId 业务ID
     * @return 消息键
     */
    public static String buildMessageKeyWithTimestamp(String prefix, String businessId) {
        String key = buildMessageKey(prefix, businessId);
        return key + ":" + System.currentTimeMillis();
    }
}