package com.rui.common.core.config;

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
        
        // 初始化线程池
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "dynamic-config-updater");
            thread.setDaemon(true);
            return thread;
        });
        
        // 添加动态配置源
        addDynamicPropertySource();
        
        log.info("DynamicConfigUpdater initialized successfully");
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
            List<String> failedKeys = new ArrayList<>();
            
            // 验证所有属性
            if (validate) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (!validatePropertyValue(entry.getKey(), entry.getValue())) {
                        failedKeys.add(entry.getKey());
                    }
                }
                
                if (!failedKeys.isEmpty()) {
                    return UpdateResult.error("Validation failed for properties: " + failedKeys);
                }
            }
            
            // 更新所有属性
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
                
                // 通知监听器
                notifyUpdateListeners(key, oldValue, newValue);
            }
            
            // 发布批量更新事件
            eventPublisher.publishEvent(new BatchPropertyUpdateEvent(properties, oldValues));
            
            log.info("Batch property update completed: {} properties updated", properties.size());
            
            return UpdateResult.success("Batch property update completed successfully");
            
        } catch (Exception e) {
            log.error("Failed to update properties in batch", e);
            return UpdateResult.error("Failed to update properties: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 刷新配置
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
                // 使用Spring Cloud的配置刷新
                Set<String> refreshedKeys = contextRefresher.refresh();
                log.info("Configuration refreshed, updated keys: {}", refreshedKeys);
                
                // 如果有ConfigManager，也刷新它
                if (configManager != null) {
                    if (prefix != null) {
                        configManager.refreshConfiguration(prefix);
                    } else {
                        configManager.refreshAllConfigurations();
                    }
                }
                
                return UpdateResult.success("Configuration refreshed successfully");
            } else {
                // 手动刷新配置
                if (configManager != null) {
                    if (prefix != null) {
                        configManager.refreshConfiguration(prefix);
                    } else {
                        configManager.refreshAllConfigurations();
                    }
                    return UpdateResult.success("Configuration refreshed manually");
                } else {
                    return UpdateResult.warning("No refresh mechanism available");
                }
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
            // 基本验证
            if (key.contains("password") || key.contains("secret")) {
                if (value instanceof String) {
                    String strValue = (String) value;
                    if (strValue.length() < 8) {
                        log.warn("Security property {} has weak value", key);
                        return false;
                    }
                }
            }
            
            // 端口号验证
            if (key.contains("port") && value instanceof Number) {
                int port = ((Number) value).intValue();
                if (port < 1 || port > 65535) {
                    log.warn("Invalid port number: {}", port);
                    return false;
                }
            }
            
            // 使用ConfigValidator进行验证
            if (configValidator != null) {
                // 这里可以扩展更复杂的验证逻辑
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error validating property: {}", key, e);
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
        if (updateHistory.size() > 1000) {
            updateHistory.subList(0, updateHistory.size() - 1000).clear();
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
        ScheduledFuture<?> existingTask = refreshTasks.get(configPrefix);
        if (existingTask != null) {
            existingTask.cancel(false);
        }
        
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    refreshConfiguration(configPrefix);
                } catch (Exception e) {
                    log.error("Error during scheduled config refresh", e);
                }
            },
            intervalSeconds,
            intervalSeconds,
            TimeUnit.SECONDS
        );
        
        refreshTasks.put(configPrefix, task);
        log.info("Scheduled config refresh for prefix: {}, interval: {}s", configPrefix, intervalSeconds);
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
     * 获取更新历史（指定数量）
     */
    public List<ConfigUpdateRecord> getUpdateHistory(int limit) {
        List<ConfigUpdateRecord> history = new ArrayList<>(updateHistory);
        if (history.size() <= limit) {
            return history;
        }
        return history.subList(history.size() - limit, history.size());
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
        
        // Getters
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
        
        // Getters and Setters
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
        
        // Getters
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
        
        // Getters
        public Map<String, Object> getNewValues() { return newValues; }
        public Map<String, Object> getOldValues() { return oldValues; }
        public long getTimestamp() { return timestamp; }
    }
}