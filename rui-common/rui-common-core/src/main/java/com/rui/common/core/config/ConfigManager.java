package com.rui.common.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 统一配置管理器
 * 提供配置的统一管理、加载、刷新和验证功能
 *
 * @author rui
 */
@Slf4j
@Component
@RefreshScope
public class ConfigManager {

    @Autowired
    private Environment environment;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired(required = false)
    private Validator validator;
    
    /**
     * 配置缓存
     */
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    
    /**
     * 配置监听器
     */
    private final Map<String, List<ConfigChangeListener>> listeners = new ConcurrentHashMap<>();
    
    /**
     * 读写锁
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 配置元数据
     */
    private final Map<String, ConfigMetadata> configMetadata = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing ConfigManager...");
        loadAllConfigurations();
        log.info("ConfigManager initialized successfully");
    }

    /**
     * 加载所有配置
     */
    private void loadAllConfigurations() {
        // 获取所有@ConfigurationProperties注解的Bean
        Map<String, Object> configBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
        
        for (Map.Entry<String, Object> entry : configBeans.entrySet()) {
            String beanName = entry.getKey();
            Object configBean = entry.getValue();
            
            ConfigurationProperties annotation = configBean.getClass().getAnnotation(ConfigurationProperties.class);
            if (annotation != null) {
                String prefix = annotation.prefix();
                if (prefix != null && !prefix.isEmpty()) {
                    cacheConfiguration(prefix, configBean);
                    
                    // 创建配置元数据
                    ConfigMetadata metadata = new ConfigMetadata();
                    metadata.setPrefix(prefix);
                    metadata.setBeanName(beanName);
                    metadata.setConfigClass(configBean.getClass());
                    metadata.setValidated(configBean.getClass().isAnnotationPresent(Validated.class));
                    configMetadata.put(prefix, metadata);
                }
            }
        }
    }

    /**
     * 缓存配置
     */
    private void cacheConfiguration(String prefix, Object configBean) {
        lock.writeLock().lock();
        try {
            configCache.put(prefix, configBean);
            log.debug("Cached configuration: {}", prefix);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取配置
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfiguration(String prefix, Class<T> configClass) {
        lock.readLock().lock();
        try {
            Object config = configCache.get(prefix);
            if (config != null && configClass.isInstance(config)) {
                return (T) config;
            }
            
            // 如果缓存中没有，尝试从ApplicationContext获取
            try {
                T bean = applicationContext.getBean(configClass);
                if (bean != null) {
                    cacheConfiguration(prefix, bean);
                    return bean;
                }
            } catch (Exception e) {
                log.debug("Failed to get configuration bean: {}", configClass.getSimpleName(), e);
            }
            
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取配置属性值
     */
    public String getProperty(String key) {
        return environment.getProperty(key);
    }

    /**
     * 获取配置属性值，带默认值
     */
    public String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    /**
     * 获取配置属性值，指定类型
     */
    public <T> T getProperty(String key, Class<T> targetType) {
        return environment.getProperty(key, targetType);
    }

    /**
     * 获取配置属性值，指定类型和默认值
     */
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return environment.getProperty(key, targetType, defaultValue);
    }

    /**
     * 检查属性是否存在
     */
    public boolean containsProperty(String key) {
        return environment.containsProperty(key);
    }

    /**
     * 获取所有配置前缀
     */
    public Set<String> getAllConfigPrefixes() {
        lock.readLock().lock();
        try {
            return new HashSet<>(configCache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取配置元数据
     */
    public ConfigMetadata getConfigMetadata(String prefix) {
        return configMetadata.get(prefix);
    }

    /**
     * 获取所有配置元数据
     */
    public Map<String, ConfigMetadata> getAllConfigMetadata() {
        return new HashMap<>(configMetadata);
    }

    /**
     * 验证配置
     */
    public ValidationResult validateConfiguration(String prefix) {
        ConfigMetadata metadata = configMetadata.get(prefix);
        if (metadata == null) {
            return ValidationResult.error("Configuration not found: " + prefix);
        }
        
        Object config = configCache.get(prefix);
        if (config == null) {
            return ValidationResult.error("Configuration instance not found: " + prefix);
        }
        
        if (validator != null && metadata.isValidated()) {
            Set<ConstraintViolation<Object>> violations = validator.validate(config);
            if (!violations.isEmpty()) {
                List<String> errors = new ArrayList<>();
                for (ConstraintViolation<Object> violation : violations) {
                    errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
                }
                return ValidationResult.error("Validation failed", errors);
            }
        }
        
        return ValidationResult.success();
    }

    /**
     * 验证所有配置
     */
    public Map<String, ValidationResult> validateAllConfigurations() {
        Map<String, ValidationResult> results = new HashMap<>();
        for (String prefix : configCache.keySet()) {
            results.put(prefix, validateConfiguration(prefix));
        }
        return results;
    }

    /**
     * 刷新配置
     */
    public void refreshConfiguration(String prefix) {
        ConfigMetadata metadata = configMetadata.get(prefix);
        if (metadata == null) {
            log.warn("Configuration metadata not found: {}", prefix);
            return;
        }
        
        try {
            // 重新获取配置Bean
            Object newConfig = applicationContext.getBean(metadata.getBeanName());
            Object oldConfig = configCache.get(prefix);
            
            // 更新缓存
            cacheConfiguration(prefix, newConfig);
            
            // 验证新配置
            ValidationResult validationResult = validateConfiguration(prefix);
            if (!validationResult.isValid()) {
                log.error("Configuration validation failed after refresh: {}, errors: {}", 
                    prefix, validationResult.getErrors());
                // 回滚到旧配置
                if (oldConfig != null) {
                    cacheConfiguration(prefix, oldConfig);
                }
                return;
            }
            
            // 通知监听器
            notifyConfigurationChanged(prefix, oldConfig, newConfig);
            
            // 发布配置变更事件
            eventPublisher.publishEvent(new ConfigurationChangedEvent(prefix, oldConfig, newConfig));
            
            log.info("Configuration refreshed successfully: {}", prefix);
            
        } catch (Exception e) {
            log.error("Failed to refresh configuration: {}", prefix, e);
        }
    }

    /**
     * 刷新所有配置
     */
    public void refreshAllConfigurations() {
        for (String prefix : configCache.keySet()) {
            refreshConfiguration(prefix);
        }
    }

    /**
     * 添加配置变更监听器
     */
    public void addConfigChangeListener(String prefix, ConfigChangeListener listener) {
        listeners.computeIfAbsent(prefix, k -> new ArrayList<>()).add(listener);
    }

    /**
     * 移除配置变更监听器
     */
    public void removeConfigChangeListener(String prefix, ConfigChangeListener listener) {
        List<ConfigChangeListener> prefixListeners = listeners.get(prefix);
        if (prefixListeners != null) {
            prefixListeners.remove(listener);
            if (prefixListeners.isEmpty()) {
                listeners.remove(prefix);
            }
        }
    }

    /**
     * 通知配置变更
     */
    private void notifyConfigurationChanged(String prefix, Object oldConfig, Object newConfig) {
        List<ConfigChangeListener> prefixListeners = listeners.get(prefix);
        if (prefixListeners != null) {
            for (ConfigChangeListener listener : prefixListeners) {
                try {
                    listener.onConfigurationChanged(prefix, oldConfig, newConfig);
                } catch (Exception e) {
                    log.error("Error notifying configuration change listener", e);
                }
            }
        }
    }

    /**
     * 获取配置摘要信息
     */
    public ConfigSummary getConfigSummary() {
        ConfigSummary summary = new ConfigSummary();
        summary.setTotalConfigurations(configCache.size());
        summary.setValidatedConfigurations((int) configMetadata.values().stream()
            .filter(ConfigMetadata::isValidated).count());
        
        Map<String, ValidationResult> validationResults = validateAllConfigurations();
        summary.setValidConfigurations((int) validationResults.values().stream()
            .filter(ValidationResult::isValid).count());
        summary.setInvalidConfigurations(summary.getTotalConfigurations() - summary.getValidConfigurations());
        
        return summary;
    }

    /**
     * 配置变更监听器接口
     */
    @FunctionalInterface
    public interface ConfigChangeListener {
        void onConfigurationChanged(String prefix, Object oldConfig, Object newConfig);
    }

    /**
     * 配置元数据
     */
    public static class ConfigMetadata {
        private String prefix;
        private String beanName;
        private Class<?> configClass;
        private boolean validated;
        
        // Getters and Setters
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        
        public String getBeanName() { return beanName; }
        public void setBeanName(String beanName) { this.beanName = beanName; }
        
        public Class<?> getConfigClass() { return configClass; }
        public void setConfigClass(Class<?> configClass) { this.configClass = configClass; }
        
        public boolean isValidated() { return validated; }
        public void setValidated(boolean validated) { this.validated = validated; }
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private List<String> errors;
        
        private ValidationResult(boolean valid, String message, List<String> errors) {
            this.valid = valid;
            this.message = message;
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, "Validation successful", null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message, null);
        }
        
        public static ValidationResult error(String message, List<String> errors) {
            return new ValidationResult(false, message, errors);
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<String> getErrors() { return errors; }
    }

    /**
     * 配置摘要
     */
    public static class ConfigSummary {
        private int totalConfigurations;
        private int validatedConfigurations;
        private int validConfigurations;
        private int invalidConfigurations;
        
        // Getters and Setters
        public int getTotalConfigurations() { return totalConfigurations; }
        public void setTotalConfigurations(int totalConfigurations) { this.totalConfigurations = totalConfigurations; }
        
        public int getValidatedConfigurations() { return validatedConfigurations; }
        public void setValidatedConfigurations(int validatedConfigurations) { this.validatedConfigurations = validatedConfigurations; }
        
        public int getValidConfigurations() { return validConfigurations; }
        public void setValidConfigurations(int validConfigurations) { this.validConfigurations = validConfigurations; }
        
        public int getInvalidConfigurations() { return invalidConfigurations; }
        public void setInvalidConfigurations(int invalidConfigurations) { this.invalidConfigurations = invalidConfigurations; }
    }

    /**
     * 配置变更事件
     */
    public static class ConfigurationChangedEvent {
        private final String prefix;
        private final Object oldConfig;
        private final Object newConfig;
        private final long timestamp;
        
        public ConfigurationChangedEvent(String prefix, Object oldConfig, Object newConfig) {
            this.prefix = prefix;
            this.oldConfig = oldConfig;
            this.newConfig = newConfig;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public String getPrefix() { return prefix; }
        public Object getOldConfig() { return oldConfig; }
        public Object getNewConfig() { return newConfig; }
        public long getTimestamp() { return timestamp; }
    }
}