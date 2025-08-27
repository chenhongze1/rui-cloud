package com.rui.common.core.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义指标端点
 * 提供详细的指标信息
 *
 * @author rui
 */
@Slf4j
@Component
@Endpoint(id = "custom-metrics")
@RequiredArgsConstructor
public class CustomMetricsEndpoint {

    private final MetricsCollector metricsCollector;

    /**
     * 获取所有指标概览
     */
    @ReadOperation
    public Map<String, Object> metrics() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", LocalDateTime.now());
            
            // JVM指标
            Map<String, Object> jvmMetrics = new HashMap<>();
            jvmMetrics.put("memory", getJvmMemoryMetrics());
            jvmMetrics.put("gc", getJvmGcMetrics());
            jvmMetrics.put("threads", getJvmThreadMetrics());
            result.put("jvm", jvmMetrics);
            
            // 系统指标
            Map<String, Object> systemMetrics = new HashMap<>();
            systemMetrics.put("cpu", getSystemCpuMetrics());
            systemMetrics.put("disk", getSystemDiskMetrics());
            result.put("system", systemMetrics);
            
            // HTTP指标
            result.put("http", getHttpMetrics());
            
            // 数据库指标
            result.put("database", getDatabaseMetrics());
            
            // Redis指标
            result.put("redis", getRedisMetrics());
            
