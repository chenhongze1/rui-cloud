package com.rui.common.config;

import com.rui.common.config.ConfigValidator.ValidationResult;
import com.rui.common.config.properties.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 动态配置更新器
 * 支持运行时配置的动态更新和热重载功能
 *
 * @author rui
 */
@Slf4j
@Component
@RefreshScope
public class DynamicConfigUpdater {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private ConfigurableEnvironment environment;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired(required = false)
    private ContextRefresher contextRefresher;
    
    @Autowired(required = false)
    private ConfigManager configManager;
    
    @Autowired(required = false)
    private ConfigValidator configValidator;
    
    @Autowired
    private ConfigProperties configProperties;
    
    /**
     * 动态配置存储
     */
    private final Map<String, Object> dynamicProperties = new ConcurrentHashMap<>();
    
    /**
     * 配置更新监听器
     */
    private final Map<String, List<ConfigUpdateListener>> updateListeners = new ConcurrentHashMap<>();
    
    /**
     * 配置更新历史
     */
    private final List<ConfigUpdateRecord> updateHistory = new CopyOnWriteArrayList<>();
    
    /**
     * 读写锁
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 定时任务执行器
     */
    private ScheduledExecutorService scheduler;
    
    /**
     * 配置刷新任务
     */
    private final Map<String, ScheduledFuture<?>> refreshTasks = new ConcurrentHashMap<>();
    
    /**
     * 动态配置源名称
     */
    private static final String DYNAMIC_PROPERTY_SOURCE_NAME = "dynamicConfigProperties";

