package com.rui.common.core.monitoring;

import com.rui.common.core.config.MonitoringConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 告警规则引擎
 * 处理告警规则的匹配和触发
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRuleEngine {

    private final MonitoringConfig monitoringConfig;
    private final AlertManager alertManager;
    
    // 规则状态缓存
    private final Map<String, RuleState> ruleStateCache = new ConcurrentHashMap<>();

    /**
     * 检查CPU使用率规则
     */
    public void checkCpuUsageRule(double cpuUsage) {
        List<MonitoringConfig.AlertRule> rules = monitoringConfig.getAlert().getRules();
        
        for (MonitoringConfig.AlertRule rule : rules) {
            if ("cpu_usage".equals(rule.getMetric())) {
                checkRule(rule, cpuUsage, "CPU使用率", 
                    value -> value > rule.getThreshold());
            }
        }
    }

    /**
     * 检查内存使用率规则
     */
    public void checkMemoryUsageRule(double memoryUsage) {
        List<MonitoringConfig.AlertRule> rules = monitoringConfig.getAlert().getRules();
        
        for (MonitoringConfig.AlertRule rule : rules) {
            if ("memory_usage".equals(rule.getMetric())) {
                checkRule(rule, memoryUsage, "内存使用率", 
                    value -> value > rule.getThreshold());
            }
        }
    }

    /**
     * 检查磁盘使用率规则
     */
    public void checkDiskUsageRule(double diskUsage) {
        List<MonitoringConfig.AlertRule> rules = monitoringConfig.getAlert().getRules();
        
        for (MonitoringConfig.AlertRule rule : rules) {
            if ("disk_usage".equals(rule.getMetric())) {
                checkRule(rule, diskUsage, "磁盘使用率", 
                    value -> value > rule.getThreshold());
            }
        }
    }

    /**
     * 检查响应时间规则
     */
    public void checkResponseTimeRule(double responseTime) {
        List<MonitoringConfig.AlertRule> rules = monitoringConfig.getAlert().getRules();
        
        for (MonitoringConfig.AlertRule rule : rules) {
            if ("response_time".equals(rule.getMetric())) {
                checkRule(rule, responseTime, "响应时间", 
                    value -> value > rule.getThreshold());
            }
        }
    }

    /**
     * 检查错误率规则
     */
    public void checkErrorRateRule(double errorRate) {
        List<MonitoringConfig.AlertRule> rules = monitoringConfig.getAlert().getRules();
        
        for (MonitoringConfig.AlertRule rule : rules) {
            if ("error_rate".equals(rule.getMetric())) {
                checkRule(rule, errorRate, "错误率", 
                    value -> value > rule.getThreshold());
            }
        }
    }

    /**
     * 检查数据库连接数规则
     */
    public void checkDatabaseConnectionRule(int connectionCount) {
        List<MonitoringConfig.AlertRule> rules = monitoringConfig.getAlert().getRules();
        
        for (MonitoringConfig.AlertRule rule : rules) {
            if ("database_connections".equals(rule.getMetric())) {
                checkRule(rule, connectionCount, "数据库连接数", 
                    value -> value > rule.getThreshold());
            }
        }
    }

    /**
     * 检查Redis连接数规则
     */
    public void checkRedisConnectionRule(int connectionCount) {
        List<MonitoringConfig.AlertRule> rules = monitoringConfig.getAlert().getRules();
        
        for (MonitoringConfig.AlertRule rule : rules) {
            if ("redis_connections".equals(rule.getMetric())) {
                checkRule(rule, connectionCount, "Redis连接数", 
                    value -> value > rule.getThreshold());
            }
        }
    }

    /**
     * 检查活跃用户数规则
     */
    public void checkActiveUserRule(int activeUserCount) {
        List<MonitoringConfig.AlertRule> rules = monitoringConfig.getAlert().getRules();
        
        for (MonitoringConfig.AlertRule rule : rules) {
            if ("active_users".equals(rule.getMetric())) {
                checkRule(rule, activeUserCount, "活跃用户数", 
                    value -> value > rule.getThreshold());
            }
        }
    }

    /**
     * 检查业务指标规则
     */
    public void checkBusinessMetricRule(String metricName, double value) {
        List<MonitoringConfig.AlertRule> rules = monitoringConfig.getAlert().getRules();
        
        for (MonitoringConfig.AlertRule rule : rules) {
            if (metricName.equals(rule.getMetric())) {
                checkRule(rule, value, metricName, 
                    val -> evaluateCondition(val, rule.getCondition(), rule.getThreshold()));
            }
        }
    }

    /**
     * 通用规则检查方法
     */
    private void checkRule(MonitoringConfig.AlertRule rule, double currentValue, 
                          String metricDisplayName, Predicate<Double> condition) {
        try {
            String ruleKey = rule.getName();
            RuleState ruleState = ruleStateCache.computeIfAbsent(ruleKey, k -> new RuleState());
            
            boolean isTriggered = condition.test(currentValue);
            
            if (isTriggered) {
                ruleState.incrementTriggerCount();
                
                // 检查是否达到连续触发次数
                if (ruleState.getTriggerCount() >= rule.getConsecutiveCount()) {
                    // 发送告警
                    sendRuleAlert(rule, currentValue, metricDisplayName);
                    
                    // 重置计数器
                    ruleState.resetTriggerCount();
                }
            } else {
                // 重置计数器
                ruleState.resetTriggerCount();
            }
            
            ruleState.setLastCheckValue(currentValue);
            
        } catch (Exception e) {
            log.error("规则检查失败: rule={}, value={}", rule.getName(), currentValue, e);
        }
    }

    /**
     * 发送规则告警
     */
    private void sendRuleAlert(MonitoringConfig.AlertRule rule, double currentValue, String metricDisplayName) {
        AlertManager.AlertLevel level = AlertManager.AlertLevel.valueOf(rule.getLevel().toUpperCase());
        
        String title = String.format("%s告警", metricDisplayName);
        String message = String.format("%s当前值%.2f，超过阈值%.2f", 
            metricDisplayName, currentValue, rule.getThreshold());
        
        Map<String, Object> context = new HashMap<>();
        context.put("ruleName", rule.getName());
        context.put("metric", rule.getMetric());
        context.put("currentValue", currentValue);
        context.put("threshold", rule.getThreshold());
        context.put("condition", rule.getCondition());
        context.put("consecutiveCount", rule.getConsecutiveCount());
        
        alertManager.sendAlert(level, title, message, context);
        
        log.warn("规则告警触发: rule={}, currentValue={}, threshold={}", 
            rule.getName(), currentValue, rule.getThreshold());
    }

    /**
     * 评估条件
     */
    private boolean evaluateCondition(double value, String condition, double threshold) {
        switch (condition.toLowerCase()) {
            case "gt":
            case ">":
                return value > threshold;
            case "gte":
            case ">=":
                return value >= threshold;
            case "lt":
            case "<":
                return value < threshold;
            case "lte":
            case "<=":
                return value <= threshold;
            case "eq":
            case "=":
                return Math.abs(value - threshold) < 0.001; // 浮点数比较
            case "ne":
            case "!=":
                return Math.abs(value - threshold) >= 0.001;
            default:
                log.warn("未知的条件操作符: {}", condition);
                return false;
        }
    }

    /**
     * 获取规则状态
     */
    public Map<String, Object> getRuleStates() {
        Map<String, Object> states = new HashMap<>();
        
        ruleStateCache.forEach((ruleKey, ruleState) -> {
            Map<String, Object> stateInfo = new HashMap<>();
            stateInfo.put("triggerCount", ruleState.getTriggerCount());
            stateInfo.put("lastCheckValue", ruleState.getLastCheckValue());
            states.put(ruleKey, stateInfo);
        });
        
        return states;
    }

    /**
     * 清理规则状态
     */
    public void clearRuleStates() {
        ruleStateCache.clear();
        log.info("规则状态已清理");
    }

    /**
     * 规则状态类
     */
    private static class RuleState {
        private int triggerCount = 0;
        private double lastCheckValue = 0.0;
        
        public int getTriggerCount() {
            return triggerCount;
        }
        
        public void incrementTriggerCount() {
            this.triggerCount++;
        }
        
        public void resetTriggerCount() {
            this.triggerCount = 0;
        }
        
        public double getLastCheckValue() {
            return lastCheckValue;
        }
        
        public void setLastCheckValue(double lastCheckValue) {
            this.lastCheckValue = lastCheckValue;
        }
    }
}