            // 业务指标
            result.put("business", getBusinessMetrics());
            
        } catch (Exception e) {
            log.error("获取指标失败", e);
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 获取指定类型的指标
     */
    @ReadOperation
    public Map<String, Object> metricsByType(@Selector String type) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", LocalDateTime.now());
            result.put("type", type);
            
            switch (type.toLowerCase()) {
                case "jvm":
                    result.put("data", getJvmMetrics());
                    break;
                case "system":
                    result.put("data", getSystemMetrics());
                    break;
                case "http":
                    result.put("data", getHttpMetrics());
                    break;
                case "database":
                    result.put("data", getDatabaseMetrics());
                    break;
                case "redis":
                    result.put("data", getRedisMetrics());
                    break;
                case "business":
                    result.put("data", getBusinessMetrics());
                    break;
                default:
                    result.put("error", "未知的指标类型: " + type);
            }
            
        } catch (Exception e) {
            log.error("获取指标失败: {}", type, e);
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 获取指标统计信息
     */
    @ReadOperation
    public Map<String, Object> summary() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", LocalDateTime.now());
            
            // 统计各类型指标数量
            Map<String, Integer> counts = new HashMap<>();
            counts.put("counters", getCounterCount());
            counts.put("gauges", getGaugeCount());
            counts.put("timers", getTimerCount());
            counts.put("total", getTotalMeterCount());
            result.put("counts", counts);
            
            // 最近活跃的指标
            result.put("recentActive", getRecentActiveMetrics());
            
        } catch (Exception e) {
            log.error("获取指标统计失败", e);
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 获取JVM内存指标
     */
    private Map<String, Object> getJvmMemoryMetrics() {
        Map<String, Object> memory = new HashMap<>();
        
        // 从MeterRegistry获取JVM内存指标
        Gauge heapUsed = metricsCollector.getMeterRegistry().find("jvm.memory.used")
            .tag("area", "heap")
            .gauge();
        if (heapUsed != null) {
            memory.put("heapUsed", heapUsed.value());
        }
        
        Gauge heapMax = metricsCollector.getMeterRegistry().find("jvm.memory.max")
            .tag("area", "heap")
            .gauge();
        if (heapMax != null) {
            memory.put("heapMax", heapMax.value());
        }
        
        return memory;
    }

    /**
     * 获取JVM GC指标
     */
    private Map<String, Object> getJvmGcMetrics() {
        Map<String, Object> gc = new HashMap<>();
        
        Timer gcPause = metricsCollector.getMeterRegistry().find("jvm.gc.pause")
            .timer();
        if (gcPause != null) {
            gc.put("pauseCount", gcPause.count());
            gc.put("pauseTotalTime", gcPause.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS));
        }
        
        return gc;
    }

    /**
     * 获取JVM线程指标
     */
    private Map<String, Object> getJvmThreadMetrics() {
        Map<String, Object> threads = new HashMap<>();
        
        Gauge liveThreads = metricsCollector.getMeterRegistry().find("jvm.threads.live")
            .gauge();
        if (liveThreads != null) {
            threads.put("live", liveThreads.value());
        }
        
        return threads;
    }

    /**
     * 获取系统CPU指标
     */
    private Map<String, Object> getSystemCpuMetrics() {
        Map<String, Object> cpu = new HashMap<>();
        
        Gauge systemCpu = metricsCollector.getMeterRegistry().find("system.cpu.usage")
            .gauge();
        if (systemCpu != null) {
            cpu.put("usage", systemCpu.value());
        }
        
        return cpu;
    }

    /**
     * 获取系统磁盘指标
     */
    private Map<String, Object> getSystemDiskMetrics() {
        Map<String, Object> disk = new HashMap<>();
        
        Gauge diskFree = metricsCollector.getMeterRegistry().find("disk.free")
            .gauge();
        if (diskFree != null) {
            disk.put("free", diskFree.value());
        }
        
        return disk;
    }

    /**
     * 获取JVM指标
     */
    private Map<String, Object> getJvmMetrics() {
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("memory", getJvmMemoryMetrics());
        jvm.put("gc", getJvmGcMetrics());
        jvm.put("threads", getJvmThreadMetrics());
        return jvm;
    }

    /**
     * 获取系统指标
     */
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> system = new HashMap<>();
        system.put("cpu", getSystemCpuMetrics());
        system.put("disk", getSystemDiskMetrics());
        return system;
    }

    /**
     * 获取HTTP指标
     */
    private Map<String, Object> getHttpMetrics() {
        Map<String, Object> http = new HashMap<>();
        
        Timer httpRequests = metricsCollector.getMeterRegistry().find("http.server.requests")
            .timer();
        if (httpRequests != null) {
            http.put("requestCount", httpRequests.count());
            http.put("requestTotalTime", httpRequests.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS));
            http.put("requestMeanTime", httpRequests.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        }
        
        return http;
    }

    /**
     * 获取数据库指标
     */
    private Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> database = new HashMap<>();
        
        Timer dbQueries = metricsCollector.getMeterRegistry().find("database.query")
            .timer();
        if (dbQueries != null) {
            database.put("queryCount", dbQueries.count());
            database.put("queryTotalTime", dbQueries.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS));
            database.put("queryMeanTime", dbQueries.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        }
        
        return database;
    }

    /**
     * 获取Redis指标
     */
    private Map<String, Object> getRedisMetrics() {
        Map<String, Object> redis = new HashMap<>();
        
        Timer redisOps = metricsCollector.getMeterRegistry().find("redis.operation")
            .timer();
        if (redisOps != null) {
            redis.put("operationCount", redisOps.count());
            redis.put("operationTotalTime", redisOps.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS));
            redis.put("operationMeanTime", redisOps.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        }
        
        return redis;
    }

    /**
     * 获取业务指标
     */
    private Map<String, Object> getBusinessMetrics() {
        Map<String, Object> business = new HashMap<>();
        
        Counter userLogins = metricsCollector.getMeterRegistry().find("user.login")
            .counter();
        if (userLogins != null) {
            business.put("userLogins", userLogins.count());
        }
        
        Gauge activeUsers = metricsCollector.getMeterRegistry().find("user.active")
            .gauge();
        if (activeUsers != null) {
            business.put("activeUsers", activeUsers.value());
        }
        
        return business;
    }

    /**
     * 获取计数器数量
     */
    private int getCounterCount() {
        return (int) metricsCollector.getMeterRegistry().getMeters().stream()
            .filter(meter -> meter instanceof Counter)
            .count();
    }

    /**
     * 获取仪表数量
     */
    private int getGaugeCount() {
        return (int) metricsCollector.getMeterRegistry().getMeters().stream()
            .filter(meter -> meter instanceof Gauge)
            .count();
    }

    /**
     * 获取计时器数量
     */
    private int getTimerCount() {
        return (int) metricsCollector.getMeterRegistry().getMeters().stream()
            .filter(meter -> meter instanceof Timer)
            .count();
    }

    /**
     * 获取总指标数量
     */
    private int getTotalMeterCount() {
        return metricsCollector.getMeterRegistry().getMeters().size();
    }

    /**
     * 获取最近活跃的指标
     */
    private List<String> getRecentActiveMetrics() {
        return metricsCollector.getMeterRegistry().getMeters().stream()
            .map(Meter::getId)
            .map(Meter.Id::getName)
            .limit(10)
            .collect(Collectors.toList());
    }
}