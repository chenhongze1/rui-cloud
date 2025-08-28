package com.rui.common.monitoring.endpoint;

import com.rui.common.monitoring.config.MonitoringConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 监控管理端点
 * 提供监控相关的管理接口
 *
 * @author rui
 */
@Endpoint(id = "monitoring-management")
@RequiredArgsConstructor
public class MonitoringManagementEndpoint {
    
    private final MonitoringConfig monitoringConfig;
    
    /**
     * 获取监控摘要
     */
    @ReadOperation
    public Map<String, Object> getMonitoringSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // 基本监控状态
        summary.put("enabled", monitoringConfig.isEnabled());
        summary.put("lastHealthCheck", monitoringConfig.getLastHealthCheck());
        
        // 指标统计
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("configReadCount", monitoringConfig.getConfigReadCount().get());
        metrics.put("configWriteCount", monitoringConfig.getConfigWriteCount().get());
        metrics.put("configErrorCount", monitoringConfig.getConfigErrorCount().get());
        metrics.put("cacheHitCount", monitoringConfig.getCacheHitCount().get());
        metrics.put("cacheMissCount", monitoringConfig.getCacheMissCount().get());
        summary.put("metrics", metrics);
        
        // 配置状态
        Map<String, Object> configs = new HashMap<>();
        configs.put("metricsEnabled", monitoringConfig.getMetrics().isEnabled());
        configs.put("healthEnabled", monitoringConfig.getHealth().isEnabled());
        configs.put("performanceEnabled", monitoringConfig.getPerformance().isEnabled());
        configs.put("businessEnabled", monitoringConfig.getBusiness().isEnabled());
        configs.put("alertEnabled", monitoringConfig.getAlert().isEnabled());
        summary.put("configs", configs);
        
        // 运行时指标
        summary.put("runtimeMetrics", monitoringConfig.getRuntimeMetrics());
        
        return summary;
    }
    
    /**
     * 获取健康检查状态
     */
    @ReadOperation
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Health health = monitoringConfig.health();
            result.put("status", health.getStatus().getCode());
            result.put("details", health.getDetails());
            result.put("timestamp", LocalDateTime.now());
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }
    
    /**
     * 刷新运行时指标
     */
    @WriteOperation
    public Map<String, Object> refreshRuntimeMetrics() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            monitoringConfig.updateRuntimeMetrics();
            result.put("success", true);
            result.put("message", "Runtime metrics refreshed successfully");
            result.put("timestamp", LocalDateTime.now());
            result.put("runtimeMetrics", monitoringConfig.getRuntimeMetrics());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to refresh runtime metrics: " + e.getMessage());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }
    
    /**
     * 重置计数器
     */
    @WriteOperation
    public Map<String, Object> resetCounters(@NotBlank String counterType) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            switch (counterType.toLowerCase()) {
                case "config":
                    monitoringConfig.getConfigReadCount().set(0);
                    monitoringConfig.getConfigWriteCount().set(0);
                    monitoringConfig.getConfigErrorCount().set(0);
                    break;
                case "cache":
                    monitoringConfig.getCacheHitCount().set(0);
                    monitoringConfig.getCacheMissCount().set(0);
                    break;
                case "all":
                    monitoringConfig.getConfigReadCount().set(0);
                    monitoringConfig.getConfigWriteCount().set(0);
                    monitoringConfig.getConfigErrorCount().set(0);
                    monitoringConfig.getCacheHitCount().set(0);
                    monitoringConfig.getCacheMissCount().set(0);
                    break;
                default:
                    result.put("success", false);
                    result.put("message", "Unknown counter type: " + counterType);
                    return result;
            }
            
            result.put("success", true);
            result.put("message", "Counters reset successfully: " + counterType);
            result.put("timestamp", LocalDateTime.now());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to reset counters: " + e.getMessage());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }
}