package com.rui.common.core.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 测试数据管理器
 * 提供测试数据的创建、管理和清理功能
 *
 * @author rui
 */
@Slf4j
public class TestDataManager {

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private final Map<String, Object> testDataCache = new HashMap<>();
    private final List<String> createdRedisKeys = new ArrayList<>();
    private final List<String> executedSqlStatements = new ArrayList<>();

    public TestDataManager(DataSource dataSource, 
                          RedisTemplate<String, Object> redisTemplate, 
                          ObjectMapper objectMapper) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 从文件加载测试数据
     */
    public void loadTestDataFromFile(String filePath) {
        try {
            ClassPathResource resource = new ClassPathResource(filePath);
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
            if (filePath.endsWith(".json")) {
                loadJsonTestData(content);
            } else if (filePath.endsWith(".sql")) {
                loadSqlTestData(content);
            } else {
                log.warn("不支持的测试数据文件格式: {}", filePath);
            }
        } catch (IOException e) {
            log.error("加载测试数据文件失败: {}", filePath, e);
            throw new RuntimeException("加载测试数据失败", e);
        }
    }

    /**
     * 加载JSON测试数据
     */
    private void loadJsonTestData(String jsonContent) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(jsonContent, Map.class);
            
            // 加载Redis数据
            if (data.containsKey("redis")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> redisData = (Map<String, Object>) data.get("redis");
                loadRedisData(redisData);
            }
            
            // 加载数据库数据
            if (data.containsKey("database")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dbData = (Map<String, Object>) data.get("database");
                loadDatabaseData(dbData);
            }
            
