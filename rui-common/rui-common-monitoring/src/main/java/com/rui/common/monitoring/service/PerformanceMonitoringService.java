package com.rui.common.monitoring.service;

import com.rui.common.monitoring.alert.AlertManager;
import com.rui.common.monitoring.metrics.MetricsCollector;
import com.rui.common.monitoring.properties.MonitoringProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控服务
 * 提供统一的性能监控功能，整合原log模块的性能监控能力
 *
 * @author rui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceMonitoringService {

    private final MetricsCollector metricsCollector;
    private final AlertManager alertManager;
    private final MonitoringProperties monitoringProperties;
    
    // 性能统计缓存
    private final Map<String, PerformanceStats> performanceStatsCache = new ConcurrentHashMap<>();
    
    // 慢操作统计
    private final AtomicLong slowOperationCount = new AtomicLong(0);
    private final AtomicLong totalOperationCount = new AtomicLong(0);

    /**
     * 记录方法性能数据
     * 兼容原log模块的性能监控接口
     */
    public void recordMethodPerformance(String operation, String module, long duration, 
                                      boolean success, Map<String, Object> additionalData) {
        
        totalOperationCount.incrementAndGet();
        
        // 构建性能数据
        Map<String, Object> performanceData = new HashMap<>();
        performanceData.put("operation", operation);
        performanceData.put("module", module);
        performanceData.put("duration", duration);
        performanceData.put("success", success);
        performanceData.put("timestamp", System.currentTimeMillis());
        
        if (additionalData != null) {
            performanceData.putAll(additionalData);
        }
        
        // 记录到指标收集器
        metricsCollector.recordPerformanceMetric(operation, duration, performanceData);
        
        // 更新性能统计
        updatePerformanceStats(operation, duration, success);
        
        // 检查是否为慢操作
        Duration slowThreshold = monitoringProperties.getPerformance().getSlowOperationThreshold();
        if (duration > slowThreshold.toMillis()) {
            handleSlowOperation(operation, module, duration, performanceData);
        }
        
        // 检查错误率告警
        checkErrorRateAlert(operation, success);
    }
    
    /**
     * 处理慢操作
     */
    private void handleSlowOperation(String operation, String module, long duration, Map<String, Object> performanceData) {
        slowOperationCount.incrementAndGet();
        
        // 记录慢操作指标
        metricsCollector.recordSlowOperation(operation, module, duration, performanceData);
        
        // 发送慢操作告警
        Map<String, Object> alertContext = new HashMap<>(performanceData);
        alertContext.put("slowThreshold", monitoringProperties.getPerformance().getSlowOperationThreshold().toMillis());
        
        String alertMessage = String.format("检测到慢操作: %s.%s 执行时间 %dms，超过阈值 %dms", 
            module, operation, duration, monitoringProperties.getPerformance().getSlowOperationThreshold().toMillis());
        
        alertManager.sendPerformanceAlert(alertMessage, alertContext);
        
        log.warn("慢操作告警: operation={}, module={}, duration={}ms", operation, module, duration);
    }
    
    /**
     * 更新性能统计
     */
    private void updatePerformanceStats(String operation, long duration, boolean success) {
        performanceStatsCache.compute(operation, (key, stats) -> {
            if (stats == null) {
                stats = new PerformanceStats();
            }
            stats.addExecution(duration, success);
            return stats;
        });
    }
    
    /**
     * 检查错误率告警
     */
    private void checkErrorRateAlert(String operation, boolean success) {
        PerformanceStats stats = performanceStatsCache.get(operation);
        if (stats != null && stats.getTotalCount() >= 10) { // 至少10次调用才检查错误率
            double errorRate = stats.getErrorRate();
            double threshold = monitoringProperties.getBusiness().getErrorMonitoring().getErrorRateThreshold();
            
            if (errorRate > threshold) {
                Map<String, Object> alertContext = new HashMap<>();
                alertContext.put("operation", operation);
                alertContext.put("errorRate", errorRate);
                alertContext.put("threshold", threshold);
                alertContext.put("totalCount", stats.getTotalCount());
                alertContext.put("errorCount", stats.getErrorCount());
                
                String alertMessage = String.format("操作 %s 错误率过高: %.2f%% (阈值: %.2f%%)", 
                    operation, errorRate * 100, threshold * 100);
                
                alertManager.sendBusinessAlert(alertMessage, alertContext);
            }
        }
    }
    
    /**
     * 获取性能统计信息
     */
    public Map<String, Object> getPerformanceStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        statistics.put("totalOperations", totalOperationCount.get());
        statistics.put("slowOperations", slowOperationCount.get());
        statistics.put("slowOperationRate", 
            totalOperationCount.get() > 0 ? 
                (double) slowOperationCount.get() / totalOperationCount.get() : 0.0);
        
        // 操作级别统计
        Map<String, Map<String, Object>> operationStats = new HashMap<>();
        performanceStatsCache.forEach((operation, stats) -> {
            Map<String, Object> operationData = new HashMap<>();
            operationData.put("totalCount", stats.getTotalCount());
            operationData.put("errorCount", stats.getErrorCount());
            operationData.put("errorRate", stats.getErrorRate());
            operationData.put("averageDuration", stats.getAverageDuration());
            operationData.put("maxDuration", stats.getMaxDuration());
            operationData.put("minDuration", stats.getMinDuration());
            operationStats.put(operation, operationData);
        });
        statistics.put("operationStats", operationStats);
        
        statistics.put("timestamp", LocalDateTime.now());
        
        return statistics;
    }
    
    /**
     * 清理过期的性能统计数据
     */
    public void cleanupExpiredStats() {
        // 这里可以实现清理逻辑，比如清理超过一定时间的统计数据
        log.debug("清理过期的性能统计数据");
    }
    
    /**
     * 重置性能统计
     */
    public void resetStatistics() {
        performanceStatsCache.clear();
        slowOperationCount.set(0);
        totalOperationCount.set(0);
        log.info("性能统计已重置");
    }
    
    /**
     * 性能统计内部类
     */
    private static class PerformanceStats {
        private long totalCount = 0;
        private long errorCount = 0;
        private long totalDuration = 0;
        private long maxDuration = 0;
        private long minDuration = Long.MAX_VALUE;
        
        public synchronized void addExecution(long duration, boolean success) {
            totalCount++;
            if (!success) {
                errorCount++;
            }
            
            totalDuration += duration;
            maxDuration = Math.max(maxDuration, duration);
            minDuration = Math.min(minDuration, duration);
        }
        
        public long getTotalCount() {
            return totalCount;
        }
        
        public long getErrorCount() {
            return errorCount;
        }
        
        public double getErrorRate() {
            return totalCount > 0 ? (double) errorCount / totalCount : 0.0;
        }
        
        public double getAverageDuration() {
            return totalCount > 0 ? (double) totalDuration / totalCount : 0.0;
        }
        
        public long getMaxDuration() {
            return maxDuration;
        }
        
        public long getMinDuration() {
            return minDuration == Long.MAX_VALUE ? 0 : minDuration;
        }
    }
}