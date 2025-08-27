package com.rui.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 监控配置类
 * 统一管理各模块的监控配置
 *
 * @author rui
 */
@Data
@Component
@ConfigurationProperties(prefix = "rui.monitoring")
public class MonitoringConfig {

    /**
     * 是否启用监控
     */
    private boolean enabled = true;

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

    /**
     * 指标收集配置
     */
    @Data
    public static class MetricsConfig {
        /**
         * 是否启用指标收集
         */
        private boolean enabled = true;

        /**
         * 指标导出间隔
         */
        private Duration exportInterval = Duration.ofSeconds(30);

        /**
         * 指标保留时间
         */
        private Duration retentionPeriod = Duration.ofDays(7);

        /**
         * 自定义标签
         */
        private Map<String, String> commonTags;

        /**
         * 启用的指标类型
         */
        private List<String> enabledMetrics = List.of(
            "jvm", "system", "http", "database", "redis", "business"
        );

        /**
         * JVM指标配置
         */
        private JvmMetricsConfig jvm = new JvmMetricsConfig();

        /**
         * HTTP指标配置
         */
        private HttpMetricsConfig http = new HttpMetricsConfig();

        /**
         * 数据库指标配置
         */
        private DatabaseMetricsConfig database = new DatabaseMetricsConfig();

        /**
         * Redis指标配置
         */
        private RedisMetricsConfig redis = new RedisMetricsConfig();
        
        public boolean isJvmEnabled() {
            return jvm.isEnabled();
        }
        
        public boolean isSystemEnabled() {
            return enabledMetrics.contains("system");
        }
        
        public int getThreadPoolSize() {
            return 10; // 默认线程池大小
        }
    }

    /**
     * JVM指标配置
     */
    @Data
    public static class JvmMetricsConfig {
        private boolean enabled = true;
        private boolean memoryMetrics = true;
        private boolean gcMetrics = true;
        private boolean threadMetrics = true;
        private boolean classLoaderMetrics = true;
    }

    /**
     * HTTP指标配置
     */
    @Data
    public static class HttpMetricsConfig {
        private boolean enabled = true;
        private boolean requestMetrics = true;
        private boolean responseMetrics = true;
        private boolean errorMetrics = true;
        private List<String> ignoredPaths = List.of("/actuator/**", "/health", "/metrics");
    }

    /**
     * 数据库指标配置
     */
    @Data
    public static class DatabaseMetricsConfig {
        private boolean enabled = true;
        private boolean connectionPoolMetrics = true;
        private boolean queryMetrics = true;
        private boolean slowQueryMetrics = true;
        private Duration slowQueryThreshold = Duration.ofSeconds(1);
    }

    /**
     * Redis指标配置
     */
    @Data
    public static class RedisMetricsConfig {
        private boolean enabled = true;
        private boolean connectionMetrics = true;
        private boolean commandMetrics = true;
        private boolean keyspaceMetrics = true;
        private boolean memoryMetrics = true;
    }

    /**
     * 健康检查配置
     */
    @Data
    public static class HealthConfig {
        /**
         * 是否启用健康检查
         */
        private boolean enabled = true;

        /**
         * 健康检查间隔
         */
        private Duration checkInterval = Duration.ofSeconds(30);

        /**
         * 健康检查超时时间
         */
        private Duration timeout = Duration.ofSeconds(10);

        /**
         * 启用的健康检查器
         */
        private List<String> enabledCheckers = List.of(
            "database", "redis", "disk", "memory", "custom"
        );

        /**
         * 数据库健康检查配置
         */
        private DatabaseHealthConfig database = new DatabaseHealthConfig();

        /**
         * Redis健康检查配置
         */
        private RedisHealthConfig redis = new RedisHealthConfig();

        /**
         * 磁盘健康检查配置
         */
        private DiskHealthConfig disk = new DiskHealthConfig();

        /**
         * 内存健康检查配置
         */
        private MemoryHealthConfig memory = new MemoryHealthConfig();
    }

    /**
     * 数据库健康检查配置
     */
    @Data
    public static class DatabaseHealthConfig {
        private boolean enabled = true;
        private String validationQuery = "SELECT 1";
        private Duration timeout = Duration.ofSeconds(5);
    }

    /**
     * Redis健康检查配置
     */
    @Data
    public static class RedisHealthConfig {
        private boolean enabled = true;
        private Duration timeout = Duration.ofSeconds(3);
    }

    /**
     * 磁盘健康检查配置
     */
    @Data
    public static class DiskHealthConfig {
        private boolean enabled = true;
        private double warningThreshold = 0.8; // 80%
        private double errorThreshold = 0.9;   // 90%
        private String path = "/";
    }

    /**
     * 内存健康检查配置
     */
    @Data
    public static class MemoryHealthConfig {
        private boolean enabled = true;
        private double warningThreshold = 0.8; // 80%
        private double errorThreshold = 0.9;   // 90%
    }

    /**
     * 性能监控配置
     */
    @Data
    public static class PerformanceConfig {
        /**
         * 是否启用性能监控
         */
        private boolean enabled = true;

