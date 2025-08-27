package com.rui.common.config;

import com.rui.common.config.encryption.ConfigEncryption;
import com.rui.common.config.properties.ConfigProperties;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
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
@EnableConfigurationProperties(ConfigProperties.class)
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
     * 配置加密器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.config.encryption", name = "enabled", havingValue = "true")
    public ConfigEncryption configEncryption(ConfigProperties configProperties) {
        ConfigProperties.EncryptionProperties encryption = configProperties.getEncryption();
        return new ConfigEncryption(encryption.getAlgorithm(), "AES/ECB/PKCS5Padding", encryption.getSecretKey());
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
                Map<String, ConfigManager.ValidationResult> validationResults = configManager.validateAllConfigurationsAsMap();
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
                summary.put("configMetadata", configManager.getAllConfigMetadata());
            }
            
            if (dynamicConfigUpdater != null) {
                summary.put("dynamicProperties", dynamicConfigUpdater.getCurrentDynamicProperties());
                summary.put("updateHistory", dynamicConfigUpdater.getUpdateHistory(10));
            }
            
            return summary;
        }
        
        /**
         * 根据前缀获取配置
         */
        @GetMapping("/prefix/{prefix}")
        public Map<String, Object> getConfigByPrefix(@PathVariable String prefix) {
            Map<String, Object> result = new HashMap<>();
            
            try {
                if (configManager != null) {
                    Map<String, Object> configs = configManager.getConfigByPrefix(prefix);
                    result.put("success", true);
                    result.put("data", configs);
                } else {
                    result.put("success", false);
                    result.put("message", "ConfigManager not available");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Error getting config: " + e.getMessage());
            }
            
            return result;
        }
        
        /**
         * 验证配置
         */
        @PostMapping("/validate/{prefix}")
        public Map<String, Object> validateConfig(@PathVariable String prefix) {
            Map<String, Object> result = new HashMap<>();
            
            try {
                if (configManager != null && configValidator != null) {
                    boolean isValid = configManager.validateConfiguration(prefix);
                    ConfigManager.ValidationResult validationResult = new ConfigManager.ValidationResult();
                    if (isValid) {
                        validationResult.addSuccess(prefix);
                    } else {
                        validationResult.addError(prefix, "Validation failed");
                    }
                    result.put("success", true);
                    result.put("data", validationResult);
                } else {
                    result.put("success", false);
                    result.put("message", "ConfigManager or ConfigValidator not available");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Error validating config: " + e.getMessage());
            }
            
            return result;
        }
        
        /**
         * 更新单个属性
         */
        @PostMapping("/property")
        public Map<String, Object> updateProperty(@RequestParam String key, 
                                                 @RequestParam String value) {
            Map<String, Object> result = new HashMap<>();
            
            try {
                if (dynamicConfigUpdater != null) {
                    DynamicConfigUpdater.UpdateResult updateResult = dynamicConfigUpdater.updateProperty(key, value);
                    result.put("success", updateResult.isSuccess());
                    result.put("message", updateResult.getMessage());
                    result.put("level", updateResult.getLevel());
                } else {
                    result.put("success", false);
                    result.put("message", "DynamicConfigUpdater not available");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Error updating property: " + e.getMessage());
            }
            
            return result;
        }
        
        /**
         * 批量更新属性
         */
        @PostMapping("/properties")
        public Map<String, Object> updateProperties(@RequestBody Map<String, Object> properties) {
            Map<String, Object> result = new HashMap<>();
            
            try {
                if (dynamicConfigUpdater != null) {
                    DynamicConfigUpdater.UpdateResult updateResult = dynamicConfigUpdater.updateProperties(properties);
                    result.put("success", updateResult.isSuccess());
                    result.put("message", updateResult.getMessage());
                    result.put("level", updateResult.getLevel());
                } else {
                    result.put("success", false);
                    result.put("message", "DynamicConfigUpdater not available");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Error updating properties: " + e.getMessage());
            }
            
            return result;
        }
        
        /**
         * 刷新配置
         */
        @PostMapping("/refresh")
        public Map<String, Object> refreshConfiguration(@RequestParam(required = false) String prefix) {
            Map<String, Object> result = new HashMap<>();
            
            try {
                if (dynamicConfigUpdater != null) {
                    DynamicConfigUpdater.UpdateResult updateResult = prefix != null ? 
                        dynamicConfigUpdater.refreshConfiguration(prefix) : 
                        dynamicConfigUpdater.refreshConfiguration();
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
        
        /**
         * 获取动态属性
         */
        @GetMapping("/dynamic")
        public Map<String, Object> getDynamicProperties() {
            Map<String, Object> result = new HashMap<>();
            
            try {
                if (dynamicConfigUpdater != null) {
                    Map<String, Object> dynamicProperties = dynamicConfigUpdater.getCurrentDynamicProperties();
                    result.put("success", true);
                    result.put("data", dynamicProperties);
                } else {
                    result.put("success", false);
                    result.put("message", "DynamicConfigUpdater not available");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Error getting dynamic properties: " + e.getMessage());
            }
            
            return result;
        }
        
        /**
         * 获取更新历史
         */
        @GetMapping("/history")
        public Map<String, Object> getUpdateHistory(@RequestParam(defaultValue = "50") int limit) {
            Map<String, Object> result = new HashMap<>();
            
            try {
                if (dynamicConfigUpdater != null) {
                    List<DynamicConfigUpdater.ConfigUpdateRecord> history = dynamicConfigUpdater.getUpdateHistory(limit);
                    result.put("success", true);
                    result.put("data", history);
                } else {
                    result.put("success", false);
                    result.put("message", "DynamicConfigUpdater not available");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Error getting update history: " + e.getMessage());
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
            
            try {
                if (dynamicConfigUpdater != null) {
                    dynamicConfigUpdater.scheduleRefresh(prefix, intervalSeconds);
                    result.put("success", true);
                    result.put("message", "Scheduled refresh configured for prefix: " + prefix);
                } else {
                    result.put("success", false);
                    result.put("message", "DynamicConfigUpdater not available");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Error scheduling refresh: " + e.getMessage());
            }
            
            return result;
        }
        
        /**
         * 取消定时刷新
         */
        @DeleteMapping("/schedule-refresh/{prefix}")
        public Map<String, Object> cancelScheduledRefresh(@PathVariable String prefix) {
            Map<String, Object> result = new HashMap<>();
            
            try {
                if (dynamicConfigUpdater != null) {
                    dynamicConfigUpdater.cancelScheduledRefresh(prefix);
                    result.put("success", true);
                    result.put("message", "Scheduled refresh cancelled for prefix: " + prefix);
                } else {
                    result.put("success", false);
                    result.put("message", "DynamicConfigUpdater not available");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "Error cancelling scheduled refresh: " + e.getMessage());
            }
            
            return result;
        }
    }

    /**
     * 配置管理健康指示器
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    @ConditionalOnProperty(prefix = "rui.config.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ConfigManagementHealthIndicator configManagementHealthIndicator(ConfigManager configManager,
                                                                          ConfigValidator configValidator) {
        return new ConfigManagementHealthIndicator(configManager, configValidator);
    }

    /**
     * 配置管理健康指示器实现
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
                
                if (configManager != null && configValidator != null) {
                    ConfigManager.ValidationResult validationResult = 
                        configManager.validateAllConfigurations();                    
                    boolean hasErrors = validationResult.hasErrors();
                    
                    if (hasErrors) {
                        builder.down().withDetail("invalidConfigurations", validationResult.getErrorCount());
                    }
                    
                    builder.withDetail("totalConfigurations", validationResult.getSuccessCount() + validationResult.getErrorCount());
                    builder.withDetail("validConfigurations", validationResult.getSuccessCount());
                }
                
                return builder.build();
                
            } catch (Exception e) {
                return org.springframework.boot.actuate.health.Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
            }
        }
    }
}