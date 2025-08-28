package com.rui.common.monitoring.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 监控指标收集器
 * 负责收集各种系统和业务指标
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringMetricsCollector {

    private final MonitoringConfig monitoringConfig;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, Object> collectedMetrics = new HashMap<>();

    @PostConstruct
    public void init() {
        if (monitoringConfig.getMetrics().isEnabled()) {
            startMetricsCollection();
        }
    }

    /**
     * 启动指标收集
     */
    private void startMetricsCollection() {
        // 每30秒收集一次系统指标
        scheduler.scheduleAtFixedRate(this::collectSystemMetrics, 0, 30, TimeUnit.SECONDS);
        
        // 每60秒收集一次业务指标
        scheduler.scheduleAtFixedRate(this::collectBusinessMetrics, 0, 60, TimeUnit.SECONDS);
        
        log.info("Monitoring metrics collection started");
    }

    /**
     * 收集系统指标
     */
    private void collectSystemMetrics() {
        try {
            if (monitoringConfig.getMetrics().isJvmEnabled()) {
                collectJvmMetrics();
            }
            
            if (monitoringConfig.getMetrics().isSystemEnabled()) {
                collectOsMetrics();
            }
            
            log.debug("System metrics collected successfully");
        } catch (Exception e) {
            log.error("Failed to collect system metrics", e);
            monitoringConfig.getConfigErrorCount().incrementAndGet();
        }
    }

    /**
     * 收集JVM指标
     */
    private void collectJvmMetrics() {
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> jvmMetrics = new HashMap<>();
        jvmMetrics.put("memory.used", runtime.totalMemory() - runtime.freeMemory());
        jvmMetrics.put("memory.free", runtime.freeMemory());
        jvmMetrics.put("memory.total", runtime.totalMemory());
        jvmMetrics.put("memory.max", runtime.maxMemory());
        jvmMetrics.put("threads.active", Thread.activeCount());
        
        // 计算内存使用率
        double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100;
        jvmMetrics.put("memory.usage.percentage", memoryUsage);
        
        collectedMetrics.put("jvm", jvmMetrics);
    }

    /**
     * 收集操作系统指标
     */
    private void collectOsMetrics() {
        Map<String, Object> osMetrics = new HashMap<>();
        
        // 获取可用处理器数量
        osMetrics.put("processors.available", Runtime.getRuntime().availableProcessors());
        
        // 获取系统负载（如果可用）
        try {
            java.lang.management.OperatingSystemMXBean osBean = 
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            osMetrics.put("load.average", osBean.getSystemLoadAverage());
        } catch (Exception e) {
            log.debug("System load average not available", e);
        }
        
        collectedMetrics.put("os", osMetrics);
    }

    /**
     * 收集业务指标
     */
    private void collectBusinessMetrics() {
        try {
            Map<String, Object> businessMetrics = new HashMap<>();
            
            // 配置操作统计
            businessMetrics.put("config.read.count", monitoringConfig.getConfigReadCount().get());
            businessMetrics.put("config.write.count", monitoringConfig.getConfigWriteCount().get());
            businessMetrics.put("config.error.count", monitoringConfig.getConfigErrorCount().get());
            
            // 缓存统计
            businessMetrics.put("cache.hit.count", monitoringConfig.getCacheHitCount().get());
            businessMetrics.put("cache.miss.count", monitoringConfig.getCacheMissCount().get());
            
            // 计算缓存命中率
            long totalCacheAccess = monitoringConfig.getCacheHitCount().get() + monitoringConfig.getCacheMissCount().get();
            if (totalCacheAccess > 0) {
                double hitRate = (double) monitoringConfig.getCacheHitCount().get() / totalCacheAccess * 100;
                businessMetrics.put("cache.hit.rate", hitRate);
            }
            
            collectedMetrics.put("business", businessMetrics);
            
            log.debug("Business metrics collected successfully");
        } catch (Exception e) {
            log.error("Failed to collect business metrics", e);
            monitoringConfig.getConfigErrorCount().incrementAndGet();
        }
    }

    /**
     * 获取收集的指标
     */
    public Map<String, Object> getCollectedMetrics() {
        return new HashMap<>(collectedMetrics);
    }

    /**
     * 获取特定类型的指标
     */
    public Map<String, Object> getMetricsByType(String type) {
        Object metrics = collectedMetrics.get(type);
        if (metrics instanceof Map) {
            return new HashMap<>((Map<String, Object>) metrics);
        }
        return new HashMap<>();
    }

    /**
     * 清除收集的指标
     */
    public void clearMetrics() {
        collectedMetrics.clear();
        log.info("Collected metrics cleared");
    }

    /**
     * 停止指标收集
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
        log.info("Monitoring metrics collection stopped");
    }
}