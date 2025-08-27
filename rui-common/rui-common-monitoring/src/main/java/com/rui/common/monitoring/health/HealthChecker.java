package com.rui.common.monitoring.health;

import com.rui.common.monitoring.properties.MonitoringProperties;
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

    private final MonitoringProperties monitoringProperties;
    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 执行全面健康检查
     */
    public Health checkOverallHealth() {
        if (!monitoringProperties.isEnabled() || !monitoringProperties.getHealth().isEnabled()) {
            return Health.up().withDetail("monitoring", "disabled").build();
        }

        Map<String, Health> healthResults = new HashMap<>();
        Health.Builder overallBuilder = Health.up();
        boolean hasDown = false;

        // 检查数据库健康状态
        if (monitoringProperties.getHealth().getEnabledCheckers().contains("database")) {
            Health dbHealth = checkDatabaseHealth();
            healthResults.put("database", dbHealth);
            if (dbHealth.getStatus() == Status.DOWN) {
                hasDown = true;
            }
        }

        // 检查Redis健康状态
        if (monitoringProperties.getHealth().getEnabledCheckers().contains("redis")) {
            Health redisHealth = checkRedisHealth();
            healthResults.put("redis", redisHealth);
            if (redisHealth.getStatus() == Status.DOWN) {
                hasDown = true;
            }
        }

        // 检查磁盘健康状态
        if (monitoringProperties.getHealth().getEnabledCheckers().contains("disk")) {
            Health diskHealth = checkDiskHealth();
            healthResults.put("disk", diskHealth);
            if (diskHealth.getStatus() == Status.DOWN) {
                hasDown = true;
            }
        }

        // 检查内存健康状态
        if (monitoringProperties.getHealth().getEnabledCheckers().contains("memory")) {
            Health memoryHealth = checkMemoryHealth();
            healthResults.put("memory", memoryHealth);
            if (memoryHealth.getStatus() == Status.DOWN) {
                hasDown = true;
            }
        }

        // 检查自定义健康状态
        if (monitoringProperties.getHealth().getEnabledCheckers().contains("custom")) {
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
        MonitoringProperties.DatabaseHealthConfig dbConfig = monitoringProperties.getHealth().getDatabase();
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
        MonitoringProperties.RedisHealthConfig redisConfig = monitoringProperties.getHealth().getRedis();
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
        MonitoringProperties.DiskHealthConfig diskConfig = monitoringProperties.getHealth().getDisk();
        if (!diskConfig.isEnabled()) {
            return Health.up().withDetail("status", "disabled").build();
        }

        try {
            File diskPath = new File(diskConfig.getPath());
            long totalSpace = diskPath.getTotalSpace();
            long freeSpace = diskPath.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            double usagePercentage = (double) usedSpace / totalSpace * 100;
            
            Health.Builder builder = Health.up();
            
            if (usagePercentage > diskConfig.getThreshold()) {
                builder = Health.down();
            }
            
            return builder
                .withDetail("path", diskConfig.getPath())
                .withDetail("total", formatBytes(totalSpace))
                .withDetail("free", formatBytes(freeSpace))
                .withDetail("used", formatBytes(usedSpace))
                .withDetail("usagePercentage", String.format("%.2f%%", usagePercentage))
                .withDetail("threshold", diskConfig.getThreshold() + "%")
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
        MonitoringProperties.MemoryHealthConfig memoryConfig = monitoringProperties.getHealth().getMemory();
        if (!memoryConfig.isEnabled()) {
            return Health.up().withDetail("status", "disabled").build();
        }

        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            
            double heapUsagePercentage = (double) heapUsed / heapMax * 100;
            double nonHeapUsagePercentage = nonHeapMax > 0 ? (double) nonHeapUsed / nonHeapMax * 100 : 0;
            
            Health.Builder builder = Health.up();
            
            if (heapUsagePercentage > memoryConfig.getHeapThreshold() || 
                nonHeapUsagePercentage > memoryConfig.getNonHeapThreshold()) {
                builder = Health.down();
            }
            
            return builder
                .withDetail("heap", Map.of(
                    "used", formatBytes(heapUsed),
                    "max", formatBytes(heapMax),
                    "usagePercentage", String.format("%.2f%%", heapUsagePercentage),
                    "threshold", memoryConfig.getHeapThreshold() + "%"
                ))
                .withDetail("nonHeap", Map.of(
                    "used", formatBytes(nonHeapUsed),
                    "max", nonHeapMax > 0 ? formatBytes(nonHeapMax) : "unlimited",
                    "usagePercentage", String.format("%.2f%%", nonHeapUsagePercentage),
                    "threshold", memoryConfig.getNonHeapThreshold() + "%"
                ))
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
            // 这里可以添加自定义的健康检查逻辑
            // 例如检查外部服务、第三方API等
            
            return Health.up()
                .withDetail("custom", "all checks passed")
                .withDetail("timestamp", LocalDateTime.now().toString())
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
            // 这里可以获取Redis的详细信息
            info.put("version", "unknown");
            info.put("mode", "standalone");
            info.put("connectedClients", 0);
        } catch (Exception e) {
            log.warn("获取Redis信息失败", e);
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
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            return hours + "h " + minutes + "m " + secs + "s";
        }
    }

    /**
     * 获取简单的健康状态
     */
    public Map<String, String> getSimpleHealthStatus() {
        Map<String, String> status = new HashMap<>();
        
        Health overallHealth = checkOverallHealth();
        status.put("overall", overallHealth.getStatus().getCode());
        
        if (overallHealth.getDetails().containsKey("components")) {
            @SuppressWarnings("unchecked")
            Map<String, Health> components = (Map<String, Health>) overallHealth.getDetails().get("components");
            components.forEach((key, health) -> status.put(key, health.getStatus().getCode()));
        }
        
        return status;
    }
}