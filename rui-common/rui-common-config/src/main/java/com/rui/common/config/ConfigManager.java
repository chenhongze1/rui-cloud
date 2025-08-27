package com.rui.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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
@EnableConfigurationProperties
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
     * 配置属性映射
     */
    private final Map<String, ConfigurationProperties> configPropertiesMap = new ConcurrentHashMap<>();
    
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
     * 添加配置变更监听器
     */
    public void addConfigChangeListener(String prefix, ConfigChangeListener listener) {
        listeners.computeIfAbsent(prefix, k -> new ArrayList<>()).add(listener);
        log.debug("Added config change listener for prefix: {}", prefix);
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
    private void notifyConfigChange(String prefix, Object oldValue, Object newValue) {
        List<ConfigChangeListener> prefixListeners = listeners.get(prefix);
        if (prefixListeners != null) {
            ConfigChangeEvent event = new ConfigChangeEvent(prefix, oldValue, newValue);
            for (ConfigChangeListener listener : prefixListeners) {
                try {
                    listener.onConfigChange(event);
                } catch (Exception e) {
                    log.error("Error notifying config change listener", e);
                }
            }
        }

        // 发布Spring事件
        eventPublisher.publishEvent(new ConfigChangeEvent(prefix, oldValue, newValue));
    }

    /**
     * 刷新配置
     */
    public void refreshConfiguration(String prefix) {
        lock.writeLock().lock();
        try {
            Object oldValue = configCache.get(prefix);
            
            // 重新加载配置
            ConfigMetadata metadata = configMetadata.get(prefix);
            if (metadata != null) {
                try {
                    Object newConfig = applicationContext.getBean(metadata.getBeanName());
                    configCache.put(prefix, newConfig);
                    
                    // 通知变更
                    notifyConfigChange(prefix, oldValue, newConfig);
                    
                    log.info("Refreshed configuration: {}", prefix);
                } catch (Exception e) {
                    log.error("Failed to refresh configuration: {}", prefix, e);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 验证配置
     */
    public boolean validateConfiguration(String prefix) {
        if (validator == null) {
            log.warn("Validator not available, skipping validation for: {}", prefix);
            return true;
        }

        Object config = configCache.get(prefix);
        if (config == null) {
            return false;
        }

        Set<ConstraintViolation<Object>> violations = validator.validate(config);
        if (!violations.isEmpty()) {
            log.error("Configuration validation failed for {}: {}", prefix, violations);
            return false;
        }

        return true;
    }
    
    /**
     * 获取激活的配置文件
     */
    public String[] getActiveProfiles() {
        return environment.getActiveProfiles();
    }
    
    /**
     * 获取默认配置文件
     */
    public String[] getDefaultProfiles() {
        return environment.getDefaultProfiles();
    }
    
    /**
     * 检查是否接受指定的配置文件
     */
    public boolean acceptsProfiles(String... profiles) {
        return environment.acceptsProfiles(profiles);
    }
    
    /**
     * 缓存配置值
     */
    public void cacheProperty(String key, Object value) {
        configCache.put(key, value);
        log.debug("缓存配置: {} = {}", key, value);
    }
    
    /**
     * 从缓存获取配置值
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedProperty(String key, Class<T> type) {
        Object value = configCache.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * 清除缓存的配置
     */
    public void clearCache() {
        configCache.clear();
        log.info("配置缓存已清除");
    }
    
    /**
     * 清除指定的缓存配置
     */
    public void clearCachedProperty(String key) {
        configCache.remove(key);
        log.debug("清除缓存配置: {}", key);
    }
    
    /**
     * 获取所有缓存的配置
     */
    public Map<String, Object> getAllCachedProperties() {
        return new ConcurrentHashMap<>(configCache);
    }
    
    /**
     * 刷新配置（重新加载）
     */
    public void refreshConfig() {
        // 清除缓存，强制重新从环境中读取
        clearCache();
        log.info("配置已刷新");
    }
    
    /**
     * 获取配置的字符串表示
     */
    public String getConfigInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Active Profiles: ").append(String.join(", ", getActiveProfiles())).append("\n");
        sb.append("Default Profiles: ").append(String.join(", ", getDefaultProfiles())).append("\n");
        sb.append("Cached Properties: ").append(configCache.size());
        return sb.toString();
    }

    /**
     * 根据前缀获取配置
     */
    public Map<String, Object> getConfigByPrefix(String prefix) {
        Map<String, Object> result = new HashMap<>();
        lock.readLock().lock();
        try {
            for (Map.Entry<String, Object> entry : configCache.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    /**
     * 验证所有配置
     */
    public ValidationResult validateAllConfigurations() {
        ValidationResult result = new ValidationResult();
        lock.readLock().lock();
        try {
            for (String prefix : configMetadata.keySet()) {
                boolean isValid = validateConfiguration(prefix);
                if (isValid) {
                    result.addSuccess(prefix);
                } else {
                    result.addError(prefix, "Validation failed");
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    /**
     * 验证所有配置并返回Map格式的结果
     */
    public Map<String, ValidationResult> validateAllConfigurationsAsMap() {
        Map<String, ValidationResult> results = new HashMap<>();
        lock.readLock().lock();
        try {
            for (String prefix : configMetadata.keySet()) {
                ValidationResult result = new ValidationResult();
                boolean isValid = validateConfiguration(prefix);
                if (isValid) {
                    result.addSuccess(prefix);
                } else {
                    result.addError(prefix, "Validation failed");
                }
                results.put(prefix, result);
            }
        } finally {
            lock.readLock().unlock();
        }
        return results;
    }

    /**
     * 获取配置摘要信息
     */
    public Map<String, Object> getConfigSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalConfigurations", configCache.size());
        summary.put("activeProfiles", Arrays.asList(getActiveProfiles()));
        summary.put("defaultProfiles", Arrays.asList(getDefaultProfiles()));
        summary.put("configPrefixes", getAllConfigPrefixes());
        summary.put("cachedProperties", configCache.size());
        summary.put("listeners", listeners.size());
        return summary;
    }

    /**
     * 配置变更监听器接口
     */
    public interface ConfigChangeListener {
        void onConfigChange(ConfigChangeEvent event);
    }

    /**
     * 配置变更事件
     */
    public static class ConfigChangeEvent {
        private final String prefix;
        private final Object oldValue;
        private final Object newValue;
        private final long timestamp;

        public ConfigChangeEvent(String prefix, Object oldValue, Object newValue) {
            this.prefix = prefix;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.timestamp = System.currentTimeMillis();
        }

        public String getPrefix() {
            return prefix;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 配置元数据
     */
    public static class ConfigMetadata {
        private String prefix;
        private String beanName;
        private Class<?> configClass;
        private boolean validated;

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getBeanName() {
            return beanName;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        public Class<?> getConfigClass() {
            return configClass;
        }

        public void setConfigClass(Class<?> configClass) {
            this.configClass = configClass;
        }

        public boolean isValidated() {
            return validated;
        }

        public void setValidated(boolean validated) {
            this.validated = validated;
        }
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final List<String> successList = new ArrayList<>();
        private final Map<String, String> errorMap = new HashMap<>();

        public void addSuccess(String prefix) {
            successList.add(prefix);
        }

        public void addError(String prefix, String error) {
            errorMap.put(prefix, error);
        }

        public List<String> getSuccessList() {
            return new ArrayList<>(successList);
        }

        public Map<String, String> getErrorMap() {
            return new HashMap<>(errorMap);
        }

        public boolean hasErrors() {
            return !errorMap.isEmpty();
        }

        public boolean isValid() {
            return !hasErrors();
        }

        public int getSuccessCount() {
            return successList.size();
        }

        public int getErrorCount() {
            return errorMap.size();
        }
    }
}