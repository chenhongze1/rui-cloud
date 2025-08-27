package com.rui.common.mq.service.impl;

import com.rui.common.mq.service.MessageProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 消息生产者服务实现
 * 
 * @author rui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducerServiceImpl implements MessageProducerService {
    
    private final RocketMQTemplate rocketMQTemplate;
    
    @Override
    public SendResult sendSyncMessage(String topic, Object message) {
        return sendSyncMessage(topic, null, null, message);
    }
    
    @Override
    public SendResult sendSyncMessage(String topic, String tag, Object message) {
        return sendSyncMessage(topic, tag, null, message);
    }
    
    @Override
    public SendResult sendSyncMessage(String topic, String tag, String key, Object message) {
        try {
            String destination = buildDestination(topic, tag);
            Message<Object> msg = buildMessage(message, key, null);
            
            log.debug("发送同步消息 - Topic: {}, Tag: {}, Key: {}", topic, tag, key);
            SendResult result = rocketMQTemplate.syncSend(destination, msg);
            log.debug("同步消息发送成功 - MsgId: {}, Status: {}", result.getMsgId(), result.getSendStatus());
            
            return result;
        } catch (Exception e) {
            log.error("发送同步消息失败 - Topic: {}, Tag: {}, Key: {}", topic, tag, key, e);
            throw new RuntimeException("发送同步消息失败", e);
        }
    }
    
    @Override
    public void sendAsyncMessage(String topic, Object message) {
        sendAsyncMessage(topic, null, null, message);
    }
    
    @Override
    public void sendAsyncMessage(String topic, String tag, Object message) {
        sendAsyncMessage(topic, tag, null, message);
    }
    
    @Override
    public void sendAsyncMessage(String topic, String tag, String key, Object message) {
        try {
            String destination = buildDestination(topic, tag);
            Message<Object> msg = buildMessage(message, key, null);
            
            log.debug("发送异步消息 - Topic: {}, Tag: {}, Key: {}", topic, tag, key);
            rocketMQTemplate.asyncSend(destination, msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.debug("异步消息发送成功 - MsgId: {}, Status: {}", sendResult.getMsgId(), sendResult.getSendStatus());
                }
                
                @Override
                public void onException(Throwable e) {
                    log.error("异步消息发送失败 - Topic: {}, Tag: {}, Key: {}", topic, tag, key, e);
                }
            });
        } catch (Exception e) {
            log.error("发送异步消息失败 - Topic: {}, Tag: {}, Key: {}", topic, tag, key, e);
            throw new RuntimeException("发送异步消息失败", e);
        }
    }
    
    @Override
    public void sendOneWayMessage(String topic, Object message) {
        sendOneWayMessage(topic, null, null, message);
    }
    
    @Override
    public void sendOneWayMessage(String topic, String tag, Object message) {
        sendOneWayMessage(topic, tag, null, message);
    }
    
    @Override
    public void sendOneWayMessage(String topic, String tag, String key, Object message) {
        try {
            String destination = buildDestination(topic, tag);
            Message<Object> msg = buildMessage(message, key, null);
            
            log.debug("发送单向消息 - Topic: {}, Tag: {}, Key: {}", topic, tag, key);
            rocketMQTemplate.sendOneWay(destination, msg);
            log.debug("单向消息发送完成 - Topic: {}, Tag: {}, Key: {}", topic, tag, key);
        } catch (Exception e) {
            log.error("发送单向消息失败 - Topic: {}, Tag: {}, Key: {}", topic, tag, key, e);
            throw new RuntimeException("发送单向消息失败", e);
        }
    }
    
    @Override
    public SendResult sendDelayMessage(String topic, Object message, int delayLevel) {
        return sendDelayMessage(topic, null, null, message, delayLevel);
    }
    
    @Override
    public SendResult sendDelayMessage(String topic, String tag, Object message, int delayLevel) {
        return sendDelayMessage(topic, tag, null, message, delayLevel);
    }
    
    @Override
    public SendResult sendDelayMessage(String topic, String tag, String key, Object message, int delayLevel) {
        try {
            String destination = buildDestination(topic, tag);
            Message<Object> msg = MessageBuilder.withPayload(message)
                    .setHeader(RocketMQHeaders.KEYS, key)
                    .setHeader(RocketMQHeaders.DELAY, delayLevel)
                    .build();
            
            log.debug("发送延时消息 - Topic: {}, Tag: {}, Key: {}, DelayLevel: {}", topic, tag, key, delayLevel);
            SendResult result = rocketMQTemplate.syncSend(destination, msg);
            log.debug("延时消息发送成功 - MsgId: {}, Status: {}", result.getMsgId(), result.getSendStatus());
            
            return result;
        } catch (Exception e) {
            log.error("发送延时消息失败 - Topic: {}, Tag: {}, Key: {}, DelayLevel: {}", topic, tag, key, delayLevel, e);
            throw new RuntimeException("发送延时消息失败", e);
        }
    }
    
    @Override
    public SendResult sendOrderlyMessage(String topic, Object message, String hashKey) {
        return sendOrderlyMessage(topic, null, message, hashKey);
    }
    
    @Override
    public SendResult sendOrderlyMessage(String topic, String tag, Object message, String hashKey) {
        try {
            String destination = buildDestination(topic, tag);
            
            log.debug("发送顺序消息 - Topic: {}, Tag: {}, HashKey: {}", topic, tag, hashKey);
            SendResult result = rocketMQTemplate.syncSendOrderly(destination, message, hashKey);
            log.debug("顺序消息发送成功 - MsgId: {}, Status: {}", result.getMsgId(), result.getSendStatus());
            
            return result;
        } catch (Exception e) {
            log.error("发送顺序消息失败 - Topic: {}, Tag: {}, HashKey: {}", topic, tag, hashKey, e);
            throw new RuntimeException("发送顺序消息失败", e);
        }
    }
    
    @Override
    public SendResult sendTransactionMessage(String topic, Object message, Object arg) {
        return sendTransactionMessage(topic, null, message, arg);
    }
    
    @Override
    public SendResult sendTransactionMessage(String topic, String tag, Object message, Object arg) {
        try {
            String destination = buildDestination(topic, tag);
            Message<Object> msg = buildMessage(message, null, null);
            
            log.debug("发送事务消息 - Topic: {}, Tag: {}", topic, tag);
            SendResult result = rocketMQTemplate.sendMessageInTransaction(destination, msg, arg);
            log.debug("事务消息发送成功 - MsgId: {}, Status: {}", result.getMsgId(), result.getSendStatus());
            
            return result;
        } catch (Exception e) {
            log.error("发送事务消息失败 - Topic: {}, Tag: {}", topic, tag, e);
            throw new RuntimeException("发送事务消息失败", e);
        }
    }
    
    @Override
    public SendResult sendMessageWithProperties(String topic, String tag, String key, Object message, Map<String, String> properties) {
        try {
            String destination = buildDestination(topic, tag);
            Message<Object> msg = buildMessage(message, key, properties);
            
            log.debug("发送带属性消息 - Topic: {}, Tag: {}, Key: {}, Properties: {}", topic, tag, key, properties);
            SendResult result = rocketMQTemplate.syncSend(destination, msg);
            log.debug("带属性消息发送成功 - MsgId: {}, Status: {}", result.getMsgId(), result.getSendStatus());
            
            return result;
        } catch (Exception e) {
            log.error("发送带属性消息失败 - Topic: {}, Tag: {}, Key: {}, Properties: {}", topic, tag, key, properties, e);
            throw new RuntimeException("发送带属性消息失败", e);
        }
    }
    
    @Override
    public RocketMQTemplate getRocketMQTemplate() {
        return rocketMQTemplate;
    }
    
    /**
     * 构建目标地址
     * 
     * @param topic 主题
     * @param tag 标签
     * @return 目标地址
     */
    private String buildDestination(String topic, String tag) {
        if (StringUtils.hasText(tag)) {
            return topic + ":" + tag;
        }
        return topic;
    }
    
    /**
     * 构建消息
     * 
     * @param payload 消息内容
     * @param key 消息键
     * @param properties 消息属性
     * @return 消息对象
     */
    private Message<Object> buildMessage(Object payload, String key, Map<String, String> properties) {
        MessageBuilder<Object> builder = MessageBuilder.withPayload(payload);
        
        if (StringUtils.hasText(key)) {
            builder.setHeader(RocketMQHeaders.KEYS, key);
        }
        
        if (properties != null && !properties.isEmpty()) {
            properties.forEach(builder::setHeader);
        }
        
        return builder.build();
    }
}