            // 缓存测试数据
            testDataCache.putAll(data);
            
        } catch (Exception e) {
            log.error("解析JSON测试数据失败", e);
            throw new RuntimeException("解析JSON测试数据失败", e);
        }
    }

    /**
     * 加载SQL测试数据
     */
    private void loadSqlTestData(String sqlContent) {
        String[] statements = sqlContent.split(";");
        
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                try {
                    jdbcTemplate.execute(trimmed);
                    executedSqlStatements.add(trimmed);
                    log.debug("执行SQL语句: {}", trimmed);
                } catch (Exception e) {
                    log.error("执行SQL语句失败: {}", trimmed, e);
                    throw new RuntimeException("执行SQL语句失败", e);
                }
            }
        }
    }

    /**
     * 加载Redis数据
     */
    private void loadRedisData(Map<String, Object> redisData) {
        redisData.forEach((key, value) -> {
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> valueMap = (Map<String, Object>) value;
                
                if (valueMap.containsKey("value")) {
                    Object actualValue = valueMap.get("value");
                    
                    // 设置过期时间
                    if (valueMap.containsKey("expiration")) {
                        long expiration = ((Number) valueMap.get("expiration")).longValue();
                        redisTemplate.opsForValue().set(key, actualValue, Duration.ofSeconds(expiration));
                    } else {
                        redisTemplate.opsForValue().set(key, actualValue);
                    }
                } else {
                    redisTemplate.opsForValue().set(key, value);
                }
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            
            createdRedisKeys.add(key);
            log.debug("设置Redis键值: {} = {}", key, value);
        });
    }

    /**
     * 加载数据库数据
     */
    private void loadDatabaseData(Map<String, Object> dbData) {
        dbData.forEach((tableName, tableData) -> {
            if (tableData instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> records = (List<Map<String, Object>>) tableData;
                
                for (Map<String, Object> record : records) {
                    insertRecord(tableName, record);
                }
            }
        });
    }

    /**
     * 插入数据库记录
     */
    private void insertRecord(String tableName, Map<String, Object> record) {
        if (record.isEmpty()) {
            return;
        }
        
        List<String> columns = new ArrayList<>(record.keySet());
        List<Object> values = new ArrayList<>(record.values());
        
        String sql = String.format(
            "INSERT INTO %s (%s) VALUES (%s)",
            tableName,
            String.join(", ", columns),
            String.join(", ", Collections.nCopies(columns.size(), "?"))
        );
        
        try {
            jdbcTemplate.update(sql, values.toArray());
            executedSqlStatements.add(sql + " -- values: " + values);
            log.debug("插入数据到表 {}: {}", tableName, record);
        } catch (Exception e) {
            log.error("插入数据失败，表: {}, 数据: {}", tableName, record, e);
            throw new RuntimeException("插入数据失败", e);
        }
    }

    /**
     * 创建随机测试用户
     */
    public TestUser createRandomUser() {
        TestUser user = new TestUser();
        user.setId(generateRandomId());
        user.setUsername("user_" + generateRandomString(8));
        user.setEmail(generateRandomString(10) + "@test.com");
        user.setCreateTime(LocalDateTime.now());
        
        // 缓存用户数据
        testDataCache.put("user_" + user.getId(), user);
        
        return user;
    }

    /**
     * 创建随机测试配置
     */
    public TestConfig createRandomConfig() {
        TestConfig config = new TestConfig();
        config.setId(generateRandomId());
        config.setConfigKey("config." + generateRandomString(10));
        config.setConfigValue(generateRandomString(20));
        config.setCreateTime(LocalDateTime.now());
        
        // 缓存配置数据
        testDataCache.put("config_" + config.getId(), config);
        
        return config;
    }

    /**
     * 设置Redis测试数据
     */
    public void setRedisTestData(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        createdRedisKeys.add(key);
        log.debug("设置Redis测试数据: {} = {}", key, value);
    }

    /**
     * 设置Redis测试数据（带过期时间）
     */
    public void setRedisTestData(String key, Object value, Duration expiration) {
        redisTemplate.opsForValue().set(key, value, expiration);
        createdRedisKeys.add(key);
        log.debug("设置Redis测试数据（过期时间{}）: {} = {}", expiration, key, value);
    }

    /**
     * 批量设置Redis测试数据
     */
    public void setRedisTestDataBatch(Map<String, Object> data) {
        data.forEach(this::setRedisTestData);
    }

    /**
     * 获取缓存的测试数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedTestData(String key, Class<T> type) {
        Object data = testDataCache.get(key);
        if (data != null && type.isInstance(data)) {
            return (T) data;
        }
        return null;
    }

    /**
     * 缓存测试数据
     */
    public void cacheTestData(String key, Object data) {
        testDataCache.put(key, data);
    }

    /**
     * 清理所有测试数据
     */
    public void cleanupAllTestData() {
        cleanupRedisTestData();
        cleanupCachedTestData();
        log.info("清理所有测试数据完成");
    }

    /**
     * 清理Redis测试数据
     */
    public void cleanupRedisTestData() {
        for (String key : createdRedisKeys) {
            try {
                redisTemplate.delete(key);
                log.debug("删除Redis测试数据: {}", key);
            } catch (Exception e) {
                log.warn("删除Redis测试数据失败: {}", key, e);
            }
        }
        createdRedisKeys.clear();
    }

    /**
     * 清理缓存的测试数据
     */
    public void cleanupCachedTestData() {
        testDataCache.clear();
    }

    /**
     * 验证测试数据完整性
     */
    public boolean validateTestDataIntegrity() {
        try {
            // 验证Redis数据
            for (String key : createdRedisKeys) {
                if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    log.warn("Redis测试数据丢失: {}", key);
                    return false;
                }
            }
            
            // 验证缓存数据
            if (testDataCache.isEmpty()) {
                log.warn("测试数据缓存为空");
            }
            
            return true;
        } catch (Exception e) {
            log.error("验证测试数据完整性失败", e);
            return false;
        }
    }

    /**
     * 获取测试数据统计信息
     */
    public TestDataStats getTestDataStats() {
        TestDataStats stats = new TestDataStats();
        stats.setRedisKeysCount(createdRedisKeys.size());
        stats.setCachedDataCount(testDataCache.size());
        stats.setExecutedSqlCount(executedSqlStatements.size());
        stats.setCreatedAt(LocalDateTime.now());
        return stats;
    }

    /**
     * 生成随机ID
     */
    private Long generateRandomId() {
        return ThreadLocalRandom.current().nextLong(1, 1000000);
    }

    /**
     * 生成随机字符串
     */
    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = ThreadLocalRandom.current().nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        
        return sb.toString();
    }

    /**
     * 测试用户实体
     */
    public static class TestUser {
        private Long id;
        private String username;
        private String email;
        private LocalDateTime createTime;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }

    /**
     * 测试配置实体
     */
    public static class TestConfig {
        private Long id;
        private String configKey;
        private String configValue;
        private LocalDateTime createTime;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getConfigKey() { return configKey; }
        public void setConfigKey(String configKey) { this.configKey = configKey; }
        public String getConfigValue() { return configValue; }
        public void setConfigValue(String configValue) { this.configValue = configValue; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }

    /**
     * 测试数据统计信息
     */
    public static class TestDataStats {
        private int redisKeysCount;
        private int cachedDataCount;
        private int executedSqlCount;
        private LocalDateTime createdAt;

        // Getters and Setters
        public int getRedisKeysCount() { return redisKeysCount; }
        public void setRedisKeysCount(int redisKeysCount) { this.redisKeysCount = redisKeysCount; }
        public int getCachedDataCount() { return cachedDataCount; }
        public void setCachedDataCount(int cachedDataCount) { this.cachedDataCount = cachedDataCount; }
        public int getExecutedSqlCount() { return executedSqlCount; }
        public void setExecutedSqlCount(int executedSqlCount) { this.executedSqlCount = executedSqlCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}