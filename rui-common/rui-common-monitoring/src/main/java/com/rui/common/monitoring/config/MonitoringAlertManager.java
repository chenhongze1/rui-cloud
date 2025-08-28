package com.rui.common.monitoring.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控告警管理器
 * 负责监控系统状态并发送告警
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringAlertManager {

    private final MonitoringConfig monitoringConfig;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final List<AlertRecord> alertHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, LocalDateTime> alertCooldowns = new ConcurrentHashMap<>();
    private final AtomicLong alertCounter = new AtomicLong(0);

    @PostConstruct
    public void init() {
        if (monitoringConfig.getAlert().isEnabled()) {
            initializeDefaultAlertRules();
            startAlertMonitoring();
        }
    }

    /**
     * 初始化默认告警规则
     */
    private void initializeDefaultAlertRules() {
        // 内存使用率告警
        addAlertRule("memory_usage", "Memory Usage High", 
            () -> getMemoryUsagePercentage() > 85.0,
            AlertLevel.WARNING, Duration.ofMinutes(5));
        
        // 错误率告警
        addAlertRule("error_rate", "Error Rate High", 
            () -> getErrorRate() > 0.05, // 5%
            AlertLevel.CRITICAL, Duration.ofMinutes(2));
        
        // 慢操作告警
        addAlertRule("slow_operations", "Too Many Slow Operations", 
            () -> getRecentSlowOperationsCount() > 10,
            AlertLevel.WARNING, Duration.ofMinutes(10));
        
        // 配置错误告警
        addAlertRule("config_errors", "Configuration Errors", 
            () -> monitoringConfig.getConfigErrorCount().get() > 0,
            AlertLevel.ERROR, Duration.ofMinutes(1));
        
        log.info("Initialized {} default alert rules", alertRules.size());
    }

    /**
     * 启动告警监控
     */
    private void startAlertMonitoring() {
        // 每30秒检查一次告警规则
        scheduler.scheduleAtFixedRate(this::checkAlertRules, 0, 30, TimeUnit.SECONDS);
        
        // 每小时清理一次告警历史
        scheduler.scheduleAtFixedRate(this::cleanupAlertHistory, 0, 1, TimeUnit.HOURS);
        
        log.info("Alert monitoring started");
    }

    /**
     * 添加告警规则
     */
    public void addAlertRule(String ruleId, String description, AlertCondition condition, 
                           AlertLevel level, Duration cooldown) {
        AlertRule rule = new AlertRule(ruleId, description, condition, level, cooldown);
        alertRules.put(ruleId, rule);
        log.debug("Added alert rule: {}", ruleId);
    }

    /**
     * 移除告警规则
     */
    public void removeAlertRule(String ruleId) {
        alertRules.remove(ruleId);
        alertCooldowns.remove(ruleId);
        log.debug("Removed alert rule: {}", ruleId);
    }

    /**
     * 检查告警规则
     */
    private void checkAlertRules() {
        try {
            for (AlertRule rule : alertRules.values()) {
                checkSingleRule(rule);
            }
        } catch (Exception e) {
            log.error("Failed to check alert rules", e);
        }
    }

    /**
     * 检查单个告警规则
     */
    private void checkSingleRule(AlertRule rule) {
        try {
            // 检查冷却时间
            LocalDateTime lastAlert = alertCooldowns.get(rule.getRuleId());
            if (lastAlert != null && 
                LocalDateTime.now().isBefore(lastAlert.plus(rule.getCooldown()))) {
                return; // 还在冷却期内
            }

            // 检查告警条件
            if (rule.getCondition().check()) {
                triggerAlert(rule);
            }
        } catch (Exception e) {
            log.error("Failed to check alert rule: {}", rule.getRuleId(), e);
        }
    }

    /**
     * 触发告警
     */
    private void triggerAlert(AlertRule rule) {
        LocalDateTime now = LocalDateTime.now();
        long alertId = alertCounter.incrementAndGet();
        
        AlertRecord alert = new AlertRecord(
            alertId, rule.getRuleId(), rule.getDescription(), 
            rule.getLevel(), now, generateAlertDetails(rule)
        );
        
        // 记录告警
        alertHistory.add(alert);
        alertCooldowns.put(rule.getRuleId(), now);
        
        // 发送告警
        sendAlert(alert);
        
        // 更新统计
        monitoringConfig.getRuntimeMetrics().merge("alertCount", 1L, (old, val) -> (Long) old + (Long) val);
        
        log.warn("Alert triggered: {} - {}", rule.getRuleId(), rule.getDescription());
    }

    /**
     * 生成告警详情
     */
    private Map<String, Object> generateAlertDetails(AlertRule rule) {
        Map<String, Object> details = new HashMap<>();
        
        switch (rule.getRuleId()) {
            case "memory_usage":
                details.put("memoryUsagePercentage", getMemoryUsagePercentage());
                details.put("threshold", 85.0);
                break;
            case "error_rate":
                details.put("errorRate", getErrorRate());
                details.put("threshold", 0.05);
                break;
            case "slow_operations":
                details.put("slowOperationsCount", getRecentSlowOperationsCount());
                details.put("threshold", 10);
                break;
            case "config_errors":
                details.put("configErrorCount", monitoringConfig.getConfigErrorCount().get());
                break;
        }
        
        return details;
    }

    /**
     * 发送告警
     */
    private void sendAlert(AlertRecord alert) {
        try {
            // 根据配置选择告警方式
            if (monitoringConfig.getAlert().getChannels().getEmail().isEnabled()) {
                sendEmailAlert(alert);
            }
            
            if (monitoringConfig.getAlert().getChannels().getWebhook().isEnabled()) {
                sendWebhookAlert(alert);
            }
            
            // 记录到日志
            logAlert(alert);
            
        } catch (Exception e) {
            log.error("Failed to send alert: {}", alert.getRuleId(), e);
        }
    }

    /**
     * 发送邮件告警
     */
    private void sendEmailAlert(AlertRecord alert) {
        // 这里应该集成邮件发送服务
        log.info("Email alert would be sent: {} - {}", alert.getRuleId(), alert.getDescription());
    }

    /**
     * 发送Webhook告警
     */
    private void sendWebhookAlert(AlertRecord alert) {
        // 这里应该发送HTTP请求到配置的Webhook URL
        log.info("Webhook alert would be sent: {} - {}", alert.getRuleId(), alert.getDescription());
    }

    /**
     * 记录告警到日志
     */
    private void logAlert(AlertRecord alert) {
        String logLevel = alert.getLevel().name();
        String message = String.format("ALERT [%s] %s - %s (ID: %d)", 
            logLevel, alert.getRuleId(), alert.getDescription(), alert.getAlertId());
        
        switch (alert.getLevel()) {
            case CRITICAL:
                log.error(message);
                break;
            case ERROR:
                log.error(message);
                break;
            case WARNING:
                log.warn(message);
                break;
            case INFO:
                log.info(message);
                break;
        }
    }

    /**
     * 获取内存使用率
     */
    private double getMemoryUsagePercentage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        return (double) usedMemory / maxMemory * 100;
    }

    /**
     * 获取错误率
     */
    private double getErrorRate() {
        // 从运行时指标中获取错误率
        Object errorCount = monitoringConfig.getRuntimeMetrics().get("errorCount");
        Object totalCount = monitoringConfig.getRuntimeMetrics().get("totalCount");
        
        if (errorCount instanceof Number && totalCount instanceof Number) {
            long errors = ((Number) errorCount).longValue();
            long total = ((Number) totalCount).longValue();
            return total > 0 ? (double) errors / total : 0.0;
        }
        
        return 0.0;
    }

    /**
     * 获取最近慢操作数量
     */
    private int getRecentSlowOperationsCount() {
        // 从运行时指标中获取慢操作数量
        Object slowOpsCount = monitoringConfig.getRuntimeMetrics().get("recentSlowOperationsCount");
        return slowOpsCount instanceof Number ? ((Number) slowOpsCount).intValue() : 0;
    }

    /**
     * 清理告警历史
     */
    private void cleanupAlertHistory() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7); // 保留7天内的告警
            alertHistory.removeIf(alert -> alert.getTimestamp().isBefore(cutoff));
            
            // 清理过期的冷却时间
            alertCooldowns.entrySet().removeIf(entry -> {
                AlertRule rule = alertRules.get(entry.getKey());
                if (rule == null) return true;
                return LocalDateTime.now().isAfter(entry.getValue().plus(rule.getCooldown()));
            });
            
            log.debug("Cleaned up alert history, remaining: {}", alertHistory.size());
        } catch (Exception e) {
            log.error("Failed to cleanup alert history", e);
        }
    }

    /**
     * 获取告警统计
     */
    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总告警数
        stats.put("totalAlerts", alertHistory.size());
        
        // 按级别统计
        Map<String, Long> levelCounts = new HashMap<>();
        for (AlertRecord alert : alertHistory) {
            levelCounts.merge(alert.getLevel().name(), 1L, Long::sum);
        }
        stats.put("alertsByLevel", levelCounts);
        
        // 最近24小时告警
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        long recentAlerts = alertHistory.stream()
            .mapToLong(alert -> alert.getTimestamp().isAfter(oneDayAgo) ? 1 : 0)
            .sum();
        stats.put("recentAlerts", recentAlerts);
        
        // 活跃告警规则
        stats.put("activeRules", alertRules.size());
        
        return stats;
    }

    /**
     * 获取最近告警
     */
    public List<Map<String, Object>> getRecentAlerts(int limit) {
        return alertHistory.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .map(alert -> {
                Map<String, Object> map = new HashMap<>();
                map.put("alertId", alert.getAlertId());
                map.put("ruleId", alert.getRuleId());
                map.put("description", alert.getDescription());
                map.put("level", alert.getLevel().name());
                map.put("timestamp", alert.getTimestamp());
                map.put("details", alert.getDetails());
                return map;
            })
            .toList();
    }

    /**
     * 手动触发告警测试
     */
    public void testAlert(String ruleId) {
        AlertRule rule = alertRules.get(ruleId);
        if (rule != null) {
            // 临时移除冷却时间限制
            alertCooldowns.remove(ruleId);
            triggerAlert(rule);
            log.info("Test alert triggered for rule: {}", ruleId);
        } else {
            log.warn("Alert rule not found: {}", ruleId);
        }
    }

    /**
     * 停止告警监控
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Alert monitoring stopped");
    }

    /**
     * 告警条件接口
     */
    @FunctionalInterface
    public interface AlertCondition {
        boolean check();
    }

    /**
     * 告警级别
     */
    public enum AlertLevel {
        INFO, WARNING, ERROR, CRITICAL
    }

    /**
     * 告警规则
     */
    private static class AlertRule {
        private final String ruleId;
        private final String description;
        private final AlertCondition condition;
        private final AlertLevel level;
        private final Duration cooldown;

        public AlertRule(String ruleId, String description, AlertCondition condition, 
                        AlertLevel level, Duration cooldown) {
            this.ruleId = ruleId;
            this.description = description;
            this.condition = condition;
            this.level = level;
            this.cooldown = cooldown;
        }

        public String getRuleId() { return ruleId; }
        public String getDescription() { return description; }
        public AlertCondition getCondition() { return condition; }
        public AlertLevel getLevel() { return level; }
        public Duration getCooldown() { return cooldown; }
    }

    /**
     * 告警记录
     */
    private static class AlertRecord {
        private final long alertId;
        private final String ruleId;
        private final String description;
        private final AlertLevel level;
        private final LocalDateTime timestamp;
        private final Map<String, Object> details;

        public AlertRecord(long alertId, String ruleId, String description, 
                          AlertLevel level, LocalDateTime timestamp, Map<String, Object> details) {
            this.alertId = alertId;
            this.ruleId = ruleId;
            this.description = description;
            this.level = level;
            this.timestamp = timestamp;
            this.details = details;
        }

        public long getAlertId() { return alertId; }
        public String getRuleId() { return ruleId; }
        public String getDescription() { return description; }
        public AlertLevel getLevel() { return level; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getDetails() { return details; }
    }
}