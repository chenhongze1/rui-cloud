package com.rui.common.mq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.messaging.Message;

/**
 * 消息消费者基类
 * 
 * @author rui
 */
@Slf4j
public abstract class BaseMessageConsumer<T> implements RocketMQListener<T> {
    
    @Override
    public void onMessage(T message) {
        try {
            log.debug("开始消费消息: {}", message);
            
            // 消息预处理
            if (!preProcess(message)) {
                log.warn("消息预处理失败，跳过消费: {}", message);
                return;
            }
            
            // 执行业务逻辑
            boolean success = doConsume(message);
            
            if (success) {
                log.debug("消息消费成功: {}", message);
                // 消息后处理
                postProcess(message, true, null);
            } else {
                log.warn("消息消费失败: {}", message);
                // 消息后处理
                postProcess(message, false, new RuntimeException("消息消费失败"));
                throw new RuntimeException("消息消费失败");
            }
        } catch (Exception e) {
            log.error("消息消费异常: {}", message, e);
            // 消息后处理
            postProcess(message, false, e);
            throw e;
        }
    }
    
    /**
     * 消息预处理
     * 
     * @param message 消息内容
     * @return 是否继续处理
     */
    protected boolean preProcess(T message) {
        return true;
    }
    
    /**
     * 执行消息消费业务逻辑
     * 
     * @param message 消息内容
     * @return 是否消费成功
     */
    protected abstract boolean doConsume(T message);
    
    /**
     * 消息后处理
     * 
     * @param message 消息内容
     * @param success 是否成功
     * @param exception 异常信息
     */
    protected void postProcess(T message, boolean success, Exception exception) {
        // 子类可以重写此方法进行后处理
    }
    
    /**
     * 获取消费者组
     * 
     * @return 消费者组
     */
    protected abstract String getConsumerGroup();
    
    /**
     * 获取主题
     * 
     * @return 主题
     */
    protected abstract String getTopic();
    
    /**
     * 获取标签（可选）
     * 
     * @return 标签
     */
    protected String getTag() {
        return "*";
    }
    
    /**
     * 获取消费模式
     * 
     * @return 消费模式
     */
    protected ConsumeMode getConsumeMode() {
        return ConsumeMode.CONCURRENTLY;
    }
    
    /**
     * 获取消息模型
     * 
     * @return 消息模型
     */
    protected MessageModel getMessageModel() {
        return MessageModel.CLUSTERING;
    }
    
    /**
     * 获取最大重试次数
     * 
     * @return 最大重试次数
     */
    protected int getMaxReconsumeTimes() {
        return 16;
    }
    
    /**
     * 获取消费超时时间（分钟）
     * 
     * @return 消费超时时间
     */
    protected long getConsumeTimeout() {
        return 15L;
    }
}