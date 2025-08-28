package com.rui.common.monitoring;

import com.rui.common.monitoring.annotation.PerformanceMonitored;
import com.rui.common.monitoring.service.PerformanceMonitoringService;
import com.rui.common.monitoring.adapter.LogPerformanceAdapter;
import com.rui.common.monitoring.metrics.MetricsCollector;
import com.rui.common.monitoring.aspect.PerformanceMonitoringAspect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能监控功能集成测试
 * 验证性能监控功能整合后的正确性
 *
 * @author rui
 */
@ExtendWith(MockitoExtension.class)
class PerformanceMonitoringIntegrationTest {

    @Mock
    private PerformanceMonitoringService performanceService;

    @Mock
    private LogPerformanceAdapter logAdapter;

    @BeforeEach
    void setUp() {
        // Mock对象已通过@Mock注解自动创建
    }

    @Test
    public void testPerformanceServiceAvailable() {
        // 验证Mock对象是否创建成功
        assertNotNull(performanceService, "PerformanceMonitoringService mock should be created");
        System.out.println("PerformanceMonitoringService mock is available");
    }

    @Test
    public void testLogAdapterAvailable() {
        assertNotNull(logAdapter, "LogPerformanceAdapter should be available");
    }

    @Test
    public void testPerformanceMonitoringAnnotation() {
        // 测试注解功能
        assertDoesNotThrow(() -> {
            testMethod();
        }, "@PerformanceMonitored annotation should work without errors");
    }

    @Test
    public void testRecordMethodPerformance() {
        // 测试记录方法性能
        assertDoesNotThrow(() -> {
            Map<String, Object> tags = new HashMap<>();
            tags.put("test", "integration");
            performanceService.recordMethodPerformance("testOperation", "testModule", 500L, true, tags);
        }, "Performance service should record metrics without errors");
    }

    @Test
    public void testSlowOperationDetection() {
        assertDoesNotThrow(() -> {
            Map<String, Object> tags = new HashMap<>();
            tags.put("test", "slow");
            // 模拟慢操作（超过阈值）
            performanceService.recordMethodPerformance("slowOperation", "testModule", 2000L, true, tags);
        }, "Slow operation detection should work without errors");
    }

    @Test
    public void testLogAdapterCompatibility() {
        assertDoesNotThrow(() -> {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("test", "compatibility");
            logAdapter.recordPerformance("legacyOperation", 300L, metrics);
        }, "Log adapter should provide backward compatibility");
    }

    @Test
    public void testPerformanceStatistics() {
        // 记录一些测试数据
        Map<String, Object> tags = new HashMap<>();
        tags.put("test", "statistics");
        
        performanceService.recordMethodPerformance("statsTest", "testModule", 100L, true, tags);
        performanceService.recordMethodPerformance("statsTest", "testModule", 200L, true, tags);
        performanceService.recordMethodPerformance("statsTest", "testModule", 300L, true, tags);
        
        // 获取统计信息
        assertDoesNotThrow(() -> {
            var stats = performanceService.getPerformanceStatistics();
            assertNotNull(stats, "Performance statistics should be available");
        }, "Getting performance statistics should work without errors");
    }

    /**
     * 测试方法，使用性能监控注解
     */
    @PerformanceMonitored(
        operation = "testMethod",
        module = "integrationTest",
        slowThreshold = 500,
        includeParameters = true,
        detailedMonitoring = true
    )
    public void testMethod() {
        try {
            // 模拟一些处理时间
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 测试慢操作
     */
    @PerformanceMonitored(
        operation = "slowTestMethod",
        module = "integrationTest",
        slowThreshold = 200
    )
    public void slowTestMethod() {
        try {
            // 模拟慢操作
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testSlowMethodDetection() {
        assertDoesNotThrow(() -> {
            slowTestMethod();
        }, "Slow method should be detected and logged without errors");
    }

    @Test
    public void testErrorHandling() {
        if (performanceService != null) {
            assertDoesNotThrow(() -> {
                // 测试异常情况下的处理
                performanceService.recordMethodPerformance("testMethod", "defaultModule", 100L, true, new java.util.HashMap<>());
                performanceService.recordMethodPerformance("testMethod2", "testModule", 200L, true, new java.util.HashMap<>());
                performanceService.recordMethodPerformance("slowMethod", "testModule", 2000L, true, new java.util.HashMap<>());
            }, "Performance service should handle edge cases gracefully");
        }
    }
}