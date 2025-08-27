package com.rui.common.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义健康检查端点
 * 提供详细的健康状态信息
 *
 * @author rui
 */
@Slf4j
@Component
@Endpoint(id = "custom-health")
@RequiredArgsConstructor
public class CustomHealthEndpoint {

    private final HealthChecker healthChecker;

    /**
     * 获取整体健康状态
     */
    @ReadOperation
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Health overallHealth = healthChecker.checkOverallHealth();
            result.put("status", overallHealth.getStatus().getCode());
            result.put("timestamp", LocalDateTime.now());
            result.put("details", overallHealth.getDetails());
            
            // 添加各组件健康状态
            Map<String, Object> components = new HashMap<>();
            components.put("database", getComponentHealth("database"));
            components.put("redis", getComponentHealth("redis"));
            components.put("disk", getComponentHealth("disk"));
            components.put("memory", getComponentHealth("memory"));
            
            result.put("components", components);
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            result.put("status", Status.DOWN.getCode());
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 获取指定组件的健康状态
     */
    @ReadOperation
    public Map<String, Object> componentHealth(@Selector String component) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Health health = getHealthByComponent(component);
            result.put("status", health.getStatus().getCode());
            result.put("timestamp", LocalDateTime.now());
            result.put("details", health.getDetails());
            
        } catch (Exception e) {
            log.error("组件健康检查失败: {}", component, e);
            result.put("status", Status.DOWN.getCode());
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 获取健康检查历史
     */
    @ReadOperation
    public Map<String, Object> history() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 这里可以从Redis或数据库获取历史数据
            result.put("timestamp", LocalDateTime.now());
            result.put("message", "健康检查历史功能待实现");
            
        } catch (Exception e) {
            log.error("获取健康检查历史失败", e);
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 获取组件健康状态的简化信息
     */
    private Map<String, Object> getComponentHealth(String component) {
        Map<String, Object> componentHealth = new HashMap<>();
        
        try {
            Health health = getHealthByComponent(component);
            componentHealth.put("status", health.getStatus().getCode());
            
            // 只包含关键信息
            Map<String, Object> details = health.getDetails();
            if (details != null && !details.isEmpty()) {
                Map<String, Object> summary = new HashMap<>();
                
                switch (component) {
                    case "database":
                        summary.put("validationQuery", details.get("validationQuery"));
                        summary.put("database", details.get("database"));
                        break;
                    case "redis":
                        summary.put("version", details.get("version"));
                        summary.put("mode", details.get("mode"));
                        break;
                    case "disk":
                        summary.put("free", details.get("free"));
                        summary.put("threshold", details.get("threshold"));
                        break;
                    case "memory":
                        summary.put("free", details.get("free"));
                        summary.put("total", details.get("total"));
                        break;
                }
                
                componentHealth.put("details", summary);
            }
            
        } catch (Exception e) {
            log.warn("获取组件健康状态失败: {}", component, e);
            componentHealth.put("status", Status.UNKNOWN.getCode());
            componentHealth.put("error", e.getMessage());
        }
        
        return componentHealth;
    }

    /**
     * 根据组件名称获取健康状态
     */
    private Health getHealthByComponent(String component) {
        switch (component.toLowerCase()) {
            case "database":
                return healthChecker.checkDatabaseHealth();
            case "redis":
                return healthChecker.checkRedisHealth();
            case "disk":
                return healthChecker.checkDiskHealth();
            case "memory":
                return healthChecker.checkMemoryHealth();
            default:
                return Health.unknown()
                    .withDetail("error", "未知的组件: " + component)
                    .build();
        }
    }
}