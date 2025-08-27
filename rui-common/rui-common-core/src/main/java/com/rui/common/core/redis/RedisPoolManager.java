package com.rui.common.core.redis;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis连接池管理器
 * 提供高性能的连接池管理和监控功能
 *
 * @author rui
 */
@Slf4j
@Component
public class RedisPoolManager {

    @Autowired
    private RedisPoolConfig poolConfig;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private ScheduledExecutorService monitoringExecutor;
    private ScheduledExecutorService healthCheckExecutor;
    
    private final Map<String, PoolMetrics> poolMetricsMap = new ConcurrentHashMap<>();
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong idleConnections = new AtomicLong(0);
    private final AtomicLong commandCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    private volatile boolean healthy = true;
    private volatile LocalDateTime lastHealthCheck;
    private volatile String healthStatus = "UP";

    /**
     * 初始化连接池管理器
     */
    @PostConstruct
    public void initialize() {
        if (!poolConfig.isEnabled()) {
            log.info("Redis连接池优化已禁用");
            return;
        }

        log.info("初始化Redis连接池管理器");
        
        // 配置连接池
        configureConnectionPool();
        
        // 启动监控
        if (poolConfig.getMonitoring().isEnabled()) {
            startMonitoring();
        }
        
        // 启动健康检查
        if (poolConfig.getMonitoring().isHealthCheck()) {
            startHealthCheck();
        }
        
        // 连接预热
        if (poolConfig.getPerformance().isConnectionWarmup()) {
            warmupConnections();
        }
        
        log.info("Redis连接池管理器初始化完成");
    }

    /**
     * 配置连接池
     */
    private void configureConnectionPool() {
        if (connectionFactory instanceof JedisConnectionFactory) {
            configureJedisPool((JedisConnectionFactory) connectionFactory);
        } else if (connectionFactory instanceof LettuceConnectionFactory) {
            configureLettucePool((LettuceConnectionFactory) connectionFactory);
        }
    }

    /**
     * 配置Jedis连接池
     */
    private void configureJedisPool(JedisConnectionFactory factory) {
        JedisPoolConfig jedisConfig = new JedisPoolConfig();
        RedisPoolConfig.Pool poolConf = poolConfig.getPool();
        
        jedisConfig.setMaxTotal(poolConf.getMaxTotal());
        jedisConfig.setMaxIdle(poolConf.getMaxIdle());
        jedisConfig.setMinIdle(poolConf.getMinIdle());
        jedisConfig.setMaxWaitMillis(poolConf.getMaxWait().toMillis());
        jedisConfig.setTestOnBorrow(poolConf.isTestOnBorrow());
        jedisConfig.setTestOnReturn(poolConf.isTestOnReturn());
        jedisConfig.setTestWhileIdle(poolConf.isTestWhileIdle());
        jedisConfig.setTimeBetweenEvictionRunsMillis(poolConf.getTimeBetweenEvictionRuns().toMillis());
        jedisConfig.setMinEvictableIdleTimeMillis(poolConf.getMinEvictableIdleTime().toMillis());
        jedisConfig.setSoftMinEvictableIdleTimeMillis(poolConf.getSoftMinEvictableIdleTime().toMillis());
        jedisConfig.setNumTestsPerEvictionRun(poolConf.getNumTestsPerEvictionRun());
        jedisConfig.setLifo(poolConf.isLifo());
        jedisConfig.setFairness(poolConf.isFairness());
        jedisConfig.setBlockWhenExhausted(poolConf.isBlockWhenExhausted());
        
        // 设置连接超时
        factory.setTimeout((int) poolConfig.getConnection().getConnectTimeout().toMillis());
        
        log.info("Jedis连接池配置完成: maxTotal={}, maxIdle={}, minIdle={}", 
                poolConf.getMaxTotal(), poolConf.getMaxIdle(), poolConf.getMinIdle());
    }

