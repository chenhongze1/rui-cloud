package com.rui.common.monitoring;

import com.rui.common.core.config.MonitoringConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 告警管理器
 * 处理监控告警的发送和管理
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertManager {

    private final MonitoringConfig monitoringConfig;
    
    // 告警抑制缓存，防止重复告警
    private final Map<String, LocalDateTime> alertSuppressionCache = new ConcurrentHashMap<>();
    
    // 告警发送线程池
    private final ScheduledExecutorService alertExecutor = Executors.newScheduledThreadPool(2);

    /**
     * 发送告警
     */
    public void sendAlert(AlertLevel level, String title, String message, Map<String, Object> context) {
        try {
            // 检查告警是否被抑制
            String alertKey = generateAlertKey(level, title);
            if (isAlertSuppressed(alertKey)) {
                log.debug("告警被抑制: {}", alertKey);
                return;
            }
            
            // 记录告警抑制
            recordAlertSuppression(alertKey);
            
            // 异步发送告警
            alertExecutor.submit(() -> {
                try {
                    doSendAlert(level, title, message, context);
                } catch (Exception e) {
                    log.error("发送告警失败: title={}, message={}", title, message, e);
                }
            });
            
        } catch (Exception e) {
            log.error("告警处理失败", e);
        }
    }

    /**
     * 发送系统告警
     */
    public void sendSystemAlert(String message, Map<String, Object> context) {
        sendAlert(AlertLevel.ERROR, "系统告警", message, context);
    }

    /**
     * 发送性能告警
     */
    public void sendPerformanceAlert(String message, Map<String, Object> context) {
        sendAlert(AlertLevel.WARNING, "性能告警", message, context);
    }

    /**
     * 发送业务告警
     */
    public void sendBusinessAlert(String message, Map<String, Object> context) {
        sendAlert(AlertLevel.INFO, "业务告警", message, context);
    }

    /**
     * 发送安全告警
     */
    public void sendSecurityAlert(String message, Map<String, Object> context) {
        sendAlert(AlertLevel.CRITICAL, "安全告警", message, context);
    }

    /**
     * 实际发送告警
     */
    private void doSendAlert(AlertLevel level, String title, String message, Map<String, Object> context) {
        MonitoringConfig.AlertConfig alertConfig = monitoringConfig.getAlert();
        
        // 构建告警内容
        String alertContent = buildAlertContent(level, title, message, context);
        
        // 根据配置的渠道发送告警
        if (alertConfig.getChannels().getEmail().isEnabled()) {
            sendEmailAlert(alertContent, alertConfig.getChannels().getEmail());
        }
        
        if (alertConfig.getChannels().getSms().isEnabled()) {
            sendSmsAlert(alertContent, alertConfig.getChannels().getSms());
        }
        
        if (alertConfig.getChannels().getWebhook().isEnabled()) {
            sendWebhookAlert(alertContent, alertConfig.getChannels().getWebhook());
        }
        
        if (alertConfig.getChannels().getDingTalk().isEnabled()) {
            sendDingTalkAlert(alertContent, alertConfig.getChannels().getDingTalk());
        }
        
        log.info("告警发送完成: level={}, title={}", level, title);
    }

    /**
     * 构建告警内容
     */
    private String buildAlertContent(AlertLevel level, String title, String message, Map<String, Object> context) {
        StringBuilder content = new StringBuilder();
        
        content.append("【").append(level.getDisplayName()).append("】").append(title).append("\n");
        content.append("时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        content.append("消息: ").append(message).append("\n");
        
        if (context != null && !context.isEmpty()) {
            content.append("详细信息:\n");
            context.forEach((key, value) -> 
                content.append("  ").append(key).append(": ").append(value).append("\n")
            );
        }
        
        return content.toString();
    }

    /**
     * 发送邮件告警
     */
    private void sendEmailAlert(String content, MonitoringConfig.EmailConfig emailConfig) {
        try {
            // 这里实现邮件发送逻辑
            log.info("发送邮件告警到: {}", emailConfig.getRecipients());
            log.debug("邮件内容: {}", content);
            
            // 实际实现中可以使用Spring Mail或其他邮件服务
            
        } catch (Exception e) {
            log.error("发送邮件告警失败", e);
        }
    }

    /**
     * 发送短信告警
     */
    private void sendSmsAlert(String content, MonitoringConfig.SmsConfig smsConfig) {
        try {
            // 这里实现短信发送逻辑
            log.info("发送短信告警到: {}", smsConfig.getPhoneNumbers());
            log.debug("短信内容: {}", content);
            
            // 实际实现中可以集成阿里云短信、腾讯云短信等服务
            
        } catch (Exception e) {
            log.error("发送短信告警失败", e);
        }
    }

    /**
     * 发送Webhook告警
     */
    private void sendWebhookAlert(String content, MonitoringConfig.WebhookConfig webhookConfig) {
        try {
            // 这里实现Webhook发送逻辑
            log.info("发送Webhook告警到: {}", webhookConfig.getUrl());
            log.debug("Webhook内容: {}", content);
            
            // 实际实现中可以使用RestTemplate或WebClient发送HTTP请求
            
        } catch (Exception e) {
            log.error("发送Webhook告警失败", e);
        }
    }

    /**
     * 发送钉钉告警
     */
    private void sendDingTalkAlert(String content, MonitoringConfig.DingTalkConfig dingTalkConfig) {
        try {
            // 这里实现钉钉机器人发送逻辑
            log.info("发送钉钉告警到: {}", dingTalkConfig.getWebhook());
            log.debug("钉钉内容: {}", content);
            
            // 实际实现中可以调用钉钉机器人API
            
        } catch (Exception e) {
            log.error("发送钉钉告警失败", e);
        }
    }

    /**
     * 生成告警键
     */
    private String generateAlertKey(AlertLevel level, String title) {
        return level.name() + ":" + title;
    }

    /**
     * 检查告警是否被抑制
     */
    private boolean isAlertSuppressed(String alertKey) {
        LocalDateTime lastAlertTime = alertSuppressionCache.get(alertKey);
        if (lastAlertTime == null) {
            return false;
        }
        
        long suppressionMinutes = monitoringConfig.getAlert().getSuppression().getCooldownPeriod().toMinutes();
        return lastAlertTime.plusMinutes(suppressionMinutes).isAfter(LocalDateTime.now());
    }

    /**
     * 记录告警抑制
     */
    private void recordAlertSuppression(String alertKey) {
        alertSuppressionCache.put(alertKey, LocalDateTime.now());
        
        // 定期清理过期的抑制记录
        alertExecutor.schedule(() -> {
            cleanupExpiredSuppressions();
        }, 1, TimeUnit.HOURS);
    }

    /**
     * 清理过期的抑制记录
     */
    private void cleanupExpiredSuppressions() {
        LocalDateTime now = LocalDateTime.now();
        long suppressionMinutes = monitoringConfig.getAlert().getSuppression().getCooldownPeriod().toMinutes();
        
        alertSuppressionCache.entrySet().removeIf(entry -> 
            entry.getValue().plusMinutes(suppressionMinutes).isBefore(now)
        );
        
        log.debug("清理过期告警抑制记录，当前记录数: {}", alertSuppressionCache.size());
    }

    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        INFO("信息"),
        WARNING("警告"),
        ERROR("错误"),
        CRITICAL("严重");
        
        private final String displayName;
        
        AlertLevel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        if (alertExecutor != null && !alertExecutor.isShutdown()) {
            alertExecutor.shutdown();
            log.info("告警管理器已关闭");
        }
    }
}