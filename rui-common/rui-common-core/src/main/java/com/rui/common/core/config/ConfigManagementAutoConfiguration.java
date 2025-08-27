package com.rui.common.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置管理自动配置类
 * 整合所有配置管理相关组件
 *
 * @author rui
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties
@ConditionalOnProperty(prefix = "rui.config", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConfigManagementAutoConfiguration {

    /**
     * 配置管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigManager configManager() {
        return new ConfigManager();
    }

    /**
     * 配置验证器
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigValidator configValidator() {
        return new ConfigValidator();
    }

    /**
     * 动态配置更新器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.config.dynamic", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DynamicConfigUpdater dynamicConfigUpdater() {
        return new DynamicConfigUpdater();
    }

    /**
     * 配置管理端点
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnProperty(prefix = "rui.config.endpoint", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ConfigManagementEndpoint configManagementEndpoint(ConfigManager configManager,
                                                            ConfigValidator configValidator,
                                                            DynamicConfigUpdater dynamicConfigUpdater) {
        return new ConfigManagementEndpoint(configManager, configValidator, dynamicConfigUpdater);
    }

    /**
     * 配置管理控制器
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
    @ConditionalOnProperty(prefix = "rui.config.web", name = "enabled", havingValue = "true")
    public ConfigManagementController configManagementController(ConfigManager configManager,
                                                                ConfigValidator configValidator,
                                                                DynamicConfigUpdater dynamicConfigUpdater) {
        return new ConfigManagementController(configManager, configValidator, dynamicConfigUpdater);
    }

    /**
     * 配置管理端点
     */
    @Endpoint(id = "config-management")
    public static class ConfigManagementEndpoint {
        
        private final ConfigManager configManager;
        private final ConfigValidator configValidator;
        private final DynamicConfigUpdater dynamicConfigUpdater;
        
        public ConfigManagementEndpoint(ConfigManager configManager,
                                      ConfigValidator configValidator,
                                      DynamicConfigUpdater dynamicConfigUpdater) {
            this.configManager = configManager;
            this.configValidator = configValidator;
            this.dynamicConfigUpdater = dynamicConfigUpdater;
        }
        
        /**
         * 获取配置摘要
         */
        @ReadOperation
        public Map<String, Object> getConfigSummary() {
            Map<String, Object> summary = new HashMap<>();
            
            if (configManager != null) {
                summary.put("configSummary", configManager.getConfigSummary());
                summary.put("configPrefixes", configManager.getAllConfigPrefixes());
                summary.put("configMetadata", configManager.getAllConfigMetadata());
            }
            
            if (dynamicConfigUpdater != null) {
                summary.put("dynamicProperties", dynamicConfigUpdater.getCurrentDynamicProperties());
                summary.put("updateHistory", dynamicConfigUpdater.getUpdateHistory(10));
            }
            
            return summary;
        }
        
        /**
         * 验证所有配置
         */
        @ReadOperation
        public Map<String, Object> validateConfigurations() {
            Map<String, Object> result = new HashMap<>();
            
            if (configManager != null && configValidator != null) {
                Map<String, ConfigManager.ValidationResult> validationResults = configManager.validateAllConfigurations();
                result.put("validationResults", validationResults);
                
                long validCount = validationResults.values().stream()
                    .filter(ConfigManager.ValidationResult::isValid)
                    .count();
                result.put("validCount", validCount);
                result.put("totalCount", validationResults.size());
                result.put("invalidCount", validationResults.size() - validCount);
            }
            
            return result;
        }
        
        /**
         * 刷新配置
         */
        @WriteOperation
        public Map<String, Object> refreshConfiguration(@NotBlank String prefix) {
            Map<String, Object> result = new HashMap<>();
            
            try {
                if (dynamicConfigUpdater != null) {
                    DynamicConfigUpdater.UpdateResult updateResult = dynamicConfigUpdater.refreshConfiguration(prefix);
                    result.put("success", updateResult.isSuccess());
                    result.put("message", updateResult.getMessage());
                    result.put("level", updateResult.getLevel());
                } else {
                    result.put("success", false);
                    result.put("message", "DynamicConfigUpdater not available");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Error refreshing configuration: " + e.getMessage());
            }
            
            return result;
        }
    }

    /**
     * 配置管理控制器
     */
    @RestController
    @RequestMapping("/api/config")
    public static class ConfigManagementController {
        
        private final ConfigManager configManager;
        private final ConfigValidator configValidator;
        private final DynamicConfigUpdater dynamicConfigUpdater;
        
        public ConfigManagementController(ConfigManager configManager,
                                        ConfigValidator configValidator,
                                        DynamicConfigUpdater dynamicConfigUpdater) {
            this.configManager = configManager;
            this.configValidator = configValidator;
            this.dynamicConfigUpdater = dynamicConfigUpdater;
        }
        
        /**
         * 获取配置摘要
         */
        @GetMapping("/summary")
        public Map<String, Object> getConfigSummary() {
            Map<String, Object> summary = new HashMap<>();
            
            if (configManager != null) {
                summary.put("configSummary", configManager.getConfigSummary());
                summary.put("configPrefixes", configManager.getAllConfigPrefixes());
            }
            
            if (dynamicConfigUpdater != null) {
                summary.put("dynamicPropertiesCount", dynamicConfigUpdater.getCurrentDynamicProperties().size());
                summary.put("updateHistoryCount", dynamicConfigUpdater.getUpdateHistory().size());
            }
            
            return summary;
        }
        
        /**
         * 获取指定前缀的配置
         */
        @GetMapping("/prefix/{prefix}")
        public Map<String, Object> getConfigByPrefix(@PathVariable String prefix) {
            Map<String, Object> result = new HashMap<>();
            
            if (configManager != null) {
                ConfigManager.ConfigMetadata metadata = configManager.getConfigMetadata(prefix);
                if (metadata != null) {
                    result.put("metadata", metadata);
                    
                    // 验证配置
                    ConfigManager.ValidationResult validationResult = configManager.validateConfiguration(prefix);
                    result.put("validation", validationResult);
                } else {
                    result.put("error", "Configuration not found for prefix: " + prefix);
                }
            }
            
            return result;
        }
        
        /**
         * 验证指定配置
         */
        @PostMapping("/validate/{prefix}")
        public Map<String, Object> validateConfig(@PathVariable String prefix) {
            Map<String, Object> result = new HashMap<>();
            
            if (configManager != null) {
                ConfigManager.ValidationResult validationResult = configManager.validateConfiguration(prefix);
                result.put("valid", validationResult.isValid());
                result.put("message", validationResult.getMessage());
                result.put("errors", validationResult.getErrors());
            } else {
                result.put("error", "ConfigManager not available");
            }
            
            return result;
        }
        
        /**
         * 更新动态配置属性
         */
        @PostMapping("/property")
        public Map<String, Object> updateProperty(@RequestParam String key, 
                                                 @RequestParam String value) {
            Map<String, Object> result = new HashMap<>();
            
            if (dynamicConfigUpdater != null) {
                DynamicConfigUpdater.UpdateResult updateResult = dynamicConfigUpdater.updateProperty(key, value);
                result.put("success", updateResult.isSuccess());
                result.put("message", updateResult.getMessage());
                result.put("level", updateResult.getLevel());
            } else {
                result.put("success", false);
                result.put("message", "DynamicConfigUpdater not available");
            }
            
            return result;
        }
        
        /**
         * 批量更新动态配置属性
         */
        @PostMapping("/properties")
        public Map<String, Object> updateProperties(@RequestBody Map<String, Object> properties) {
            Map<String, Object> result = new HashMap<>();
            
            if (dynamicConfigUpdater != null) {
                DynamicConfigUpdater.UpdateResult updateResult = dynamicConfigUpdater.updateProperties(properties);
                result.put("success", updateResult.isSuccess());
                result.put("message", updateResult.getMessage());
                result.put("level", updateResult.getLevel());
            } else {
                result.put("success", false);
                result.put("message", "DynamicConfigUpdater not available");
            }
            
            return result;
        }
        
        /**
         * 刷新配置
         */
        @PostMapping("/refresh")
        public Map<String, Object> refreshConfiguration(@RequestParam(required = false) String prefix) {
            Map<String, Object> result = new HashMap<>();
            
            if (dynamicConfigUpdater != null) {
                DynamicConfigUpdater.UpdateResult updateResult = dynamicConfigUpdater.refreshConfiguration(prefix);
                result.put("success", updateResult.isSuccess());
                result.put("message", updateResult.getMessage());
                result.put("level", updateResult.getLevel());
            } else {
                result.put("success", false);
                result.put("message", "DynamicConfigUpdater not available");
            }
            
            return result;
        }
        
        /**
         * 获取动态配置属性
         */
        @GetMapping("/dynamic")
        public Map<String, Object> getDynamicProperties() {
            Map<String, Object> result = new HashMap<>();
            
            if (dynamicConfigUpdater != null) {
                result.put("properties", dynamicConfigUpdater.getCurrentDynamicProperties());
            } else {
                result.put("error", "DynamicConfigUpdater not available");
            }
            
            return result;
        }
        
        /**
         * 获取更新历史
         */
        @GetMapping("/history")
        public Map<String, Object> getUpdateHistory(@RequestParam(defaultValue = "50") int limit) {
            Map<String, Object> result = new HashMap<>();
            
            if (dynamicConfigUpdater != null) {
                List<DynamicConfigUpdater.ConfigUpdateRecord> history = dynamicConfigUpdater.getUpdateHistory(limit);
                result.put("history", history);
                result.put("count", history.size());
            } else {
                result.put("error", "DynamicConfigUpdater not available");
            }
            
            return result;
        }
        
        /**
         * 定时刷新配置
         */
        @PostMapping("/schedule-refresh")
        public Map<String, Object> scheduleRefresh(@RequestParam String prefix,
                                                  @RequestParam long intervalSeconds) {
            Map<String, Object> result = new HashMap<>();
            
            if (dynamicConfigUpdater != null) {
                try {
                    dynamicConfigUpdater.scheduleRefresh(prefix, intervalSeconds);
                    result.put("success", true);
                    result.put("message", "Scheduled refresh configured for prefix: " + prefix);
                } catch (Exception e) {
                    result.put("success", false);
                    result.put("message", "Error scheduling refresh: " + e.getMessage());
                }
            } else {
                result.put("success", false);
                result.put("message", "DynamicConfigUpdater not available");
            }
            
            return result;
        }
        
        /**
         * 取消定时刷新
         */
        @DeleteMapping("/schedule-refresh/{prefix}")
        public Map<String, Object> cancelScheduledRefresh(@PathVariable String prefix) {
            Map<String, Object> result = new HashMap<>();
            
            if (dynamicConfigUpdater != null) {
                try {
                    dynamicConfigUpdater.cancelScheduledRefresh(prefix);
                    result.put("success", true);
                    result.put("message", "Cancelled scheduled refresh for prefix: " + prefix);
                } catch (Exception e) {
                    result.put("success", false);
                    result.put("message", "Error cancelling scheduled refresh: " + e.getMessage());
                }
            } else {
                result.put("success", false);
                result.put("message", "DynamicConfigUpdater not available");
            }
            
            return result;
        }
    }

    /**
     * 配置管理健康检查
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    @ConditionalOnProperty(prefix = "rui.config.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ConfigManagementHealthIndicator configManagementHealthIndicator(ConfigManager configManager,
                                                                          ConfigValidator configValidator) {
        return new ConfigManagementHealthIndicator(configManager, configValidator);
    }

    /**
     * 配置管理健康检查指示器
     */
    public static class ConfigManagementHealthIndicator implements org.springframework.boot.actuate.health.HealthIndicator {
        
        private final ConfigManager configManager;
        private final ConfigValidator configValidator;
        
        public ConfigManagementHealthIndicator(ConfigManager configManager, ConfigValidator configValidator) {
            this.configManager = configManager;
            this.configValidator = configValidator;
        }
        
        @Override
        public org.springframework.boot.actuate.health.Health health() {
            try {
                org.springframework.boot.actuate.health.Health.Builder builder = 
                    org.springframework.boot.actuate.health.Health.up();
                
                if (configManager != null) {
                    ConfigManager.ConfigSummary summary = configManager.getConfigSummary();
                    builder.withDetail("totalConfigurations", summary.getTotalConfigurations())
                           .withDetail("validConfigurations", summary.getValidConfigurations())
                           .withDetail("invalidConfigurations", summary.getInvalidConfigurations());
                    
                    // 如果有无效配置，标记为DOWN
                    if (summary.getInvalidConfigurations() > 0) {
                        builder.down().withDetail("reason", "Invalid configurations detected");
                    }
                }
                
                return builder.build();
                
            } catch (Exception e) {
                return org.springframework.boot.actuate.health.Health.down(e)
                    .withDetail("error", "Error checking config management health")
                    .build();
            }
        }
    }
}