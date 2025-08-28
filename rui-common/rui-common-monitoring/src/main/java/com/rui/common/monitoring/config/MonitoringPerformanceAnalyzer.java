package com.rui.common.monitoring.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 监控性能分析器
 * 负责分析系统性能指标和慢操作
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringPerformanceAnalyzer {

    private final MonitoringConfig monitoringConfig;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, PerformanceRecord> performanceRecords = new ConcurrentHashMap<>();
    private final List<SlowOperationRecord> slowOperations = Collections.synchronizedList(new ArrayList<>());

    @PostConstruct
    public void init() {
        if (monitoringConfig.getPerformance().isEnabled()) {
            startPerformanceAnalysis();
        }
    }

    /**
     * 启动性能分析
     */
    private void startPerformanceAnalysis() {
        // 每分钟分析一次性能数据
        scheduler.scheduleAtFixedRate(this::analyzePerformance, 0, 1, TimeUnit.MINUTES);
        
        // 每5分钟清理过期的慢操作记录
        scheduler.scheduleAtFixedRate(this::cleanupSlowOperations, 0, 5, TimeUnit.MINUTES);
        
        log.info("Performance analysis started");
    }

    /**
     * 记录操作性能
     */
    public void recordOperation(String operationType, String operationName, Duration duration) {
        try {
            String key = operationType + ":" + operationName;
            PerformanceRecord record = performanceRecords.computeIfAbsent(key, 
                k -> new PerformanceRecord(operationType, operationName));
            
            record.addDuration(duration);
            
            // 检查是否为慢操作
            if (isSlowOperation(operationType, duration)) {
                recordSlowOperation(operationType, operationName, duration);
            }
            
        } catch (Exception e) {
            log.error("Failed to record operation performance", e);
        }
    }

    /**
     * 判断是否为慢操作
     */
    private boolean isSlowOperation(String operationType, Duration duration) {
        Duration threshold;
        
        switch (operationType.toLowerCase()) {
            case "http":
                threshold = monitoringConfig.getPerformance().getSlowOperation().getHttpRequestThreshold();
                break;
            case "database":
                threshold = monitoringConfig.getPerformance().getSlowOperation().getDatabaseQueryThreshold();
                break;
            case "redis":
                threshold = monitoringConfig.getPerformance().getSlowOperation().getRedisOperationThreshold();
                break;
            default:
                threshold = monitoringConfig.getPerformance().getSlowOperationThreshold();
                break;
        }
        
        return duration.compareTo(threshold) > 0;
    }

    /**
     * 记录慢操作
     */
    private void recordSlowOperation(String operationType, String operationName, Duration duration) {
        SlowOperationRecord slowOp = new SlowOperationRecord(
            operationType, operationName, duration, LocalDateTime.now()
        );
        
        slowOperations.add(slowOp);
        
        // 限制慢操作记录数量
        if (slowOperations.size() > 1000) {
            slowOperations.subList(0, 100).clear();
        }
        
        log.warn("Slow operation detected: {} {} took {}", 
            operationType, operationName, duration);
    }

    /**
     * 分析性能数据
     */
    private void analyzePerformance() {
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            // 分析平均响应时间
            Map<String, Double> avgResponseTimes = new HashMap<>();
            for (Map.Entry<String, PerformanceRecord> entry : performanceRecords.entrySet()) {
                PerformanceRecord record = entry.getValue();
                avgResponseTimes.put(entry.getKey(), record.getAverageMillis());
            }
            analysis.put("averageResponseTimes", avgResponseTimes);
            
            // 分析慢操作统计
            Map<String, Long> slowOpCounts = new HashMap<>();
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            
            for (SlowOperationRecord slowOp : slowOperations) {
                if (slowOp.getTimestamp().isAfter(oneHourAgo)) {
                    String key = slowOp.getOperationType();
                    slowOpCounts.merge(key, 1L, Long::sum);
                }
            }
            analysis.put("slowOperationCounts", slowOpCounts);
            
            // 分析系统资源使用
            if (monitoringConfig.getPerformance().isResourceUsageEnabled()) {
                analysis.put("resourceUsage", analyzeResourceUsage());
            }
            
            // 更新到监控配置的运行时指标中
            monitoringConfig.getRuntimeMetrics().put("performanceAnalysis", analysis);
            
            log.debug("Performance analysis completed: {}", analysis);
            
        } catch (Exception e) {
            log.error("Failed to analyze performance", e);
            monitoringConfig.getConfigErrorCount().incrementAndGet();
        }
    }

    /**
     * 分析资源使用情况
     */
    private Map<String, Object> analyzeResourceUsage() {
        Map<String, Object> resourceUsage = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // 内存使用分析
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsagePercentage = (double) usedMemory / maxMemory * 100;
            
            Map<String, Object> memoryUsage = new HashMap<>();
            memoryUsage.put("used", usedMemory);
            memoryUsage.put("max", maxMemory);
            memoryUsage.put("usedPercentage", memoryUsagePercentage);
            resourceUsage.put("memoryUsage", memoryUsage);
            
            // 线程使用分析
            Map<String, Object> threadUsage = new HashMap<>();
            threadUsage.put("activeCount", Thread.activeCount());
            resourceUsage.put("threadUsage", threadUsage);
            
            // 检查资源使用警告
            if (memoryUsagePercentage > 80) {
                log.warn("High memory usage detected: {}%", String.format("%.2f", memoryUsagePercentage));
            }
            
        } catch (Exception e) {
            log.error("Failed to analyze resource usage", e);
        }
        
        return resourceUsage;
    }

    /**
     * 清理过期的慢操作记录
     */
    private void cleanupSlowOperations() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24); // 保留24小时内的记录
            slowOperations.removeIf(record -> record.getTimestamp().isBefore(cutoff));
            
            log.debug("Cleaned up slow operation records, remaining: {}", slowOperations.size());
        } catch (Exception e) {
            log.error("Failed to cleanup slow operations", e);
        }
    }

    /**
     * 获取性能统计
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 操作统计
        Map<String, Map<String, Object>> operationStats = new HashMap<>();
        for (Map.Entry<String, PerformanceRecord> entry : performanceRecords.entrySet()) {
            PerformanceRecord record = entry.getValue();
            Map<String, Object> recordStats = new HashMap<>();
            recordStats.put("count", record.getCount());
            recordStats.put("averageMillis", record.getAverageMillis());
            recordStats.put("minMillis", record.getMinMillis());
            recordStats.put("maxMillis", record.getMaxMillis());
            operationStats.put(entry.getKey(), recordStats);
        }
        stats.put("operations", operationStats);
        
        // 慢操作统计
        stats.put("slowOperationsCount", slowOperations.size());
        stats.put("recentSlowOperations", getRecentSlowOperations(10));
        
        return stats;
    }

    /**
     * 获取最近的慢操作
     */
    public List<Map<String, Object>> getRecentSlowOperations(int limit) {
        return slowOperations.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .map(record -> {
                Map<String, Object> map = new HashMap<>();
                map.put("operationType", record.getOperationType());
                map.put("operationName", record.getOperationName());
                map.put("duration", record.getDuration().toMillis());
                map.put("timestamp", record.getTimestamp());
                return map;
            })
            .toList();
    }

    /**
     * 停止性能分析
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
        log.info("Performance analysis stopped");
    }

    /**
     * 性能记录
     */
    private static class PerformanceRecord {
        private final String operationType;
        private final String operationName;
        private long count = 0;
        private long totalMillis = 0;
        private long minMillis = Long.MAX_VALUE;
        private long maxMillis = 0;

        public PerformanceRecord(String operationType, String operationName) {
            this.operationType = operationType;
            this.operationName = operationName;
        }

        public synchronized void addDuration(Duration duration) {
            long millis = duration.toMillis();
            count++;
            totalMillis += millis;
            minMillis = Math.min(minMillis, millis);
            maxMillis = Math.max(maxMillis, millis);
        }

        public long getCount() { return count; }
        public double getAverageMillis() { return count > 0 ? (double) totalMillis / count : 0; }
        public long getMinMillis() { return minMillis == Long.MAX_VALUE ? 0 : minMillis; }
        public long getMaxMillis() { return maxMillis; }
    }

    /**
     * 慢操作记录
     */
    private static class SlowOperationRecord {
        private final String operationType;
        private final String operationName;
        private final Duration duration;
        private final LocalDateTime timestamp;

        public SlowOperationRecord(String operationType, String operationName, Duration duration, LocalDateTime timestamp) {
            this.operationType = operationType;
            this.operationName = operationName;
            this.duration = duration;
            this.timestamp = timestamp;
        }

        public String getOperationType() { return operationType; }
        public String getOperationName() { return operationName; }
        public Duration getDuration() { return duration; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}