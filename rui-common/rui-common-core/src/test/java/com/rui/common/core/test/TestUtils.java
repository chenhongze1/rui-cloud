package com.rui.common.core.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试工具类
 * 提供常用的测试辅助方法和断言功能
 *
 * @author rui
 */
@Slf4j
public class TestUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TestUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 等待异步操作完成
     */
    public static void waitForAsync(CompletableFuture<?> future, long timeout, TimeUnit unit) {
        try {
            future.get(timeout, unit);
        } catch (Exception e) {
            fail("异步操作超时或失败: " + e.getMessage());
        }
    }

    /**
     * 等待条件满足
     */
    public static void waitForCondition(Supplier<Boolean> condition, long timeout, TimeUnit unit) {
        waitForCondition(condition, timeout, unit, "条件等待超时");
    }

    /**
     * 等待条件满足（带自定义错误消息）
     */
    public static void waitForCondition(Supplier<Boolean> condition, long timeout, TimeUnit unit, String message) {
        long timeoutMillis = unit.toMillis(timeout);
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (condition.get()) {
                return;
            }
            
            try {
                Thread.sleep(100); // 100ms间隔检查
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("等待条件时被中断: " + e.getMessage());
            }
        }
        
        fail(message);
    }

    /**
     * 断言JSON字符串相等
     */
    public static void assertJsonEquals(String expected, String actual) {
        try {
            Object expectedObj = OBJECT_MAPPER.readValue(expected, Object.class);
            Object actualObj = OBJECT_MAPPER.readValue(actual, Object.class);
            assertEquals(expectedObj, actualObj, "JSON内容不匹配");
        } catch (Exception e) {
            fail("JSON解析失败: " + e.getMessage());
        }
    }

    /**
     * 断言JSON字符串包含指定字段
     */
    public static void assertJsonContains(String json, String fieldPath, Object expectedValue) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonMap = OBJECT_MAPPER.readValue(json, Map.class);
            Object actualValue = getNestedValue(jsonMap, fieldPath);
            assertEquals(expectedValue, actualValue, "JSON字段值不匹配: " + fieldPath);
        } catch (Exception e) {
            fail("JSON解析或字段访问失败: " + e.getMessage());
        }
    }

    /**
     * 获取嵌套字段值
     */
    @SuppressWarnings("unchecked")
    private static Object getNestedValue(Map<String, Object> map, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Object current = map;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }

    /**
     * 断言列表包含指定元素
     */
    public static <T> void assertListContains(List<T> list, T element) {
        assertTrue(list.contains(element), "列表不包含指定元素: " + element);
    }

    /**
     * 断言列表不包含指定元素
     */
    public static <T> void assertListNotContains(List<T> list, T element) {
        assertFalse(list.contains(element), "列表包含不应该存在的元素: " + element);
    }

    /**
     * 断言列表大小
     */
    public static <T> void assertListSize(List<T> list, int expectedSize) {
        assertEquals(expectedSize, list.size(), "列表大小不匹配");
    }

    /**
     * 断言列表非空
     */
    public static <T> void assertListNotEmpty(List<T> list) {
        assertNotNull(list, "列表为null");
        assertFalse(list.isEmpty(), "列表为空");
    }

    /**
     * 断言列表为空
     */
    public static <T> void assertListEmpty(List<T> list) {
        if (list != null) {
            assertTrue(list.isEmpty(), "列表不为空");
        }
    }

    /**
     * 断言Map包含指定键
     */
    public static <K, V> void assertMapContainsKey(Map<K, V> map, K key) {
        assertTrue(map.containsKey(key), "Map不包含指定键: " + key);
    }

    /**
     * 断言Map包含指定键值对
     */
    public static <K, V> void assertMapContains(Map<K, V> map, K key, V value) {
        assertTrue(map.containsKey(key), "Map不包含指定键: " + key);
        assertEquals(value, map.get(key), "Map键值不匹配: " + key);
    }

    /**
     * 断言Map大小
     */
    public static <K, V> void assertMapSize(Map<K, V> map, int expectedSize) {
        assertEquals(expectedSize, map.size(), "Map大小不匹配");
    }

    /**
     * 断言时间在指定范围内
     */
    public static void assertTimeInRange(LocalDateTime actual, LocalDateTime start, LocalDateTime end) {
        assertNotNull(actual, "时间为null");
        assertTrue(actual.isAfter(start) || actual.isEqual(start), 
                  "时间早于开始时间: " + actual.format(DATE_TIME_FORMATTER) + " < " + start.format(DATE_TIME_FORMATTER));
        assertTrue(actual.isBefore(end) || actual.isEqual(end), 
                  "时间晚于结束时间: " + actual.format(DATE_TIME_FORMATTER) + " > " + end.format(DATE_TIME_FORMATTER));
    }

    /**
     * 断言时间接近当前时间
     */
    public static void assertTimeNearNow(LocalDateTime actual, long toleranceSeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusSeconds(toleranceSeconds);
        LocalDateTime end = now.plusSeconds(toleranceSeconds);
        assertTimeInRange(actual, start, end);
    }

    /**
     * 断言异常消息包含指定文本
     */
    public static void assertExceptionMessageContains(Exception exception, String expectedText) {
        assertNotNull(exception, "异常为null");
        String message = exception.getMessage();
        assertNotNull(message, "异常消息为null");
        assertTrue(message.contains(expectedText), 
                  "异常消息不包含期望文本。期望: " + expectedText + ", 实际: " + message);
    }

    /**
     * 断言执行时间在指定范围内
     */
    public static void assertExecutionTime(Runnable task, long minMillis, long maxMillis) {
        long startTime = System.currentTimeMillis();
        task.run();
        long executionTime = System.currentTimeMillis() - startTime;
        
        assertTrue(executionTime >= minMillis, 
                  "执行时间过短: " + executionTime + "ms < " + minMillis + "ms");
        assertTrue(executionTime <= maxMillis, 
                  "执行时间过长: " + executionTime + "ms > " + maxMillis + "ms");
    }

    /**
     * 断言字符串匹配正则表达式
     */
    public static void assertStringMatches(String actual, String regex) {
        assertNotNull(actual, "字符串为null");
        assertTrue(actual.matches(regex), 
                  "字符串不匹配正则表达式。字符串: " + actual + ", 正则: " + regex);
    }

    /**
     * 断言字符串包含指定子串
     */
    public static void assertStringContains(String actual, String substring) {
        assertNotNull(actual, "字符串为null");
        assertTrue(actual.contains(substring), 
                  "字符串不包含指定子串。字符串: " + actual + ", 子串: " + substring);
    }

    /**
     * 断言字符串不为空
     */
    public static void assertStringNotEmpty(String actual) {
        assertNotNull(actual, "字符串为null");
        assertFalse(actual.trim().isEmpty(), "字符串为空");
    }

    /**
     * 从MvcResult提取响应内容
     */
    public static String extractResponseContent(MvcResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (UnsupportedEncodingException e) {
            fail("提取响应内容失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从MvcResult提取JSON响应
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractJsonResponse(MvcResult result) {
        String content = extractResponseContent(result);
        try {
            return OBJECT_MAPPER.readValue(content, Map.class);
        } catch (Exception e) {
            fail("解析JSON响应失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从JSON响应中提取指定字段
     */
    public static Object extractJsonField(MvcResult result, String fieldPath) {
        Map<String, Object> json = extractJsonResponse(result);
        return getNestedValue(json, fieldPath);
    }

    /**
     * 创建测试对象的副本
     */
    @SuppressWarnings("unchecked")
    public static <T> T copyObject(T original) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(original);
            return (T) OBJECT_MAPPER.readValue(json, original.getClass());
        } catch (Exception e) {
            fail("复制对象失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 设置对象字段值（通过反射）
     */
    public static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            fail("设置字段值失败: " + fieldName + ", " + e.getMessage());
        }
    }

    /**
     * 获取对象字段值（通过反射）
     */
    public static Object getFieldValue(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            fail("获取字段值失败: " + fieldName + ", " + e.getMessage());
            return null;
        }
    }

    /**
     * 查找字段（包括父类）
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("字段不存在: " + fieldName);
    }

    /**
     * 生成随机字符串
     */
    public static String randomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }

    /**
     * 生成随机数字
     */
    public static int randomInt(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * 生成随机邮箱
     */
    public static String randomEmail() {
        return randomString(8) + "@test.com";
    }

    /**
     * 生成随机手机号
     */
    public static String randomPhone() {
        return "1" + randomInt(30, 89) + String.format("%08d", randomInt(0, 99999999));
    }

    /**
     * 断言集合满足条件
     */
    public static <T> void assertCollectionMatches(Collection<T> collection, Predicate<T> predicate) {
        assertNotNull(collection, "集合为null");
        assertTrue(collection.stream().allMatch(predicate), "集合中存在不满足条件的元素");
    }

    /**
     * 断言集合存在满足条件的元素
     */
    public static <T> void assertCollectionContainsMatch(Collection<T> collection, Predicate<T> predicate) {
        assertNotNull(collection, "集合为null");
        assertTrue(collection.stream().anyMatch(predicate), "集合中不存在满足条件的元素");
    }

    /**
     * 断言集合不存在满足条件的元素
     */
    public static <T> void assertCollectionNotContainsMatch(Collection<T> collection, Predicate<T> predicate) {
        assertNotNull(collection, "集合为null");
        assertFalse(collection.stream().anyMatch(predicate), "集合中存在不应该满足条件的元素");
    }

    /**
     * 打印测试信息
     */
    public static void printTestInfo(String message) {
        log.info("[TEST] {}", message);
    }

    /**
     * 打印测试对象
     */
    public static void printTestObject(String name, Object object) {
        try {
            String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            log.info("[TEST] {}: \n{}", name, json);
        } catch (Exception e) {
            log.info("[TEST] {}: {}", name, object);
        }
    }

    /**
     * 创建测试用的Map
     */
    public static Map<String, Object> createTestMap(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("参数个数必须为偶数");
        }
        
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        
        return map;
    }

    /**
     * 创建测试用的List
     */
    @SafeVarargs
    public static <T> List<T> createTestList(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }
}