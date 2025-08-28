package com.rui.common.monitoring.health;

import com.rui.common.monitoring.config.MonitoringConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 监控健康检查器
 * 检查监控系统的健康状态
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringHealthIndicator implements HealthIndicator {
    
    private final MonitoringConfig monitoringConfig;
    
    @Override
    public Health health() {
        try {
            // 检查监控配置是否启用
            if (!monitoringConfig.isEnabled()) {
                return Health.down()
                    .withDetail("reason", "Monitoring is disabled")
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
            }
            
            // 检查各个监控组件状态
            Map<String, Object> details = new HashMap<>();
            boolean allHealthy = true;
            
            // 检查指标收集
            if (monitoringConfig.getMetrics().isEnabled()) {
                boolean metricsHealthy = checkMetricsHealth();
                details.put("metrics", metricsHealthy ? "UP" : "DOWN");
                allHealthy = allHealthy && metricsHealthy;
            }
            
            // 检查性能监控
            if (monitoringConfig.getPerformance().isEnabled()) {
                boolean performanceHealthy = checkPerformanceHealth();
                details.put("performance", performanceHealthy ? "UP" : "DOWN");
                allHealthy = allHealthy && performanceHealthy;
            }
            
            // 检查业务监控
            if (monitoringConfig.getBusiness().isEnabled()) {
                boolean businessHealthy = checkBusinessHealth();
                details.put("business", businessHealthy ? "UP" : "DOWN");
                allHealthy = allHealthy && businessHealthy;
            }
            
            // 检查告警系统
            if (monitoringConfig.getAlert().isEnabled()) {
                boolean alertHealthy = checkAlertHealth();
                details.put("alert", alertHealthy ? "UP" : "DOWN");
                allHealthy = allHealthy && alertHealthy;
            }
            
            // 添加统计信息
            details.put("configReadCount", monitoringConfig.getConfigReadCount().get());
            details.put("configWriteCount", monitoringConfig.getConfigWriteCount().get());
            details.put("configErrorCount", monitoringConfig.getConfigErrorCount().get());
            details.put("cacheHitCount", monitoringConfig.getCacheHitCount().get());
            details.put("cacheMissCount", monitoringConfig.getCacheMissCount().get());
            details.put("lastHealthCheck", LocalDateTime.now());
            
            // 更新最后健康检查时间
            monitoringConfig.setLastHealthCheck(LocalDateTime.now());
            
            if (allHealthy) {
                return Health.up().withDetails(details).build();
            } else {
                return Health.down().withDetails(details).build();
            }
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("timestamp", LocalDateTime.now())
                .build();
        }
    }
    
    /**
     * 检查指标收集健康状态
     */
    private boolean checkMetricsHealth() {
        try {
            // 检查指标收集是否正常工作
            long errorCount = monitoringConfig.getConfigErrorCount().get();
            long totalCount = monitoringConfig.getConfigReadCount().get() + 
                            monitoringConfig.getConfigWriteCount().get();
            
            // 如果错误率超过10%，认为不健康
            if (totalCount > 0) {
                double errorRate = (double) errorCount / totalCount;
                return errorRate < 0.1;
            }
            
            return true;
        } catch (Exception e) {
            log.warn("Failed to check metrics health", e);
            return false;
        }
    }
    
    /**
     * 检查性能监控健康状态
     */
    private boolean checkPerformanceHealth() {
        try {
            // 检查性能监控组件是否正常
            Map<String, Object> runtimeMetrics = monitoringConfig.getRuntimeMetrics();
            
            // 检查内存使用率
            if (runtimeMetrics.containsKey("memoryUsage")) {
                Map<String, Object> memoryUsage = (Map<String, Object>) runtimeMetrics.get("memoryUsage");
                double usedPercentage = (Double) memoryUsage.get("usedPercentage");
                
                // 如果内存使用率超过90%，认为不健康
                if (usedPercentage > 90.0) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log.warn("Failed to check performance health", e);
            return false;
        }
    }
    
    /**
     * 检查业务监控健康状态
     */
    private boolean checkBusinessHealth() {
        try {
            // 检查业务监控组件是否正常
            // 这里可以添加具体的业务健康检查逻辑
            return true;
        } catch (Exception e) {
            log.warn("Failed to check business health", e);
            return false;
        }
    }
    
    /**
     * 检查告警系统健康状态
     */
    private boolean checkAlertHealth() {
        try {
            // 检查告警系统是否正常
            // 这里可以添加具体的告警系统健康检查逻辑
            return true;
        } catch (Exception e) {
            log.warn("Failed to check alert health", e);
            return false;
        }
    }
}