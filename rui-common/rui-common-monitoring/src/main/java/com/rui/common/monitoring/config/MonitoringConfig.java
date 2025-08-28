package com.rui.common.monitoring.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控配置类
 * 统一管理各模块的监控配置
 *
 * @author rui
 */
@Data
@Component
@ConfigurationProperties(prefix = "rui.monitoring")
public class MonitoringConfig implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringConfig.class);

    private final AtomicLong configReadCount = new AtomicLong(0);
    private final AtomicLong configWriteCount = new AtomicLong(0);
    private final AtomicLong configErrorCount = new AtomicLong(0);
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong cacheMissCount = new AtomicLong(0);
    
    private LocalDateTime lastHealthCheck;
    private final Map<String, Object> runtimeMetrics = new HashMap<>();

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    /**
     * 是否启用监控
     */
    private boolean enabled = true;

    @PostConstruct
    public void init() {
        lastHealthCheck = LocalDateTime.now();
        if (meterRegistry != null) {
            registerMetrics();
        }
    }

    @Override
    public Health health() {
        try {
            lastHealthCheck = LocalDateTime.now();
            
            Health.Builder builder = Health.up();
            
            // 添加基本指标
            builder.withDetail("configReadCount", configReadCount.get())
                   .withDetail("configWriteCount", configWriteCount.get())
                   .withDetail("configErrorCount", configErrorCount.get())
                   .withDetail("cacheHitCount", cacheHitCount.get())
                   .withDetail("cacheMissCount", cacheMissCount.get())
                   .withDetail("lastHealthCheck", lastHealthCheck);
            
            // 计算缓存命中率
            long totalCacheAccess = cacheHitCount.get() + cacheMissCount.get();
            if (totalCacheAccess > 0) {
                double hitRate = (double) cacheHitCount.get() / totalCacheAccess;
                builder.withDetail("cacheHitRate", String.format("%.2f%%", hitRate * 100));
            }
            
            // 检查错误率
            long totalOperations = configReadCount.get() + configWriteCount.get();
            if (totalOperations > 0) {
                double errorRate = (double) configErrorCount.get() / totalOperations;
                builder.withDetail("errorRate", String.format("%.2f%%", errorRate * 100));
                
                // 如果错误率过高，标记为DOWN
                if (errorRate > 0.1) { // 10%错误率阈值
                    builder.down().withDetail("reason", "High error rate: " + String.format("%.2f%%", errorRate * 100));
                }
            }
            
            return builder.build();
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return Health.down(e).build();
        }
    }

    /**
     * 注册Micrometer指标
     */
    private void registerMetrics() {
        if (meterRegistry == null) {
            return;
        }
        
        // 注册计数器指标
        Counter.builder("config.read.count")
                .description("Configuration read operations count")
                .register(meterRegistry);
                
        Counter.builder("config.write.count")
                .description("Configuration write operations count")
                .register(meterRegistry);
                
        Counter.builder("config.error.count")
                .description("Configuration error operations count")
                .register(meterRegistry);
                
        // 注册缓存指标
        Gauge.builder("cache.hit.rate", this, config -> {
                    long total = config.cacheHitCount.get() + config.cacheMissCount.get();
                    return total > 0 ? (double) config.cacheHitCount.get() / total : 0.0;
                })
                .description("Cache hit rate")
                .register(meterRegistry);
                  
        Gauge.builder("cache.hit.count", this, config -> (double) config.cacheHitCount.get())
                .description("Cache hit count")
                .register(meterRegistry);
                  
        Gauge.builder("cache.miss.count", this, config -> (double) config.cacheMissCount.get())
                .description("Cache miss count")
                .register(meterRegistry);
    }

    /**
     * 定时更新运行时指标
     */
    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    public void updateRuntimeMetrics() {
        if (!enabled) {
            return;
        }
        
        try {
            // 更新JVM指标
            Runtime runtime = Runtime.getRuntime();
            runtimeMetrics.put("jvm.memory.used", runtime.totalMemory() - runtime.freeMemory());
            runtimeMetrics.put("jvm.memory.free", runtime.freeMemory());
            runtimeMetrics.put("jvm.memory.total", runtime.totalMemory());
            runtimeMetrics.put("jvm.memory.max", runtime.maxMemory());
            
            // 更新线程指标
            runtimeMetrics.put("jvm.threads.active", Thread.activeCount());
            
            logger.debug("Runtime metrics updated: {}", runtimeMetrics);
        } catch (Exception e) {
            logger.error("Failed to update runtime metrics", e);
            configErrorCount.incrementAndGet();
        }
    }

    /**
     * 指标收集配置
     */
    private MetricsConfig metrics = new MetricsConfig();

    /**
     * 健康检查配置
     */
    private HealthConfig health = new HealthConfig();

    /**
     * 性能监控配置
     */
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * 业务监控配置
     */
    private BusinessConfig business = new BusinessConfig();

    /**
     * 告警配置
     */
    private AlertConfig alert = new AlertConfig();

    // 内部配置类保持不变...
    @Data
    public static class MetricsConfig {
        private boolean enabled = true;
        private Duration exportInterval = Duration.ofSeconds(30);
        private Duration retentionPeriod = Duration.ofDays(7);
        private Map<String, String> commonTags;
        private List<String> enabledMetrics = List.of(
            "jvm", "system", "http", "database", "redis", "business"
        );
        private JvmMetricsConfig jvm = new JvmMetricsConfig();
        private HttpMetricsConfig http = new HttpMetricsConfig();
        private DatabaseMetricsConfig database = new DatabaseMetricsConfig();
        private RedisMetricsConfig redis = new RedisMetricsConfig();
        
        public boolean isJvmEnabled() {
            return jvm.isEnabled();
        }
        
        public boolean isSystemEnabled() {
            return enabledMetrics.contains("system");
        }
        
        public int getThreadPoolSize() {
            return 10;
        }
    }

    @Data
    public static class JvmMetricsConfig {
        private boolean enabled = true;
        private boolean memoryMetrics = true;
        private boolean gcMetrics = true;
        private boolean threadMetrics = true;
        private boolean classLoaderMetrics = true;
    }

    @Data
    public static class HttpMetricsConfig {
        private boolean enabled = true;
        private boolean requestMetrics = true;
        private boolean responseMetrics = true;
        private boolean errorMetrics = true;
        private List<String> ignoredPaths = List.of("/actuator/**", "/health", "/metrics");
    }

    @Data
    public static class DatabaseMetricsConfig {
        private boolean enabled = true;
        private boolean connectionPoolMetrics = true;
        private boolean queryMetrics = true;
        private boolean slowQueryMetrics = true;
        private Duration slowQueryThreshold = Duration.ofSeconds(1);
    }

    @Data
    public static class RedisMetricsConfig {
        private boolean enabled = true;
        private boolean connectionMetrics = true;
        private boolean commandMetrics = true;
        private boolean keyspaceMetrics = true;
        private boolean memoryMetrics = true;
    }

    @Data
    public static class HealthConfig {
        private boolean enabled = true;
        private Duration checkInterval = Duration.ofSeconds(30);
        private Duration timeout = Duration.ofSeconds(10);
        private List<String> enabledCheckers = List.of(
            "database", "redis", "disk", "memory", "custom"
        );
        private DatabaseHealthConfig database = new DatabaseHealthConfig();
        private RedisHealthConfig redis = new RedisHealthConfig();
        private DiskHealthConfig disk = new DiskHealthConfig();
        private MemoryHealthConfig memory = new MemoryHealthConfig();
    }

    @Data
    public static class DatabaseHealthConfig {
        private boolean enabled = true;
        private String validationQuery = "SELECT 1";
        private Duration timeout = Duration.ofSeconds(5);
    }

    @Data
    public static class RedisHealthConfig {
        private boolean enabled = true;
        private Duration timeout = Duration.ofSeconds(3);
    }

    @Data
    public static class DiskHealthConfig {
        private boolean enabled = true;
        private double warningThreshold = 0.8;
        private double errorThreshold = 0.9;
        private String path = "/";
    }

    @Data
    public static class MemoryHealthConfig {
        private boolean enabled = true;
        private double warningThreshold = 0.8;
        private double errorThreshold = 0.9;
    }

    @Data
    public static class PerformanceConfig {
        private boolean enabled = true;
        private Duration slowThreshold = Duration.ofSeconds(2);
        private MethodTimingConfig methodTiming = new MethodTimingConfig();
        private SlowOperationConfig slowOperation = new SlowOperationConfig();
        private ResourceUsageConfig resourceUsage = new ResourceUsageConfig();
        
        public boolean isSlowOperationEnabled() {
            return slowOperation.isEnabled();
        }
        
        public boolean isResourceUsageEnabled() {
            return resourceUsage.isEnabled();
        }
        
        public Duration getSlowOperationThreshold() {
            return slowOperation.getSlowOperationThreshold();
        }
    }

    @Data
    public static class MethodTimingConfig {
        private boolean enabled = true;
        private List<String> includedPackages = List.of("com.rui");
        private List<String> excludedPackages = List.of("com.rui.common.core.config");
        private Duration slowThreshold = Duration.ofMillis(500);
    }

    @Data
    public static class SlowOperationConfig {
        private boolean enabled = true;
        private Duration httpRequestThreshold = Duration.ofSeconds(2);
        private Duration databaseQueryThreshold = Duration.ofSeconds(1);
        private Duration redisOperationThreshold = Duration.ofMillis(100);

        public Duration getSlowOperationThreshold() {
            return httpRequestThreshold;
        }
    }

    @Data
    public static class ResourceUsageConfig {
        private boolean enabled = true;
        private Duration samplingInterval = Duration.ofSeconds(10);
        private boolean cpuUsage = true;
        private boolean memoryUsage = true;
        private boolean diskUsage = true;
        private boolean networkUsage = true;
    }

    @Data
    public static class BusinessConfig {
        private boolean enabled = true;
        private Duration analysisInterval = Duration.ofMinutes(5);
        private UserBehaviorConfig userBehavior = new UserBehaviorConfig();
        private BusinessMetricsConfig businessMetrics = new BusinessMetricsConfig();
        private ErrorMonitoringConfig errorMonitoring = new ErrorMonitoringConfig();
    }

    @Data
    public static class UserBehaviorConfig {
        private boolean enabled = true;
        private boolean loginTracking = true;
        private boolean operationTracking = true;
        private boolean pageViewTracking = true;
        private Duration sessionTimeout = Duration.ofMinutes(30);
    }

    @Data
    public static class BusinessMetricsConfig {
        private boolean enabled = true;
        private boolean transactionMetrics = true;
        private boolean orderMetrics = true;
        private boolean paymentMetrics = true;
        private boolean customMetrics = true;
    }

    @Data
    public static class ErrorMonitoringConfig {
        private boolean enabled = true;
        private boolean exceptionTracking = true;
        private boolean errorRateTracking = true;
        private boolean alertOnHighErrorRate = true;
        private double errorRateThreshold = 0.05;
        private Duration errorRateWindow = Duration.ofMinutes(5);
    }

    @Data
    public static class AlertConfig {
        private boolean enabled = true;
        private Duration checkInterval = Duration.ofMinutes(1);
        private List<AlertRule> rules;
        private AlertChannelConfig channels = new AlertChannelConfig();
        private AlertSuppressionConfig suppression = new AlertSuppressionConfig();
    }

    @Data
    public static class AlertRule {
        private String name;
        private String metric;
        private String condition;
        private double threshold;
        private Duration duration;
        private String severity;
        private String level = "warning";
        private int consecutiveCount = 1;
        private String description;
        private Map<String, String> labels;
    }

    @Data
    public static class AlertChannelConfig {
        private EmailConfig email = new EmailConfig();
        private SmsConfig sms = new SmsConfig();
        private WebhookConfig webhook = new WebhookConfig();
        private DingTalkConfig dingTalk = new DingTalkConfig();
    }

    @Data
    public static class EmailConfig {
        private boolean enabled = false;
        private List<String> recipients;
        private String subject = "[RUI-Cloud] 监控告警";
    }

    @Data
    public static class SmsConfig {
        private boolean enabled = false;
        private List<String> phoneNumbers;
        private String template;
    }

    @Data
    public static class WebhookConfig {
        private boolean enabled = false;
        private String url;
        private Map<String, String> headers;
        private Duration timeout = Duration.ofSeconds(10);
    }

    @Data
    public static class DingTalkConfig {
        private boolean enabled = false;
        private String webhook;
        private String secret;
        private List<String> atMobiles;
        private boolean atAll = false;
    }

    @Data
    public static class AlertSuppressionConfig {
        private boolean enabled = true;
        private Duration cooldownPeriod = Duration.ofMinutes(10);
        private int maxAlertsPerHour = 10;
        private boolean groupSimilarAlerts = true;
    }

    // 提供计数器操作方法
    public void incrementConfigReadCount() {
        configReadCount.incrementAndGet();
    }

    public void incrementConfigWriteCount() {
        configWriteCount.incrementAndGet();
    }

    public void incrementConfigErrorCount() {
        configErrorCount.incrementAndGet();
    }

    public void incrementCacheHitCount() {
        cacheHitCount.incrementAndGet();
    }

    public void incrementCacheMissCount() {
        cacheMissCount.incrementAndGet();
    }
}