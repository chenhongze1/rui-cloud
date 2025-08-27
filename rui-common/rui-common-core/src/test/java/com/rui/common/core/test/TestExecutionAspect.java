package com.rui.common.core.test;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试执行切面
 * 用于自动记录测试执行过程和性能指标
 *
 * @author rui
 */
@Aspect
@TestComponent
@Order(1)
@Slf4j
public class TestExecutionAspect {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // 测试执行统计
    private final AtomicInteger totalTests = new AtomicInteger(0);
    private final AtomicInteger successfulTests = new AtomicInteger(0);
    private final AtomicInteger failedTests = new AtomicInteger(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    
    // 测试方法执行记录
    private final Map<String, TestExecutionRecord> executionRecords = new ConcurrentHashMap<>();
    
    // 慢测试阈值（毫秒）
    private static final long SLOW_TEST_THRESHOLD = 5000;

    /**
     * 拦截测试方法执行
     */
    @Around("@annotation(org.junit.jupiter.api.Test)")
    public Object aroundTestExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String testName = getTestName(method);
        
        TestExecutionRecord record = new TestExecutionRecord();
        record.setTestName(testName);
        record.setClassName(method.getDeclaringClass().getSimpleName());
        record.setMethodName(method.getName());
        record.setStartTime(LocalDateTime.now());
        
        totalTests.incrementAndGet();
        
        log.info("[TEST-START] {} - {}", testName, record.getStartTime().format(FORMATTER));
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;
        
        try {
            result = joinPoint.proceed();
            record.setSuccess(true);
            successfulTests.incrementAndGet();
            log.info("[TEST-SUCCESS] {} - 执行成功", testName);
        } catch (Throwable e) {
            exception = e;
            record.setSuccess(false);
            record.setException(e);
            failedTests.incrementAndGet();
            log.error("[TEST-FAILED] {} - 执行失败: {}", testName, e.getMessage());
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            record.setEndTime(LocalDateTime.now());
            record.setExecutionTimeMs(executionTime);
            
            totalExecutionTime.addAndGet(executionTime);
            executionRecords.put(testName, record);
            
            logTestCompletion(testName, executionTime, exception == null);
            
            // 检查慢测试
            if (executionTime > SLOW_TEST_THRESHOLD) {
                log.warn("[SLOW-TEST] {} - 执行时间: {}ms (超过阈值 {}ms)", 
                        testName, executionTime, SLOW_TEST_THRESHOLD);
            }
        }
        
        return result;
    }

    /**
     * 记录测试完成信息
     */
    private void logTestCompletion(String testName, long executionTime, boolean success) {
        String status = success ? "SUCCESS" : "FAILED";
        log.info("[TEST-END] {} - {} - 执行时间: {}ms", testName, status, executionTime);
        
        // 记录当前统计信息
        if (log.isDebugEnabled()) {
            log.debug("[TEST-STATS] 总计: {}, 成功: {}, 失败: {}, 总耗时: {}ms", 
                    totalTests.get(), successfulTests.get(), failedTests.get(), totalExecutionTime.get());
        }
    }

    /**
     * 获取测试名称
     */
    private String getTestName(Method method) {
        Test testAnnotation = method.getAnnotation(Test.class);
        String displayName = "";
        
        // 尝试获取显示名称
        try {
            // 检查是否有DisplayName注解
            if (method.isAnnotationPresent(org.junit.jupiter.api.DisplayName.class)) {
                displayName = method.getAnnotation(org.junit.jupiter.api.DisplayName.class).value();
            }
        } catch (Exception e) {
            // 忽略异常，使用默认名称
        }
        
        if (displayName.isEmpty()) {
            displayName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }
        
        return displayName;
    }

    /**
     * 获取测试执行统计信息
     */
    public TestExecutionStats getExecutionStats() {
        TestExecutionStats stats = new TestExecutionStats();
        stats.setTotalTests(totalTests.get());
        stats.setSuccessfulTests(successfulTests.get());
        stats.setFailedTests(failedTests.get());
        stats.setTotalExecutionTimeMs(totalExecutionTime.get());
        
        if (stats.getTotalTests() > 0) {
            stats.setSuccessRate((double) stats.getSuccessfulTests() / stats.getTotalTests() * 100);
            stats.setAverageExecutionTimeMs((double) stats.getTotalExecutionTimeMs() / stats.getTotalTests());
        }
        
        return stats;
    }

