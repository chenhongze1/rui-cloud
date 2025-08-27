package com.rui.common.core.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础测试类
 * 提供通用的测试方法和断言工具
 *
 * @author rui
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(BaseTestConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class BaseTest {

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected MySQLContainer<?> mysqlContainer;

    @Autowired
    protected GenericContainer<?> redisContainer;

    @Autowired
    protected BaseTestConfiguration.TestProperties testProperties;

    protected TestDataManager testDataManager;
    protected TestAssertions testAssertions;

    @BeforeEach
    void setUp() {
        // 初始化测试数据管理器
        testDataManager = new TestDataManager(dataSource, redisTemplate, objectMapper);
        
        // 初始化测试断言工具
        testAssertions = new TestAssertions();
        
        // 清理测试数据
        cleanupTestData();
        
        // 初始化测试数据
        if (testProperties.isTestDataInitialization()) {
            initializeTestData();
        }
    }

    /**
     * 清理测试数据
     */
    protected void cleanupTestData() {
        // 清理Redis数据
        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        } catch (Exception e) {
            // 忽略清理错误
        }
        
        // 清理数据库数据（由@Transactional自动回滚）
    }

    /**
     * 初始化测试数据
     */
    protected void initializeTestData() {
        // 子类可以重写此方法来初始化特定的测试数据
    }

    /**
     * 等待异步操作完成
     */
    protected void waitForAsyncOperation(long timeout, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(timeout));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("等待异步操作被中断");
        }
    }

    /**
     * 等待条件满足
     */
    protected void waitForCondition(java.util.function.Supplier<Boolean> condition, 
                                   long timeout, TimeUnit unit, String message) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = unit.toMillis(timeout);
        
        while (!condition.get()) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                fail(message != null ? message : "等待条件超时");
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("等待条件被中断");
            }
        }
    }

    /**
     * 断言JSON相等
     */
    protected void assertJsonEquals(String expected, String actual) {
        try {
            Object expectedObj = objectMapper.readValue(expected, Object.class);
            Object actualObj = objectMapper.readValue(actual, Object.class);
            assertEquals(expectedObj, actualObj);
        } catch (Exception e) {
            fail("JSON比较失败: " + e.getMessage());
        }
    }

    /**
     * 断言对象JSON相等
     */
    protected void assertObjectJsonEquals(Object expected, Object actual) {
        try {
            String expectedJson = objectMapper.writeValueAsString(expected);
            String actualJson = objectMapper.writeValueAsString(actual);
            assertJsonEquals(expectedJson, actualJson);
        } catch (Exception e) {
            fail("对象JSON比较失败: " + e.getMessage());
        }
    }

    /**
     * 断言Redis键存在
     */
    protected void assertRedisKeyExists(String key) {
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(key)), "Redis键不存在: " + key);
    }

    /**
     * 断言Redis键不存在
     */
    protected void assertRedisKeyNotExists(String key) {
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(key)), "Redis键存在: " + key);
    }

    /**
     * 断言Redis值相等
     */
    protected void assertRedisValueEquals(String key, Object expected) {
        Object actual = redisTemplate.opsForValue().get(key);
        assertEquals(expected, actual, "Redis值不匹配，键: " + key);
    }

    /**
     * 断言列表不为空且包含元素
     */
    protected <T> void assertListNotEmptyAndContains(List<T> list, T element) {
        assertNotNull(list, "列表不能为null");
        assertFalse(list.isEmpty(), "列表不能为空");
        assertTrue(list.contains(element), "列表不包含期望的元素: " + element);
    }

    /**
     * 断言Map包含键值对
     */
    protected <K, V> void assertMapContains(Map<K, V> map, K key, V value) {
        assertNotNull(map, "Map不能为null");
        assertTrue(map.containsKey(key), "Map不包含键: " + key);
        assertEquals(value, map.get(key), "Map值不匹配，键: " + key);
    }

    /**
     * 断言时间在范围内
     */
    protected void assertTimeInRange(LocalDateTime actual, LocalDateTime start, LocalDateTime end) {
        assertNotNull(actual, "时间不能为null");
        assertTrue(actual.isAfter(start) || actual.isEqual(start), 
                  "时间应该在开始时间之后: " + actual + " vs " + start);
        assertTrue(actual.isBefore(end) || actual.isEqual(end), 
                  "时间应该在结束时间之前: " + actual + " vs " + end);
    }

    /**
     * 断言异常包含消息
     */
    protected void assertExceptionContainsMessage(Exception exception, String expectedMessage) {
        assertNotNull(exception, "异常不能为null");
        assertNotNull(exception.getMessage(), "异常消息不能为null");
        assertTrue(exception.getMessage().contains(expectedMessage), 
                  "异常消息不包含期望的文本: " + expectedMessage + ", 实际消息: " + exception.getMessage());
    }

    /**
     * 断言执行时间在范围内
     */
    protected void assertExecutionTimeInRange(Runnable operation, long minMillis, long maxMillis) {
        long startTime = System.currentTimeMillis();
        operation.run();
        long executionTime = System.currentTimeMillis() - startTime;
        
        assertTrue(executionTime >= minMillis, 
                  "执行时间太短: " + executionTime + "ms, 期望至少: " + minMillis + "ms");
        assertTrue(executionTime <= maxMillis, 
                  "执行时间太长: " + executionTime + "ms, 期望最多: " + maxMillis + "ms");
    }

    /**
     * 创建测试用户数据
     */
    protected TestUser createTestUser(String username, String email) {
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }

    /**
     * 创建测试配置数据
     */
    protected TestConfig createTestConfig(String key, String value) {
        TestConfig config = new TestConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setCreateTime(LocalDateTime.now());
        return config;
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
     * 测试断言工具类
     */
    public static class TestAssertions {
        
        public void assertValidationError(Runnable operation, String expectedField) {
            try {
                operation.run();
                fail("期望验证异常但没有抛出");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains(expectedField), 
                          "验证异常消息不包含期望的字段: " + expectedField);
            }
        }
        
        public void assertNoException(Runnable operation) {
            try {
                operation.run();
            } catch (Exception e) {
                fail("不期望异常但抛出了: " + e.getMessage());
            }
        }
        
        public <T extends Exception> void assertSpecificException(Runnable operation, Class<T> exceptionClass) {
            try {
                operation.run();
                fail("期望异常 " + exceptionClass.getSimpleName() + " 但没有抛出");
            } catch (Exception e) {
                assertTrue(exceptionClass.isInstance(e), 
                          "期望异常类型 " + exceptionClass.getSimpleName() + 
                          " 但实际是 " + e.getClass().getSimpleName());
            }
        }
    }
}