        /**
         * 方法执行时间监控
         */
        private MethodTimingConfig methodTiming = new MethodTimingConfig();

        /**
         * 慢操作监控
         */
        private SlowOperationConfig slowOperation = new SlowOperationConfig();

        /**
         * 资源使用监控
         */
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

    /**
     * 方法执行时间监控配置
     */
    @Data
    public static class MethodTimingConfig {
        private boolean enabled = true;
        private List<String> includedPackages = List.of("com.rui");
        private List<String> excludedPackages = List.of("com.rui.common.core.config");
        private Duration slowThreshold = Duration.ofMillis(500);
    }

    /**
     * 慢操作监控配置
     */
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

    /**
     * 资源使用监控配置
     */
    @Data
    public static class ResourceUsageConfig {
        private boolean enabled = true;
        private Duration samplingInterval = Duration.ofSeconds(10);
        private boolean cpuUsage = true;
        private boolean memoryUsage = true;
        private boolean diskUsage = true;
        private boolean networkUsage = true;
    }

    /**
     * 业务监控配置
     */
    @Data
    public static class BusinessConfig {
        /**
         * 是否启用业务监控
         */
        private boolean enabled = true;

        /**
         * 用户行为监控
         */
        private UserBehaviorConfig userBehavior = new UserBehaviorConfig();

        /**
         * 业务指标监控
         */
        private BusinessMetricsConfig businessMetrics = new BusinessMetricsConfig();

        /**
         * 错误监控
         */
        private ErrorMonitoringConfig errorMonitoring = new ErrorMonitoringConfig();
    }

    /**
     * 用户行为监控配置
     */
    @Data
    public static class UserBehaviorConfig {
        private boolean enabled = true;
        private boolean loginTracking = true;
        private boolean operationTracking = true;
        private boolean pageViewTracking = true;
        private Duration sessionTimeout = Duration.ofMinutes(30);
    }

    /**
     * 业务指标监控配置
     */
    @Data
    public static class BusinessMetricsConfig {
        private boolean enabled = true;
        private boolean transactionMetrics = true;
        private boolean orderMetrics = true;
        private boolean paymentMetrics = true;
        private boolean customMetrics = true;
    }

    /**
     * 错误监控配置
     */
    @Data
    public static class ErrorMonitoringConfig {
        private boolean enabled = true;
        private boolean exceptionTracking = true;
        private boolean errorRateTracking = true;
        private boolean alertOnHighErrorRate = true;
        private double errorRateThreshold = 0.05; // 5%
        private Duration errorRateWindow = Duration.ofMinutes(5);
    }

    /**
     * 告警配置
     */
    @Data
    public static class AlertConfig {
        /**
         * 是否启用告警
         */
        private boolean enabled = true;

        /**
         * 告警规则
         */
        private List<AlertRule> rules;

        /**
         * 告警通道配置
         */
        private AlertChannelConfig channels = new AlertChannelConfig();

        /**
         * 告警抑制配置
         */
        private AlertSuppressionConfig suppression = new AlertSuppressionConfig();
    }

    /**
     * 告警规则
     */
    @Data
    public static class AlertRule {
        private String name;
        private String metric;
        private String condition; // >, <, >=, <=, ==, !=
        private double threshold;
        private Duration duration;
        private String severity; // critical, warning, info
        private String level = "warning"; // critical, warning, info
        private int consecutiveCount = 1; // 连续触发次数
        private String description;
        private Map<String, String> labels;
    }

    /**
     * 告警通道配置
     */
    @Data
    public static class AlertChannelConfig {
        private EmailConfig email = new EmailConfig();
        private SmsConfig sms = new SmsConfig();
        private WebhookConfig webhook = new WebhookConfig();
        private DingTalkConfig dingTalk = new DingTalkConfig();
    }

    /**
     * 邮件告警配置
     */
    @Data
    public static class EmailConfig {
        private boolean enabled = false;
        private List<String> recipients;
        private String subject = "[RUI-Cloud] 监控告警";
    }

    /**
     * 短信告警配置
     */
    @Data
    public static class SmsConfig {
        private boolean enabled = false;
        private List<String> phoneNumbers;
        private String template;
    }

    /**
     * Webhook告警配置
     */
    @Data
    public static class WebhookConfig {
        private boolean enabled = false;
        private String url;
        private Map<String, String> headers;
        private Duration timeout = Duration.ofSeconds(10);
    }

    /**
     * 钉钉告警配置
     */
    @Data
    public static class DingTalkConfig {
        private boolean enabled = false;
        private String webhook;
        private String secret;
        private List<String> atMobiles;
        private boolean atAll = false;
    }

    /**
     * 告警抑制配置
     */
    @Data
    public static class AlertSuppressionConfig {
        private boolean enabled = true;
        private Duration cooldownPeriod = Duration.ofMinutes(10);
        private int maxAlertsPerHour = 10;
        private boolean groupSimilarAlerts = true;
    }
}