    /**
     * 获取指定测试的执行记录
     */
    public TestExecutionRecord getExecutionRecord(String testName) {
        return executionRecords.get(testName);
    }

    /**
     * 获取所有测试执行记录
     */
    public Map<String, TestExecutionRecord> getAllExecutionRecords() {
        return new ConcurrentHashMap<>(executionRecords);
    }

    /**
     * 获取慢测试列表
     */
    public Map<String, TestExecutionRecord> getSlowTests() {
        Map<String, TestExecutionRecord> slowTests = new ConcurrentHashMap<>();
        
        executionRecords.forEach((testName, record) -> {
            if (record.getExecutionTimeMs() > SLOW_TEST_THRESHOLD) {
                slowTests.put(testName, record);
            }
        });
        
        return slowTests;
    }

    /**
     * 获取失败测试列表
     */
    public Map<String, TestExecutionRecord> getFailedTests() {
        Map<String, TestExecutionRecord> failedTests = new ConcurrentHashMap<>();
        
        executionRecords.forEach((testName, record) -> {
            if (!record.isSuccess()) {
                failedTests.put(testName, record);
            }
        });
        
        return failedTests;
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalTests.set(0);
        successfulTests.set(0);
        failedTests.set(0);
        totalExecutionTime.set(0);
        executionRecords.clear();
        
        log.info("[TEST-STATS] 统计信息已重置");
    }

    /**
     * 打印测试执行报告
     */
    public void printExecutionReport() {
        TestExecutionStats stats = getExecutionStats();
        
        log.info("\n" +
                "=== 测试执行报告 ===\n" +
                "总测试数: {}\n" +
                "成功测试: {}\n" +
                "失败测试: {}\n" +
                "成功率: {:.2f}%\n" +
                "总执行时间: {}ms\n" +
                "平均执行时间: {:.2f}ms\n" +
                "慢测试数量: {}\n" +
                "===================",
                stats.getTotalTests(),
                stats.getSuccessfulTests(),
                stats.getFailedTests(),
                stats.getSuccessRate(),
                stats.getTotalExecutionTimeMs(),
                stats.getAverageExecutionTimeMs(),
                getSlowTests().size());
        
        // 打印慢测试详情
        Map<String, TestExecutionRecord> slowTests = getSlowTests();
        if (!slowTests.isEmpty()) {
            log.info("\n=== 慢测试详情 ===");
            slowTests.forEach((testName, record) -> {
                log.info("{} - {}ms", testName, record.getExecutionTimeMs());
            });
        }
        
        // 打印失败测试详情
        Map<String, TestExecutionRecord> failedTests = getFailedTests();
        if (!failedTests.isEmpty()) {
            log.info("\n=== 失败测试详情 ===");
            failedTests.forEach((testName, record) -> {
                String errorMessage = record.getException() != null ? 
                        record.getException().getMessage() : "未知错误";
                log.info("{} - {}", testName, errorMessage);
            });
        }
    }

    /**
     * 测试执行记录
     */
    public static class TestExecutionRecord {
        private String testName;
        private String className;
        private String methodName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long executionTimeMs;
        private boolean success;
        private Throwable exception;

        // Getters and Setters
        public String getTestName() { return testName; }
        public void setTestName(String testName) { this.testName = testName; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public Throwable getException() { return exception; }
        public void setException(Throwable exception) { this.exception = exception; }
    }

    /**
     * 测试执行统计信息
     */
    public static class TestExecutionStats {
        private int totalTests;
        private int successfulTests;
        private int failedTests;
        private long totalExecutionTimeMs;
        private double successRate;
        private double averageExecutionTimeMs;

        // Getters and Setters
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        public int getSuccessfulTests() { return successfulTests; }
        public void setSuccessfulTests(int successfulTests) { this.successfulTests = successfulTests; }
        public int getFailedTests() { return failedTests; }
        public void setFailedTests(int failedTests) { this.failedTests = failedTests; }
        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
        public void setTotalExecutionTimeMs(long totalExecutionTimeMs) { this.totalExecutionTimeMs = totalExecutionTimeMs; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public double getAverageExecutionTimeMs() { return averageExecutionTimeMs; }
        public void setAverageExecutionTimeMs(double averageExecutionTimeMs) { this.averageExecutionTimeMs = averageExecutionTimeMs; }
    }
}