    @PostConstruct
    public void init() {
        log.info("Initializing DynamicConfigUpdater...");
        
        // 检查动态配置是否启用
        if (!configProperties.getDynamic().isEnabled()) {
            log.info("Dynamic configuration is disabled");
            return;
        }
        
        // 初始化线程池
        int threadPoolSize = getThreadPoolSize();
        scheduler = Executors.newScheduledThreadPool(threadPoolSize, r -> {
            Thread thread = new Thread(r, "dynamic-config-updater");
            thread.setDaemon(true);
            return thread;
        });
        
        // 添加动态配置源
        addDynamicPropertySource();
        
        // 启动定时刷新任务
        if (configProperties.getDynamic().getRefreshInterval() > 0) {
            scheduleRefresh("*", configProperties.getDynamic().getRefreshInterval());
        }
        
        log.info("DynamicConfigUpdater initialized successfully with thread pool size: {}", threadPoolSize);
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 添加动态配置源
     */
    private void addDynamicPropertySource() {
        MutablePropertySources propertySources = environment.getPropertySources();
        if (!propertySources.contains(DYNAMIC_PROPERTY_SOURCE_NAME)) {
            MapPropertySource dynamicPropertySource = new MapPropertySource(
                DYNAMIC_PROPERTY_SOURCE_NAME, dynamicProperties);
            propertySources.addFirst(dynamicPropertySource);
            log.debug("Added dynamic property source: {}", DYNAMIC_PROPERTY_SOURCE_NAME);
        }
    }

    /**
     * 更新单个配置属性
     */
    public UpdateResult updateProperty(String key, Object value) {
        return updateProperty(key, value, true);
    }

    /**
     * 更新单个配置属性
     */
    public UpdateResult updateProperty(String key, Object value, boolean validate) {
        if (key == null || key.trim().isEmpty()) {
            return UpdateResult.error("Property key cannot be null or empty");
        }
        
        lock.writeLock().lock();
        try {
            Object oldValue = dynamicProperties.get(key);
            
            // 验证新值
            if (validate && !validatePropertyValue(key, value)) {
                return UpdateResult.error("Property validation failed for key: " + key);
            }
            
            // 更新属性
            if (value == null) {
                dynamicProperties.remove(key);
            } else {
                dynamicProperties.put(key, value);
            }
            
            // 记录更新历史
            recordUpdate(key, oldValue, value);
            
            // 通知监听器
            notifyUpdateListeners(key, oldValue, value);
            
            // 发布事件
            eventPublisher.publishEvent(new PropertyUpdateEvent(key, oldValue, value));
            
            log.info("Property updated: {} = {} (old: {})", key, value, oldValue);
            
            return UpdateResult.success("Property updated successfully");
            
        } catch (Exception e) {
            log.error("Failed to update property: {}", key, e);
            return UpdateResult.error("Failed to update property: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量更新配置属性
     */
    public UpdateResult updateProperties(Map<String, Object> properties) {
        return updateProperties(properties, true);
    }

    /**
     * 批量更新配置属性
     */
    public UpdateResult updateProperties(Map<String, Object> properties, boolean validate) {
        if (properties == null || properties.isEmpty()) {
            return UpdateResult.error("Properties map cannot be null or empty");
        }
        
        lock.writeLock().lock();
        try {
            Map<String, Object> oldValues = new HashMap<>();
            
            // 验证所有属性
            if (validate) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (!validatePropertyValue(entry.getKey(), entry.getValue())) {
                        return UpdateResult.error("Property validation failed for key: " + entry.getKey());
                    }
                }
            }
            
            // 批量更新
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String key = entry.getKey();
                Object newValue = entry.getValue();
                Object oldValue = dynamicProperties.get(key);
                
                oldValues.put(key, oldValue);
                
                if (newValue == null) {
                    dynamicProperties.remove(key);
                } else {
                    dynamicProperties.put(key, newValue);
                }
                
                // 记录更新历史
                recordUpdate(key, oldValue, newValue);
            }
            
            // 批量通知监听器
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String key = entry.getKey();
                Object oldValue = oldValues.get(key);
                Object newValue = entry.getValue();
                notifyUpdateListeners(key, oldValue, newValue);
            }
            
            // 发布批量更新事件
            eventPublisher.publishEvent(new BatchPropertyUpdateEvent(properties, oldValues));
            
            log.info("Batch properties updated: {} properties", properties.size());
            
            return UpdateResult.success("Batch properties updated successfully");
            
        } catch (Exception e) {
            log.error("Failed to update properties", e);
            return UpdateResult.error("Failed to update properties: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 刷新所有配置
     */
    public UpdateResult refreshConfiguration() {
        return refreshConfiguration(null);
    }

    /**
     * 刷新指定前缀的配置
     */
    public UpdateResult refreshConfiguration(String prefix) {
        try {
            if (contextRefresher != null) {
                Set<String> refreshedKeys = contextRefresher.refresh();
                
                if (prefix != null) {
                    refreshedKeys = refreshedKeys.stream()
                        .filter(key -> key.startsWith(prefix))
                        .collect(java.util.stream.Collectors.toSet());
                }
                
                log.info("Configuration refreshed: {} keys", refreshedKeys.size());
                
                return UpdateResult.success("Configuration refreshed successfully: " + refreshedKeys.size() + " keys");
            } else {
                log.warn("ContextRefresher not available, cannot refresh configuration");
                return UpdateResult.warning("ContextRefresher not available");
            }
        } catch (Exception e) {
            log.error("Failed to refresh configuration", e);
            return UpdateResult.error("Failed to refresh configuration: " + e.getMessage());
        }
    }

    /**
     * 验证属性值
     */
    private boolean validatePropertyValue(String key, Object value) {
        try {
            // 检查是否启用验证
            if (!configProperties.getValidation().isEnabled()) {
                return true;
            }
            
            if (configValidator != null) {
                ValidationResult result = configValidator.validateProperty(key, value);
                return result.isValid();
            } else {
                // 基本验证
                if (key == null || key.trim().isEmpty()) {
                    return false;
                }
                
                // 检查是否在允许的配置键列表中
                List<String> allowedKeys = getAllowedKeys();
                if (!allowedKeys.isEmpty() && !allowedKeys.contains(key)) {
                    log.warn("Property key not in allowed list: {}", key);
                    return false;
                }
                
                // 可以添加更多基本验证逻辑
                return true;
            }
        } catch (Exception e) {
            log.error("Error validating property: {} = {}", key, value, e);
            return false;
        }
    }

    /**
     * 记录更新历史
     */
    private void recordUpdate(String key, Object oldValue, Object newValue) {
        ConfigUpdateRecord record = new ConfigUpdateRecord();
        record.setKey(key);
        record.setOldValue(oldValue);
        record.setNewValue(newValue);
        record.setTimestamp(System.currentTimeMillis());
        record.setThread(Thread.currentThread().getName());
        
        updateHistory.add(record);
        
        // 保持历史记录数量在合理范围内
        int maxHistorySize = getHistory();
        if (updateHistory.size() > maxHistorySize) {
            updateHistory.remove(0);
        }
    }

    /**
     * 通知更新监听器
     */
    private void notifyUpdateListeners(String key, Object oldValue, Object newValue) {
        List<ConfigUpdateListener> listeners = updateListeners.get(key);
        if (listeners != null) {
            for (ConfigUpdateListener listener : listeners) {
                try {
                    listener.onConfigUpdate(key, oldValue, newValue);
                } catch (Exception e) {
                    log.error("Error notifying config update listener", e);
                }
            }
        }
        
        // 通知通配符监听器
        List<ConfigUpdateListener> wildcardListeners = updateListeners.get("*");
        if (wildcardListeners != null) {
            for (ConfigUpdateListener listener : wildcardListeners) {
                try {
                    listener.onConfigUpdate(key, oldValue, newValue);
                } catch (Exception e) {
                    log.error("Error notifying wildcard config update listener", e);
                }
            }
        }
    }

    /**
     * 添加配置更新监听器
     */
    public void addUpdateListener(String key, ConfigUpdateListener listener) {
        updateListeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * 移除配置更新监听器
     */
    public void removeUpdateListener(String key, ConfigUpdateListener listener) {
        List<ConfigUpdateListener> listeners = updateListeners.get(key);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                updateListeners.remove(key);
            }
        }
    }

    /**
     * 定时刷新配置
     */
    public void scheduleRefresh(String configPrefix, long intervalSeconds) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("Interval must be positive");
        }
        
        // 取消已存在的任务
        cancelScheduledRefresh(configPrefix);
        
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    refreshConfiguration(configPrefix);
                } catch (Exception e) {
                    log.error("Error in scheduled config refresh for prefix: {}", configPrefix, e);
                }
            },
            intervalSeconds,
            intervalSeconds,
            TimeUnit.SECONDS
        );
        
        refreshTasks.put(configPrefix, task);
        log.info("Scheduled config refresh for prefix: {} every {} seconds", configPrefix, intervalSeconds);
    }

    /**
     * 取消定时刷新
     */
    public void cancelScheduledRefresh(String configPrefix) {
        ScheduledFuture<?> task = refreshTasks.remove(configPrefix);
        if (task != null) {
            task.cancel(false);
            log.info("Cancelled scheduled config refresh for prefix: {}", configPrefix);
        }
    }

    /**
     * 获取当前动态配置
     */
    public Map<String, Object> getCurrentDynamicProperties() {
        lock.readLock().lock();
        try {
            return new HashMap<>(dynamicProperties);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取更新历史
     */
    public List<ConfigUpdateRecord> getUpdateHistory() {
        return new ArrayList<>(updateHistory);
    }

    /**
     * 获取指定数量的更新历史
     */
    public List<ConfigUpdateRecord> getUpdateHistory(int limit) {
        List<ConfigUpdateRecord> history = new ArrayList<>(updateHistory);
        if (history.size() <= limit) {
            return history;
        }
        return history.subList(history.size() - limit, history.size());
    }

    /**
     * 获取线程池大小
     */
    private int getThreadPoolSize() {
        return configProperties.getDynamic().getThreadPoolSize();
    }

    /**
     * 获取允许的配置键列表
     */
    private List<String> getAllowedKeys() {
        return configProperties.getValidation().getAllowedKeys();
    }

    /**
     * 获取历史记录最大数量
     */
    private int getHistory() {
        return configProperties.getHistory().getMaxSize();
    }

    /**
     * 配置更新监听器接口
     */
    @FunctionalInterface
    public interface ConfigUpdateListener {
        void onConfigUpdate(String key, Object oldValue, Object newValue);
    }

    /**
     * 更新结果
     */
    public static class UpdateResult {
        private boolean success;
        private String message;
        private UpdateLevel level;
        
        private UpdateResult(boolean success, String message, UpdateLevel level) {
            this.success = success;
            this.message = message;
            this.level = level;
        }
        
        public static UpdateResult success(String message) {
            return new UpdateResult(true, message, UpdateLevel.INFO);
        }
        
        public static UpdateResult error(String message) {
            return new UpdateResult(false, message, UpdateLevel.ERROR);
        }
        
        public static UpdateResult warning(String message) {
            return new UpdateResult(true, message, UpdateLevel.WARNING);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public UpdateLevel getLevel() { return level; }
    }

    /**
     * 更新级别
     */
    public enum UpdateLevel {
        INFO, WARNING, ERROR
    }

    /**
     * 配置更新记录
     */
    public static class ConfigUpdateRecord {
        private String key;
        private Object oldValue;
        private Object newValue;
        private long timestamp;
        private String thread;
        
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public Object getOldValue() { return oldValue; }
        public void setOldValue(Object oldValue) { this.oldValue = oldValue; }
        
        public Object getNewValue() { return newValue; }
        public void setNewValue(Object newValue) { this.newValue = newValue; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public String getThread() { return thread; }
        public void setThread(String thread) { this.thread = thread; }
    }

    /**
     * 属性更新事件
     */
    public static class PropertyUpdateEvent {
        private final String key;
        private final Object oldValue;
        private final Object newValue;
        private final long timestamp;

        public PropertyUpdateEvent(String key, Object oldValue, Object newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getKey() { return key; }
        public Object getOldValue() { return oldValue; }
        public Object getNewValue() { return newValue; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 批量属性更新事件
     */
    public static class BatchPropertyUpdateEvent {
        private final Map<String, Object> newValues;
        private final Map<String, Object> oldValues;
        private final long timestamp;

        public BatchPropertyUpdateEvent(Map<String, Object> newValues, Map<String, Object> oldValues) {
            this.newValues = new HashMap<>(newValues);
            this.oldValues = new HashMap<>(oldValues);
            this.timestamp = System.currentTimeMillis();
        }
        
        public Map<String, Object> getNewValues() { return newValues; }
        public Map<String, Object> getOldValues() { return oldValues; }
        public long getTimestamp() { return timestamp; }
    }
}