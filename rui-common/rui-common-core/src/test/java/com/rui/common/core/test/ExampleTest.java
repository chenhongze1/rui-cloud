package com.rui.common.core.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.rui.common.core.test.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 示例测试类
 * 展示如何使用测试基础设施
 *
 * @author rui
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.test.database.replace=none",
        "logging.level.com.rui=DEBUG"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class ExampleTest extends BaseTest {

    @Autowired
    private TestDataManager testDataManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        log.info("开始执行测试方法");
        // 每个测试方法执行前的准备工作
        testDataManager.cleanupAllTestData();
    }

    @AfterEach
    void tearDown() {
        log.info("测试方法执行完成");
        // 每个测试方法执行后的清理工作
        testDataManager.cleanupAllTestData();
    }

    @Test
    @Order(1)
    @DisplayName("测试基础断言功能")
    void testBasicAssertions() {
        // 字符串断言
        String testString = "Hello, World!";
        assertStringNotEmpty(testString);
        assertStringContains(testString, "World");
        assertStringMatches(testString, "Hello.*");

        // 数字断言
        int number = randomInt(1, 100);
        assertTrue(number >= 1 && number <= 100);

        // 时间断言
        LocalDateTime now = LocalDateTime.now();
        assertTimeNearNow(now, 5);

        printTestInfo("基础断言测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("测试集合断言功能")
    void testCollectionAssertions() {
        // 创建测试列表
        List<String> testList = createTestList("apple", "banana", "cherry");
        
        assertListNotEmpty(testList);
        assertListSize(testList, 3);
        assertListContains(testList, "banana");
        assertListNotContains(testList, "orange");
        
        // 集合条件断言
        assertCollectionMatches(testList, item -> item.length() > 3);
        assertCollectionContainsMatch(testList, item -> item.startsWith("a"));
        assertCollectionNotContainsMatch(testList, item -> item.startsWith("z"));

        // 创建测试Map
        Map<String, Object> testMap = createTestMap(
                "name", "测试用户",
                "age", 25,
                "email", randomEmail()
        );
        
        assertMapSize(testMap, 3);
        assertMapContainsKey(testMap, "name");
        assertMapContains(testMap, "age", 25);

        printTestObject("测试集合", testList);
        printTestObject("测试Map", testMap);
    }

    @Test
    @Order(3)
    @DisplayName("测试JSON处理功能")
    void testJsonHandling() throws Exception {
        // 创建测试对象
        TestDataManager.TestUser user = testDataManager.createRandomUser();
        
        // 转换为JSON
        String userJson = objectMapper.writeValueAsString(user);
        assertStringNotEmpty(userJson);
        assertStringContains(userJson, user.getUsername());
        
        // JSON字段断言
        assertJsonContains(userJson, "username", user.getUsername());
        assertJsonContains(userJson, "email", user.getEmail());
        
        // 复制对象测试
        TestDataManager.TestUser copiedUser = copyObject(user);
        assertNotSame(user, copiedUser);
        assertEquals(user.getUsername(), copiedUser.getUsername());
        
        printTestObject("原始用户", user);
        printTestObject("复制用户", copiedUser);
    }

    @Test
    @Order(4)
    @DisplayName("测试Redis数据管理")
    void testRedisDataManagement() {
        String testKey = "test:user:" + randomString(8);
        String testValue = "测试值_" + randomString(10);
        
        // 设置Redis数据
        testDataManager.setRedisTestData(testKey, testValue);
        
        // 验证数据存在
        assertTrue(redisTemplate.hasKey(testKey));
        assertEquals(testValue, redisTemplate.opsForValue().get(testKey));
        
        // 设置带过期时间的数据
        String expireKey = "test:expire:" + randomString(8);
        testDataManager.setRedisTestData(expireKey, "过期测试", Duration.ofSeconds(10));
        
        assertTrue(redisTemplate.hasKey(expireKey));
        Long ttl = redisTemplate.getExpire(expireKey, TimeUnit.SECONDS);
        assertTrue(ttl > 0 && ttl <= 10);
        
        // 批量设置数据
        Map<String, Object> batchData = createTestMap(
                "test:batch:1", "值1",
                "test:batch:2", "值2",
                "test:batch:3", "值3"
        );
        testDataManager.setRedisTestDataBatch(batchData);
        
        batchData.keySet().forEach(key -> {
            assertTrue(redisTemplate.hasKey(key));
        });
        
        printTestInfo("Redis数据管理测试通过");
    }

    @Test
    @Order(5)
    @DisplayName("测试异步操作")
    void testAsyncOperations() {
        // 创建异步任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000); // 模拟耗时操作
                return "异步操作完成";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
        
        // 等待异步操作完成
        waitForAsync(future, 5, TimeUnit.SECONDS);
        
        // 验证结果
        assertTrue(future.isDone());
        assertEquals("异步操作完成", future.join());
        
        printTestInfo("异步操作测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("测试条件等待")
    void testConditionWaiting() {
        // 模拟条件变化
        final boolean[] condition = {false};
        
        // 启动后台任务改变条件
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000);
                condition[0] = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // 等待条件满足
        waitForCondition(() -> condition[0], 5, TimeUnit.SECONDS, "条件未在指定时间内满足");
        
        assertTrue(condition[0]);
        printTestInfo("条件等待测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("测试执行时间断言")
    void testExecutionTimeAssertion() {
        // 测试快速操作
        assertExecutionTime(() -> {
            // 快速操作
            String result = "快速" + "操作";
            assertNotNull(result);
        }, 0, 100);
        
        // 测试较慢操作
        assertExecutionTime(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 400, 1000);
        
        printTestInfo("执行时间断言测试通过");
    }

    @Test
    @Order(8)
    @DisplayName("测试异常处理")
    void testExceptionHandling() {
        // 测试异常抛出
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            throw new RuntimeException("测试异常消息");
        });
        
        assertExceptionMessageContains(exception, "测试异常");
        
        // 测试无异常情况
        assertDoesNotThrow(() -> {
            String result = "正常操作";
            assertNotNull(result);
        });
        
        printTestInfo("异常处理测试通过");
    }

    @Test
    @Order(9)
    @DisplayName("测试反射功能")
    void testReflectionFeatures() {
        TestDataManager.TestUser user = testDataManager.createRandomUser();
        
        // 获取字段值
        String username = (String) getFieldValue(user, "username");
        assertNotNull(username);
        assertEquals(user.getUsername(), username);
        
        // 设置字段值
        String newUsername = "新用户名_" + randomString(5);
        setFieldValue(user, "username", newUsername);
        assertEquals(newUsername, user.getUsername());
        
        printTestObject("修改后的用户", user);
    }

    @Test
    @Order(10)
    @DisplayName("测试数据完整性验证")
    void testDataIntegrityValidation() {
        // 创建测试数据
        testDataManager.setRedisTestData("integrity:test:1", "值1");
        testDataManager.setRedisTestData("integrity:test:2", "值2");
        
        TestDataManager.TestUser user = testDataManager.createRandomUser();
        testDataManager.cacheTestData("test_user", user);
        
        // 验证数据完整性
        assertTrue(testDataManager.validateTestDataIntegrity());
        
        // 获取统计信息
        TestDataManager.TestDataStats stats = testDataManager.getTestDataStats();
        assertTrue(stats.getRedisKeysCount() >= 2);
        assertTrue(stats.getCachedDataCount() >= 1);
        
        printTestObject("数据统计", stats);
    }

    @Test
    @Order(11)
    @DisplayName("测试随机数据生成")
    void testRandomDataGeneration() {
        // 生成随机字符串
        String randomStr = randomString(10);
        assertEquals(10, randomStr.length());
        
        // 生成随机数字
        int randomNum = randomInt(1, 100);
        assertTrue(randomNum >= 1 && randomNum <= 100);
        
        // 生成随机邮箱
        String email = randomEmail();
        assertStringContains(email, "@test.com");
        
        // 生成随机手机号
        String phone = randomPhone();
        assertEquals(11, phone.length());
        assertTrue(phone.startsWith("1"));
        
        printTestInfo("随机数据: 字符串=" + randomStr + ", 数字=" + randomNum + 
                     ", 邮箱=" + email + ", 手机=" + phone);
    }

    @Test
    @Order(12)
    @DisplayName("综合测试场景")
    void testIntegratedScenario() {
        // 创建用户
        TestDataManager.TestUser user = testDataManager.createRandomUser();
        assertNotNull(user);
        
        // 存储到Redis
        String userKey = "user:" + user.getId();
        testDataManager.setRedisTestData(userKey, user);
        
        // 验证存储
        assertTrue(redisTemplate.hasKey(userKey));
        
        // 创建配置
        TestDataManager.TestConfig config = testDataManager.createRandomConfig();
        String configKey = "config:" + config.getId();
        testDataManager.setRedisTestData(configKey, config, Duration.ofMinutes(5));
        
        // 验证配置存储
        assertTrue(redisTemplate.hasKey(configKey));
        
        // 批量操作
        Map<String, Object> batchData = createTestMap(
                "batch:1", "批量数据1",
                "batch:2", "批量数据2"
        );
        testDataManager.setRedisTestDataBatch(batchData);
        
        // 验证批量数据
        batchData.keySet().forEach(key -> assertTrue(redisTemplate.hasKey(key)));
        
        // 验证数据完整性
        assertTrue(testDataManager.validateTestDataIntegrity());
        
        // 获取统计信息
        TestDataManager.TestDataStats stats = testDataManager.getTestDataStats();
        assertTrue(stats.getRedisKeysCount() >= 4); // user + config + 2 batch items
        
        printTestObject("最终统计", stats);
        printTestInfo("综合测试场景通过");
    }
}