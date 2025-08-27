package com.rui.common.core.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis优化自动配置
 * 整合所有Redis优化组件
 *
 * @author rui
 */
@Configuration
@ConditionalOnClass({RedisConnectionFactory.class, RedisTemplate.class})
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(RedisPoolConfig.class)
@ConditionalOnProperty(prefix = "rui.redis.pool", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisOptimizationAutoConfiguration {

    /**
     * Redis连接池管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisPoolManager redisPoolManager(RedisPoolConfig poolConfig,
                                            RedisConnectionFactory connectionFactory,
                                            RedisTemplate<String, Object> redisTemplate,
                                            MeterRegistry meterRegistry) {
        return new RedisPoolManager();
    }

    /**
     * Redis性能优化器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.redis.pool.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisPerformanceOptimizer redisPerformanceOptimizer(RedisPoolConfig poolConfig,
                                                              RedisTemplate<String, Object> redisTemplate,
                                                              MeterRegistry meterRegistry,
                                                              ObjectMapper objectMapper) {
        return new RedisPerformanceOptimizer();
    }

    /**
     * Redis健康检查指示器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.redis.pool.monitoring", name = "health-check", havingValue = "true", matchIfMissing = true)
    public RedisPoolHealthIndicator redisPoolHealthIndicator(RedisPoolManager poolManager) {
        return new RedisPoolHealthIndicator(poolManager);
    }

    /**
     * Redis监控端点
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.redis.pool.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisPoolEndpoint redisPoolEndpoint(RedisPoolManager poolManager,
                                              RedisPerformanceOptimizer performanceOptimizer) {
        return new RedisPoolEndpoint(poolManager, performanceOptimizer);
    }

    /**
     * Redis管理控制器
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisManagementController redisManagementController(RedisPoolManager poolManager,
                                                              RedisPerformanceOptimizer performanceOptimizer) {
        return new RedisManagementController(poolManager, performanceOptimizer);
    }

    /**
     * Redis连接池健康检查指示器
     */
    public static class RedisPoolHealthIndicator implements HealthIndicator {
        
        private final RedisPoolManager poolManager;
        
        public RedisPoolHealthIndicator(RedisPoolManager poolManager) {
            this.poolManager = poolManager;
        }
        
        @Override
        public Health health() {
            try {
                RedisPoolManager.PoolStatus status = poolManager.getPoolStatus();
                
                Health.Builder builder = status.isHealthy() ? Health.up() : Health.down();
                
                builder.withDetail("enabled", status.isEnabled())
                       .withDetail("healthy", status.isHealthy())
                       .withDetail("status", status.getHealthStatus())
                       .withDetail("lastHealthCheck", status.getLastHealthCheck());
                
                if (status.getMetrics() != null) {
                    RedisPoolManager.PoolMetrics metrics = status.getMetrics();
                    builder.withDetail("totalConnections", metrics.getTotalConnections())
                           .withDetail("activeConnections", metrics.getActiveConnections())
                           .withDetail("idleConnections", metrics.getIdleConnections())
                           .withDetail("commandCount", metrics.getCommandCount())
                           .withDetail("errorCount", metrics.getErrorCount());
                }
                
                return builder.build();
                
            } catch (Exception e) {
                return Health.down(e)
                           .withDetail("error", e.getMessage())
                           .build();
            }
        }
    }

    /**
     * Redis连接池监控端点
     */
    @Endpoint(id = "redis-pool")
    public static class RedisPoolEndpoint {
        
        private final RedisPoolManager poolManager;
        private final RedisPerformanceOptimizer performanceOptimizer;
        
        public RedisPoolEndpoint(RedisPoolManager poolManager, 
                                RedisPerformanceOptimizer performanceOptimizer) {
            this.poolManager = poolManager;
            this.performanceOptimizer = performanceOptimizer;
        }
        
        @ReadOperation
        public Map<String, Object> redisPool() {
            Map<String, Object> result = new HashMap<>();
            
            // 连接池状态
            RedisPoolManager.PoolStatus poolStatus = poolManager.getPoolStatus();
            result.put("poolStatus", poolStatus);
            
            // 性能统计
            if (performanceOptimizer != null) {
                RedisPerformanceOptimizer.PerformanceStats perfStats = performanceOptimizer.getPerformanceStats();
                result.put("performanceStats", perfStats);
            }
            
            // 配置信息
            result.put("configuration", poolManager.getPoolConfig());
            
            return result;
        }
    }

    /**
     * Redis管理控制器
     */
    @RestController
    @RequestMapping("/api/redis")
    public static class RedisManagementController {
        
        private final RedisPoolManager poolManager;
        private final RedisPerformanceOptimizer performanceOptimizer;
        
        public RedisManagementController(RedisPoolManager poolManager,
                                       RedisPerformanceOptimizer performanceOptimizer) {
            this.poolManager = poolManager;
            this.performanceOptimizer = performanceOptimizer;
        }
        
        /**
         * 获取连接池状态
         */
        @GetMapping("/pool/status")
        public ResponseEntity<RedisPoolManager.PoolStatus> getPoolStatus() {
            return ResponseEntity.ok(poolManager.getPoolStatus());
        }
        
        /**
         * 获取性能统计
         */
        @GetMapping("/performance/stats")
        public ResponseEntity<RedisPerformanceOptimizer.PerformanceStats> getPerformanceStats() {
            if (performanceOptimizer == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(performanceOptimizer.getPerformanceStats());
        }
        
        /**
         * 获取连接池配置
         */
        @GetMapping("/pool/config")
        public ResponseEntity<RedisPoolConfig> getPoolConfig() {
            return ResponseEntity.ok(poolManager.getPoolConfig());
        }
        
        /**
         * 更新连接池配置
         */
        @PutMapping("/pool/config")
        public ResponseEntity<String> updatePoolConfig(@RequestBody RedisPoolConfig newConfig) {
            try {
                poolManager.reconfigurePool(newConfig);
                return ResponseEntity.ok("连接池配置更新成功");
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("配置更新失败: " + e.getMessage());
            }
        }
        
        /**
         * 清理性能优化缓存
         */
        @PostMapping("/performance/clear-cache")
        public ResponseEntity<String> clearPerformanceCache() {
            if (performanceOptimizer == null) {
                return ResponseEntity.notFound().build();
            }
            
            try {
                performanceOptimizer.clearCaches();
                return ResponseEntity.ok("性能优化缓存清理成功");
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("缓存清理失败: " + e.getMessage());
            }
        }
        
        /**
         * 批量设置键值
         */
        @PostMapping("/batch/set")
        public ResponseEntity<String> batchSet(@RequestBody BatchSetRequest request) {
            if (performanceOptimizer == null) {
                return ResponseEntity.notFound().build();
            }
            
            try {
                Duration expiration = request.getExpirationSeconds() != null ? 
                    Duration.ofSeconds(request.getExpirationSeconds()) : null;
                
                performanceOptimizer.batchSet(request.getKeyValues(), expiration);
                return ResponseEntity.ok("批量设置成功");
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("批量设置失败: " + e.getMessage());
            }
        }
        
        /**
         * 批量获取键值
         */
        @PostMapping("/batch/get")
        public ResponseEntity<Map<String, Object>> batchGet(@RequestBody BatchGetRequest request) {
            if (performanceOptimizer == null) {
                return ResponseEntity.notFound().build();
            }
            
            try {
                Map<String, Object> result = performanceOptimizer.batchGet(request.getKeys());
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        /**
         * 原子计数器操作
         */
        @PostMapping("/atomic/increment")
        public ResponseEntity<Long> atomicIncrement(@RequestBody AtomicIncrementRequest request) {
            if (performanceOptimizer == null) {
                return ResponseEntity.notFound().build();
            }
            
            try {
                Duration expiration = request.getExpirationSeconds() != null ? 
                    Duration.ofSeconds(request.getExpirationSeconds()) : null;
                
                Long result = performanceOptimizer.atomicIncrement(
                    request.getKey(), 
                    request.getDelta(), 
                    expiration
                );
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        /**
         * 获取综合监控信息
         */
        @GetMapping("/monitoring")
        public ResponseEntity<Map<String, Object>> getMonitoringInfo() {
            Map<String, Object> result = new HashMap<>();
            
            // 连接池状态
            result.put("poolStatus", poolManager.getPoolStatus());
            
            // 性能统计
            if (performanceOptimizer != null) {
                result.put("performanceStats", performanceOptimizer.getPerformanceStats());
            }
            
            // 系统信息
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("timestamp", LocalDateTime.now());
            systemInfo.put("jvmMemory", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
            result.put("systemInfo", systemInfo);
            
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 批量设置请求
     */
    public static class BatchSetRequest {
        private Map<String, Object> keyValues;
        private Long expirationSeconds;
        
        public Map<String, Object> getKeyValues() { return keyValues; }
        public void setKeyValues(Map<String, Object> keyValues) { this.keyValues = keyValues; }
        public Long getExpirationSeconds() { return expirationSeconds; }
        public void setExpirationSeconds(Long expirationSeconds) { this.expirationSeconds = expirationSeconds; }
    }
    
    /**
     * 批量获取请求
     */
    public static class BatchGetRequest {
        private java.util.List<String> keys;
        
        public java.util.List<String> getKeys() { return keys; }
        public void setKeys(java.util.List<String> keys) { this.keys = keys; }
    }
    
    /**
     * 原子计数器请求
     */
    public static class AtomicIncrementRequest {
        private String key;
        private long delta = 1;
        private Long expirationSeconds;
        
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public long getDelta() { return delta; }
        public void setDelta(long delta) { this.delta = delta; }
        public Long getExpirationSeconds() { return expirationSeconds; }
        public void setExpirationSeconds(Long expirationSeconds) { this.expirationSeconds = expirationSeconds; }
    }
}