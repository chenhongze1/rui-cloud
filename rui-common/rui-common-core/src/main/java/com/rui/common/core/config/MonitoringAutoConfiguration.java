package com.rui.common.core.config;

import com.rui.common.core.monitoring.AlertManager;
import com.rui.common.core.monitoring.AlertRuleEngine;
import com.rui.common.core.monitoring.CustomHealthEndpoint;
import com.rui.common.core.monitoring.CustomMetricsEndpoint;
import com.rui.common.core.monitoring.HealthChecker;
import com.rui.common.core.monitoring.MetricsCollector;
import com.rui.common.core.monitoring.MonitoringScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * 监控自动配置类
 * 整合监控相关组件的自动配置
 *
 * @author rui
 */
@Configuration
@RequiredArgsConstructor
@EnableScheduling
@EnableConfigurationProperties(MonitoringConfig.class)
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnProperty(prefix = "rui.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringAutoConfiguration {

    /**
     * 监控配置
     */
    @Bean
    @ConditionalOnMissingBean
    public MonitoringConfig monitoringConfig() {
        return new MonitoringConfig();
    }

    /**
     * 指标收集器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "rui.monitoring.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MetricsCollector metricsCollector(MeterRegistry meterRegistry, MonitoringConfig monitoringConfig) {
        return new MetricsCollector(meterRegistry, monitoringConfig);
    }

    /**
     * 健康检查器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.monitoring.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HealthChecker healthChecker(MonitoringConfig monitoringConfig,
                                     DataSource dataSource,
                                     RedisTemplate<String, Object> redisTemplate) {
        return new HealthChecker(monitoringConfig, dataSource, redisTemplate);
    }

    /**
     * 监控端点配置
     */
    @Configuration
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.web.WebEndpointResponse")
    static class MonitoringEndpointConfiguration {

        /**
         * 自定义健康检查端点
         */
        @Bean
        @ConditionalOnMissingBean
        public CustomHealthEndpoint customHealthEndpoint(HealthChecker healthChecker) {
            return new CustomHealthEndpoint(healthChecker);
        }

        /**
         * 自定义指标端点
         */
        @Bean
        @ConditionalOnMissingBean
        public CustomMetricsEndpoint customMetricsEndpoint(MetricsCollector metricsCollector) {
            return new CustomMetricsEndpoint(metricsCollector);
        }
    }

    /**
     * 监控任务调度配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "rui.monitoring", name = "enabled", havingValue = "true")
    static class MonitoringScheduleConfiguration {

        /**
         * 监控任务调度器
         */
        @Bean
        @ConditionalOnMissingBean
        public MonitoringScheduler monitoringScheduler(MonitoringConfig monitoringConfig,
                                                      MetricsCollector metricsCollector,
                                                      HealthChecker healthChecker) {
            return new MonitoringScheduler(monitoringConfig, metricsCollector, healthChecker);
        }
    }

    /**
     * 告警配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "rui.monitoring.alert", name = "enabled", havingValue = "true")
    static class AlertConfiguration {

        /**
         * 告警管理器
         */
        @Bean
        @ConditionalOnMissingBean
        public AlertManager alertManager(MonitoringConfig monitoringConfig) {
            return new AlertManager(monitoringConfig);
        }

        /**
         * 告警规则引擎
         */
        @Bean
        @ConditionalOnMissingBean
        public AlertRuleEngine alertRuleEngine(MonitoringConfig monitoringConfig,
                                              AlertManager alertManager) {
            return new AlertRuleEngine(monitoringConfig, alertManager);
        }
    }
}