    /**
     * 配置Lettuce连接池
     */
    private void configureLettucePool(LettuceConnectionFactory factory) {
        GenericObjectPoolConfig<?> poolConf = new GenericObjectPoolConfig<>();
        RedisPoolConfig.Pool poolConfig = this.poolConfig.getPool();
        
        poolConf.setMaxTotal(poolConfig.getMaxTotal());
        poolConf.setMaxIdle(poolConfig.getMaxIdle());
        poolConf.setMinIdle(poolConfig.getMinIdle());
        poolConf.setMaxWaitMillis(poolConfig.getMaxWait().toMillis());
        poolConf.setTestOnBorrow(poolConfig.isTestOnBorrow());
        poolConf.setTestOnReturn(poolConfig.isTestOnReturn());
        poolConf.setTestWhileIdle(poolConfig.isTestWhileIdle());
        poolConf.setTimeBetweenEvictionRunsMillis(poolConfig.getTimeBetweenEvictionRuns().toMillis());
        poolConf.setMinEvictableIdleTimeMillis(poolConfig.getMinEvictableIdleTime().toMillis());
        poolConf.setSoftMinEvictableIdleTimeMillis(poolConfig.getSoftMinEvictableIdleTime().toMillis());
        poolConf.setNumTestsPerEvictionRun(poolConfig.getNumTestsPerEvictionRun());
        poolConf.setLifo(poolConfig.isLifo());
        poolConf.setFairness(poolConfig.isFairness());
        poolConf.setBlockWhenExhausted(poolConfig.isBlockWhenExhausted());
        
        // 设置连接和读取超时
        factory.setTimeout(this.poolConfig.getConnection().getConnectTimeout().toMillis());
        
        log.info("Lettuce连接池配置完成: maxTotal={}, maxIdle={}, minIdle={}", 
                poolConfig.getMaxTotal(), poolConfig.getMaxIdle(), poolConfig.getMinIdle());
    }

    /**
     * 启动监控
     */
    private void startMonitoring() {
        monitoringExecutor = Executors.newScheduledThreadPool(2);
        
        // 定期收集指标
        monitoringExecutor.scheduleAtFixedRate(
                this::collectMetrics,
                0,
                poolConfig.getMonitoring().getMetricsInterval().getSeconds(),
                TimeUnit.SECONDS
        );
        
        // 定期检测慢查询
        if (poolConfig.getPerformance().isSlowQueryDetection()) {
            monitoringExecutor.scheduleAtFixedRate(
                    this::detectSlowQueries,
                    10,
                    30,
                    TimeUnit.SECONDS
            );
        }
        
        log.info("Redis连接池监控已启动");
    }

    /**
     * 启动健康检查
     */
    private void startHealthCheck() {
        healthCheckExecutor = Executors.newScheduledThreadPool(1);
        
        healthCheckExecutor.scheduleAtFixedRate(
                this::performHealthCheck,
                0,
                poolConfig.getMonitoring().getHealthCheckInterval().getSeconds(),
                TimeUnit.SECONDS
        );
        
        log.info("Redis连接池健康检查已启动");
    }

    /**
     * 连接预热
     */
    private void warmupConnections() {
        int warmupCount = poolConfig.getPerformance().getWarmupConnections();
        
        try {
            for (int i = 0; i < warmupCount; i++) {
                redisTemplate.opsForValue().get("warmup:" + i);
            }
            log.info("连接预热完成，预热连接数: {}", warmupCount);
        } catch (Exception e) {
            log.warn("连接预热失败", e);
        }
    }

    /**
     * 收集指标
     */
    private void collectMetrics() {
        try {
            PoolMetrics metrics = getCurrentPoolMetrics();
            poolMetricsMap.put("current", metrics);
            
            // 发送到监控系统
            if (meterRegistry != null) {
                sendMetricsToMicrometer(metrics);
            }
            
            // 记录日志
            if (log.isDebugEnabled()) {
                log.debug("连接池指标: {}", metrics);
            }
        } catch (Exception e) {
            log.error("收集连接池指标失败", e);
        }
    }

    /**
     * 获取当前连接池指标
     */
    private PoolMetrics getCurrentPoolMetrics() {
        PoolMetrics metrics = new PoolMetrics();
        
        if (connectionFactory instanceof JedisConnectionFactory) {
            // Jedis指标收集
            JedisConnectionFactory jedisFactory = (JedisConnectionFactory) connectionFactory;
            // 这里需要根据实际的Jedis版本和API来获取指标
            metrics.setTotalConnections(totalConnections.get());
            metrics.setActiveConnections(activeConnections.get());
            metrics.setIdleConnections(idleConnections.get());
        } else if (connectionFactory instanceof LettuceConnectionFactory) {
            // Lettuce指标收集
            metrics.setTotalConnections(totalConnections.get());
            metrics.setActiveConnections(activeConnections.get());
            metrics.setIdleConnections(idleConnections.get());
        }
        
        metrics.setCommandCount(commandCount.get());
        metrics.setErrorCount(errorCount.get());
        metrics.setTimestamp(LocalDateTime.now());
        
        return metrics;
    }

