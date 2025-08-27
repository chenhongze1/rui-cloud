package com.rui.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置管理器
 * 用于管理应用程序的配置信息
 *
 * @author rui
 * @since 1.0.0
 */
@Component
public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    @Autowired
    private Environment environment;
    
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    
    /**
     * 获取配置值
     */
    public String getProperty(String key) {
        return environment.getProperty(key);
    }
    
    /**
     * 获取配置值（带默认值）
     */
    public String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }
    
    /**
     * 获取配置值并转换为指定类型
     */
    public <T> T getProperty(String key, Class<T> targetType) {
        return environment.getProperty(key, targetType);
    }
    
    /**
     * 获取配置值并转换为指定类型（带默认值）
     */
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return environment.getProperty(key, targetType, defaultValue);
    }
    
    /**
     * 检查配置是否存在
     */
    public boolean containsProperty(String key) {
        return environment.containsProperty(key);
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
        logger.debug("缓存配置: {} = {}", key, value);
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
        logger.info("配置缓存已清除");
    }
    
    /**
     * 清除指定的缓存配置
     */
    public void clearCachedProperty(String key) {
        configCache.remove(key);
        logger.debug("清除缓存配置: {}", key);
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
        logger.info("配置已刷新");
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
}