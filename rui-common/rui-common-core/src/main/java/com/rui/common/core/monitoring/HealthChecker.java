package com.rui.common.core.monitoring;

import com.rui.common.core.config.MonitoringConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 健康检查器
 * 检查各个组件的健康状态
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthChecker {

    private final MonitoringConfig monitoringConfig;
    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 执行全面健康检查
     */
    public Health checkOverallHealth() {
        if (!monitoringConfig.isEnabled() || !monitoringConfig.getHealth().isEnabled()) {
            return Health.up().withDetail("monitoring", "disabled").build();
        }

        Map<String, Health> healthResults = new HashMap<>();
        Health.Builder overallBuilder = Health.up();
        boolean hasDown = false;

        // 检查数据库健康状态
        if (monitoringConfig.getHealth().getEnabledCheckers().contains("database")) {
            Health dbHealth = checkDatabaseHealth();
            healthResults.put("database", dbHealth);
            if (dbHealth.getStatus() == Status.DOWN) {
                hasDown = true;
            }
        }

        // 检查Redis健康状态
        if (monitoringConfig.getHealth().getEnabledCheckers().contains("redis")) {
            Health redisHealth = checkRedisHealth();
            healthResults.put("redis", redisHealth);
            if (redisHealth.getStatus() == Status.DOWN) {
                hasDown = true;
            }
        }

        // 检查磁盘健康状态
        if (monitoringConfig.getHealth().getEnabledCheckers().contains("disk")) {
            Health diskHealth = checkDiskHealth();
            healthResults.put("disk", diskHealth);
            if (diskHealth.getStatus() == Status.DOWN) {
                hasDown = true;
            }
        }

        // 检查内存健康状态
        if (monitoringConfig.getHealth().getEnabledCheckers().contains("memory")) {
            Health memoryHealth = checkMemoryHealth();
            healthResults.put("memory", memoryHealth);
            if (memoryHealth.getStatus() == Status.DOWN) {
                hasDown = true;
            }
        }

        // 检查自定义健康状态
        if (monitoringConfig.getHealth().getEnabledCheckers().contains("custom")) {
            Health customHealth = checkCustomHealth();
            healthResults.put("custom", customHealth);
            if (customHealth.getStatus() == Status.DOWN) {
                hasDown = true;
            }
        }

        // 设置总体状态
        if (hasDown) {
            overallBuilder.down();
        }

        // 添加检查时间
        overallBuilder.withDetail("timestamp", LocalDateTime.now().toString());
        overallBuilder.withDetail("components", healthResults);

        return overallBuilder.build();
    }

    /**
     * 检查数据库健康状态
     */
    public Health checkDatabaseHealth() {
        MonitoringConfig.DatabaseHealthConfig dbConfig = monitoringConfig.getHealth().getDatabase();
        if (!dbConfig.isEnabled()) {
            return Health.up().withDetail("status", "disabled").build();
        }

        try {
            CompletableFuture<Health> future = CompletableFuture.supplyAsync(() -> {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(dbConfig.getValidationQuery());
                     ResultSet resultSet = statement.executeQuery()) {

                    if (resultSet.next()) {
                        return Health.up()
                            .withDetail("database", "available")
                            .withDetail("validationQuery", dbConfig.getValidationQuery())
                            .build();
                    } else {
                        return Health.down()
                            .withDetail("database", "validation query returned no results")
                            .build();
                    }
                } catch (Exception e) {
                    log.error("数据库健康检查失败", e);
                    return Health.down()
                        .withDetail("database", "connection failed")
                        .withDetail("error", e.getMessage())
                        .build();
                }
            });

            return future.get(dbConfig.getTimeout().toSeconds(), TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("数据库健康检查超时或失败", e);
            return Health.down()
                .withDetail("database", "check timeout or failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    /**
     * 检查Redis健康状态
     */
    public Health checkRedisHealth() {
        MonitoringConfig.RedisHealthConfig redisConfig = monitoringConfig.getHealth().getRedis();
        if (!redisConfig.isEnabled()) {
            return Health.up().withDetail("status", "disabled").build();
        }

        try {
            CompletableFuture<Health> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 执行PING命令
                    String pong = redisTemplate.getConnectionFactory().getConnection().ping();
                    
                    if ("PONG".equals(pong)) {
                        // 获取Redis信息
                        Map<String, Object> info = getRedisInfo();
                        
                        return Health.up()
                            .withDetail("redis", "available")
                            .withDetail("ping", pong)
                            .withDetails(info)
                            .build();
                    } else {
                        return Health.down()
                            .withDetail("redis", "ping failed")
                            .withDetail("response", pong)
                            .build();
                    }
                } catch (Exception e) {
                    log.error("Redis健康检查失败", e);
                    return Health.down()
                        .withDetail("redis", "connection failed")
                        .withDetail("error", e.getMessage())
                        .build();
                }
            });

            return future.get(redisConfig.getTimeout().toSeconds(), TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Redis健康检查超时或失败", e);
            return Health.down()
                .withDetail("redis", "check timeout or failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    /**
     * 检查磁盘健康状态
     */
    public Health checkDiskHealth() {
        MonitoringConfig.DiskHealthConfig diskConfig = monitoringConfig.getHealth().getDisk();
        if (!diskConfig.isEnabled()) {
            return Health.up().withDetail("status", "disabled").build();
        }

        try {
            File diskPath = new File(diskConfig.getPath());
            long totalSpace = diskPath.getTotalSpace();
            long freeSpace = diskPath.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            double usageRatio = totalSpace > 0 ? (double) usedSpace / totalSpace : 0;
            
            Health.Builder builder = Health.up();
            Status status = Status.UP;
            
            if (usageRatio >= diskConfig.getErrorThreshold()) {
                builder.down();
                status = Status.DOWN;
            } else if (usageRatio >= diskConfig.getWarningThreshold()) {
                builder.status("WARNING");
            }
            
            return builder
                .withDetail("disk", status.getCode().toLowerCase())
                .withDetail("path", diskConfig.getPath())
                .withDetail("total", formatBytes(totalSpace))
                .withDetail("free", formatBytes(freeSpace))
                .withDetail("used", formatBytes(usedSpace))
                .withDetail("usageRatio", String.format("%.2f%%", usageRatio * 100))
                .withDetail("warningThreshold", String.format("%.2f%%", diskConfig.getWarningThreshold() * 100))
                .withDetail("errorThreshold", String.format("%.2f%%", diskConfig.getErrorThreshold() * 100))
                .build();
                
        } catch (Exception e) {
            log.error("磁盘健康检查失败", e);
            return Health.down()
                .withDetail("disk", "check failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    /**
     * 检查内存健康状态
     */
    public Health checkMemoryHealth() {
        MonitoringConfig.MemoryHealthConfig memoryConfig = monitoringConfig.getHealth().getMemory();
        if (!memoryConfig.isEnabled()) {
            return Health.up().withDetail("status", "disabled").build();
        }

        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // 堆内存
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUsageRatio = heapMax > 0 ? (double) heapUsed / heapMax : 0;
            
            // 非堆内存
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            double nonHeapUsageRatio = nonHeapMax > 0 ? (double) nonHeapUsed / nonHeapMax : 0;
            
            Health.Builder builder = Health.up();
            Status status = Status.UP;
            
            // 检查堆内存使用率
            if (heapUsageRatio >= memoryConfig.getErrorThreshold()) {
                builder.down();
                status = Status.DOWN;
            } else if (heapUsageRatio >= memoryConfig.getWarningThreshold()) {
                builder.status("WARNING");
            }
            
            return builder
                .withDetail("memory", status.getCode().toLowerCase())
                .withDetail("heap.used", formatBytes(heapUsed))
                .withDetail("heap.max", formatBytes(heapMax))
                .withDetail("heap.usageRatio", String.format("%.2f%%", heapUsageRatio * 100))
                .withDetail("nonHeap.used", formatBytes(nonHeapUsed))
                .withDetail("nonHeap.max", nonHeapMax > 0 ? formatBytes(nonHeapMax) : "unlimited")
                .withDetail("nonHeap.usageRatio", String.format("%.2f%%", nonHeapUsageRatio * 100))
                .withDetail("warningThreshold", String.format("%.2f%%", memoryConfig.getWarningThreshold() * 100))
                .withDetail("errorThreshold", String.format("%.2f%%", memoryConfig.getErrorThreshold() * 100))
                .build();
                
        } catch (Exception e) {
            log.error("内存健康检查失败", e);
            return Health.down()
                .withDetail("memory", "check failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    /**
     * 检查自定义健康状态
     */
    public Health checkCustomHealth() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // 检查线程状态
            int activeThreads = Thread.activeCount();
            details.put("threads.active", activeThreads);
            
            // 检查类加载器状态
            long loadedClassCount = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
            details.put("classes.loaded", loadedClassCount);
            
            // 检查运行时间
            long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
            details.put("uptime", formatDuration(Duration.ofMillis(uptime)));
            
            // 检查GC状态
            long totalGcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(bean -> bean.getCollectionTime())
                .sum();
            details.put("gc.totalTime", formatDuration(Duration.ofMillis(totalGcTime)));
            
            return Health.up()
                .withDetail("custom", "available")
                .withDetails(details)
                .build();
                
        } catch (Exception e) {
            log.error("自定义健康检查失败", e);
            return Health.down()
                .withDetail("custom", "check failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    /**
     * 获取Redis信息
     */
    private Map<String, Object> getRedisInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            // 这里可以添加更多Redis信息获取逻辑
            info.put("connected", true);
            info.put("checkTime", LocalDateTime.now().toString());
        } catch (Exception e) {
            log.warn("获取Redis信息失败", e);
            info.put("connected", false);
            info.put("error", e.getMessage());
        }
        return info;
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        String[] units = {"KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * 格式化持续时间
     */
    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * 获取简化的健康状态
     */
    public Map<String, String> getSimpleHealthStatus() {
        Map<String, String> status = new HashMap<>();
        
        Health overallHealth = checkOverallHealth();
        status.put("overall", overallHealth.getStatus().getCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Health> components = (Map<String, Health>) overallHealth.getDetails().get("components");
        
        if (components != null) {
            components.forEach((name, health) -> 
                status.put(name, health.getStatus().getCode()));
        }
        
        return status;
    }
}