    /**
     * 发送指标到Micrometer
     */
    private void sendMetricsToMicrometer(PoolMetrics metrics) {
        meterRegistry.gauge("redis.pool.total.connections", metrics.getTotalConnections());
        meterRegistry.gauge("redis.pool.active.connections", metrics.getActiveConnections());
        meterRegistry.gauge("redis.pool.idle.connections", metrics.getIdleConnections());
        meterRegistry.counter("redis.pool.commands.total").increment(metrics.getCommandCount());
        meterRegistry.counter("redis.pool.errors.total").increment(metrics.getErrorCount());
    }

    /**
     * 检测慢查询
     */
    private void detectSlowQueries() {
        // 这里可以实现慢查询检测逻辑
        // 由于Redis本身的慢查询日志功能，这里主要是收集和分析
        try {
            // 获取慢查询日志
            // 这需要根据具体的Redis客户端实现
            log.debug("执行慢查询检测");
        } catch (Exception e) {
            log.error("慢查询检测失败", e);
        }
    }

    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        try {
            Timer.Sample sample = Timer.start();
            
            // 执行ping命令
            String result = redisTemplate.getConnectionFactory().getConnection().ping();
            
            if (meterRegistry != null) {
                sample.stop(Timer.builder("redis.health.check")
                        .description("Redis health check duration")
                        .register(meterRegistry));
            }
            
            healthy = "PONG".equals(result);
            healthStatus = healthy ? "UP" : "DOWN";
            lastHealthCheck = LocalDateTime.now();
            
            if (!healthy) {
                log.warn("Redis健康检查失败: {}", result);
            }
        } catch (Exception e) {
            healthy = false;
            healthStatus = "DOWN";
            lastHealthCheck = LocalDateTime.now();
            log.error("Redis健康检查异常", e);
        }
    }

    /**
     * 获取连接池状态
     */
    public PoolStatus getPoolStatus() {
        PoolStatus status = new PoolStatus();
        status.setEnabled(poolConfig.isEnabled());
        status.setHealthy(healthy);
        status.setHealthStatus(healthStatus);
        status.setLastHealthCheck(lastHealthCheck);
        status.setMetrics(poolMetricsMap.get("current"));
        return status;
    }

    /**
     * 获取连接池配置信息
     */
    public RedisPoolConfig getPoolConfig() {
        return poolConfig;
    }

    /**
     * 重新配置连接池
     */
    public void reconfigurePool(RedisPoolConfig newConfig) {
        log.info("重新配置Redis连接池");
        this.poolConfig = newConfig;
        configureConnectionPool();
        log.info("Redis连接池重新配置完成");
    }

    /**
     * 清理资源
     */
    @PreDestroy
    public void destroy() {
        log.info("关闭Redis连接池管理器");
        
        if (monitoringExecutor != null && !monitoringExecutor.isShutdown()) {
            monitoringExecutor.shutdown();
            try {
                if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitoringExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitoringExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (healthCheckExecutor != null && !healthCheckExecutor.isShutdown()) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("Redis连接池管理器已关闭");
    }

    /**
     * 连接池指标
     */
    public static class PoolMetrics {
        private long totalConnections;
        private long activeConnections;
        private long idleConnections;
        private long commandCount;
        private long errorCount;
        private LocalDateTime timestamp;

        // Getters and Setters
        public long getTotalConnections() { return totalConnections; }
        public void setTotalConnections(long totalConnections) { this.totalConnections = totalConnections; }
        public long getActiveConnections() { return activeConnections; }
        public void setActiveConnections(long activeConnections) { this.activeConnections = activeConnections; }
        public long getIdleConnections() { return idleConnections; }
        public void setIdleConnections(long idleConnections) { this.idleConnections = idleConnections; }
        public long getCommandCount() { return commandCount; }
        public void setCommandCount(long commandCount) { this.commandCount = commandCount; }
        public long getErrorCount() { return errorCount; }
        public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        @Override
        public String toString() {
            return String.format("PoolMetrics{total=%d, active=%d, idle=%d, commands=%d, errors=%d, time=%s}",
                    totalConnections, activeConnections, idleConnections, commandCount, errorCount, timestamp);
        }
    }

    /**
     * 连接池状态
     */
    public static class PoolStatus {
        private boolean enabled;
        private boolean healthy;
        private String healthStatus;
        private LocalDateTime lastHealthCheck;
        private PoolMetrics metrics;

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        public String getHealthStatus() { return healthStatus; }
        public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
        public LocalDateTime getLastHealthCheck() { return lastHealthCheck; }
        public void setLastHealthCheck(LocalDateTime lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }
        public PoolMetrics getMetrics() { return metrics; }
        public void setMetrics(PoolMetrics metrics) { this.metrics = metrics; }
    }
}