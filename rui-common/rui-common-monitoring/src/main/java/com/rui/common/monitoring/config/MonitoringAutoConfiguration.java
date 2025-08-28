package com.rui.common.monitoring.config;

import com.rui.common.monitoring.endpoint.MonitoringManagementEndpoint;
import com.rui.common.monitoring.health.MonitoringHealthIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 监控自动配置类
 * 整合所有监控相关组件
 *
 * @author rui
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MonitoringProperties.class)
@ConditionalOnProperty(prefix = "rui.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringAutoConfiguration {

    /**
     * 监控配置
     */
    @Bean
    @ConditionalOnMissingBean
    public MonitoringConfig monitoringConfig(MonitoringProperties monitoringProperties) {
        MonitoringConfig config = new MonitoringConfig();
        
        // 从properties复制配置
        config.setEnabled(monitoringProperties.isEnabled());
        
        // 复制metrics配置
        MonitoringConfig.MetricsConfig metricsConfig = new MonitoringConfig.MetricsConfig();
        metricsConfig.setEnabled(monitoringProperties.getMetrics().isEnabled());
        metricsConfig.setExportInterval(monitoringProperties.getMetrics().getExportInterval());
        config.setMetrics(metricsConfig);
        
        // 复制health配置
        MonitoringConfig.HealthConfig healthConfig = new MonitoringConfig.HealthConfig();
        healthConfig.setEnabled(monitoringProperties.getHealth().isEnabled());
        healthConfig.setCheckInterval(monitoringProperties.getHealth().getCheckInterval());
        config.setHealth(healthConfig);
        
        // 复制performance配置
        MonitoringConfig.PerformanceConfig performanceConfig = new MonitoringConfig.PerformanceConfig();
        performanceConfig.setEnabled(monitoringProperties.getPerformance().isEnabled());
        performanceConfig.setSlowThreshold(monitoringProperties.getPerformance().getSlowOperationThreshold());
        config.setPerformance(performanceConfig);
        
        // 复制business配置
        MonitoringConfig.BusinessConfig businessConfig = new MonitoringConfig.BusinessConfig();
        businessConfig.setEnabled(monitoringProperties.getBusiness().isEnabled());
        businessConfig.setAnalysisInterval(Duration.ofMinutes(5)); // 设置默认分析间隔
        config.setBusiness(businessConfig);
        
        // 复制alert配置
        MonitoringConfig.AlertConfig alertConfig = new MonitoringConfig.AlertConfig();
        alertConfig.setEnabled(monitoringProperties.getAlert().isEnabled());
        alertConfig.setCheckInterval(Duration.ofMinutes(1)); // 设置默认检查间隔
        config.setAlert(alertConfig);
        
        return config;
    }

    /**
     * 监控管理端点
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnProperty(prefix = "rui.monitoring.endpoint", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MonitoringManagementEndpoint monitoringManagementEndpoint(MonitoringConfig monitoringConfig) {
        return new MonitoringManagementEndpoint(monitoringConfig);
    }

    /**
     * 监控健康检查器
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    @ConditionalOnProperty(prefix = "rui.monitoring.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MonitoringHealthIndicator monitoringHealthIndicator(MonitoringConfig monitoringConfig) {
        return new MonitoringHealthIndicator(monitoringConfig);
    }

    /**
     * 监控指标收集器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.monitoring.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MonitoringMetricsCollector monitoringMetricsCollector(MonitoringConfig monitoringConfig) {
        return new MonitoringMetricsCollector(monitoringConfig);
    }

    /**
     * 监控性能分析器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.monitoring.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MonitoringPerformanceAnalyzer monitoringPerformanceAnalyzer(MonitoringConfig monitoringConfig) {
        return new MonitoringPerformanceAnalyzer(monitoringConfig);
    }

    /**
     * 监控告警管理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.monitoring.alert", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MonitoringAlertManager monitoringAlertManager(MonitoringConfig monitoringConfig) {
        return new MonitoringAlertManager(monitoringConfig);
    }

    /**
     * 监控业务分析器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.monitoring.business", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MonitoringBusinessAnalyzer monitoringBusinessAnalyzer(MonitoringConfig monitoringConfig) {
        return new MonitoringBusinessAnalyzer(monitoringConfig);
    }
}