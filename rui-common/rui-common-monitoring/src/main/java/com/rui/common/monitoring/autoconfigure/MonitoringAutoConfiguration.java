package com.rui.common.monitoring.autoconfigure;

import com.rui.common.monitoring.alert.AlertManager;
import com.rui.common.monitoring.alert.AlertRuleEngine;
import com.rui.common.monitoring.health.CustomHealthEndpoint;
import com.rui.common.monitoring.health.HealthChecker;
import com.rui.common.monitoring.metrics.CustomMetricsEndpoint;
import com.rui.common.monitoring.metrics.MetricsCollector;
import com.rui.common.monitoring.properties.MonitoringProperties;
import com.rui.common.monitoring.scheduler.MonitoringScheduler;
import com.rui.common.monitoring.aspect.PerformanceMonitoringAspect;
import com.rui.common.monitoring.service.PerformanceMonitoringService;
import com.rui.common.monitoring.adapter.LogPerformanceAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * 监控模块自动配置类
 * 统一配置监控相关的所有组件
 *
 * @author rui
 */
@Slf4j
@AutoConfiguration
@EnableScheduling
@ComponentScan(basePackages = "com.rui.common.monitoring")
@EnableConfigurationProperties(MonitoringProperties.class)
@ConditionalOnProperty(prefix = "rui.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringAutoConfiguration {

    /**
     * 监控属性配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MonitoringProperties monitoringProperties() {
        log.info("初始化监控配置属性");
        return new MonitoringProperties();
    }

    /**
     * 指标收集器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MetricsCollector metricsCollector(MeterRegistry meterRegistry, MonitoringProperties monitoringProperties) {
        log.info("初始化指标收集器");
        return new MetricsCollector(meterRegistry, monitoringProperties);
    }

    /**
     * 健康检查器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HealthChecker healthChecker(MonitoringProperties monitoringProperties, 
                                     @Autowired(required = false) DataSource dataSource,
                                     @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        log.info("初始化健康检查器");
        return new HealthChecker(monitoringProperties, dataSource, redisTemplate);
    }

    /**
     * 告警管理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring.alert", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AlertManager alertManager(MonitoringProperties monitoringProperties) {
        log.info("初始化告警管理器");
        return new AlertManager(monitoringProperties);
    }

    /**
     * 告警规则引擎
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring.alert", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AlertRuleEngine alertRuleEngine(MonitoringProperties monitoringProperties, AlertManager alertManager) {
        log.info("初始化告警规则引擎");
        return new AlertRuleEngine(monitoringProperties, alertManager);
    }

    /**
     * 监控调度器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MonitoringScheduler monitoringScheduler(MonitoringProperties monitoringProperties,
                                                   MetricsCollector metricsCollector,
                                                   HealthChecker healthChecker) {
        log.info("初始化监控调度器");
        return new MonitoringScheduler(monitoringProperties, metricsCollector, healthChecker);
    }

    /**
     * 自定义指标端点
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CustomMetricsEndpoint customMetricsEndpoint(MetricsCollector metricsCollector) {
        log.info("初始化自定义指标端点");
        return new CustomMetricsEndpoint(metricsCollector);
    }

    /**
     * 自定义健康检查端点
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CustomHealthEndpoint customHealthEndpoint(HealthChecker healthChecker) {
        log.info("初始化自定义健康检查端点");
        return new CustomHealthEndpoint(healthChecker);
    }

    /**
     * 性能监控切面
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PerformanceMonitoringAspect performanceMonitoringAspect(MetricsCollector metricsCollector, 
                                                                  MonitoringProperties monitoringProperties) {
        log.info("初始化性能监控切面");
        return new PerformanceMonitoringAspect(metricsCollector, monitoringProperties);
    }

    /**
     * 性能监控服务
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PerformanceMonitoringService performanceMonitoringService(MetricsCollector metricsCollector,
                                                                    AlertManager alertManager,
                                                                    MonitoringProperties monitoringProperties) {
        log.info("初始化性能监控服务");
        return new PerformanceMonitoringService(metricsCollector, alertManager, monitoringProperties);
    }

    /**
     * 日志性能适配器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.monitoring.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LogPerformanceAdapter logPerformanceAdapter(PerformanceMonitoringService performanceMonitoringService) {
        log.info("初始化日志性能适配器");
        return new LogPerformanceAdapter(performanceMonitoringService);
    }
}