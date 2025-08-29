package com.rui.demo.web.controller;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 监控测试控制器
 * 用于测试监控模块的各种功能
 *
 * @author rui
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringTestController {

    private final MeterRegistry meterRegistry;
    private final Random random = new Random();

    /**
     * 基础监控测试
     */
    @GetMapping("/test")
    @Timed(value = "monitoring.test.basic", description = "基础监控测试")
    public Map<String, Object> basicTest() {
        log.info("执行基础监控测试");
        
        // 增加自定义计数器
        Counter.builder("monitoring.test.counter")
                .description("监控测试计数器")
                .tag("type", "basic")
                .register(meterRegistry)
                .increment();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "基础监控测试成功");
        result.put("timestamp", LocalDateTime.now());
        result.put("status", "success");
        
        return result;
    }

    /**
     * 性能监控测试
     */
    @GetMapping("/performance")
    @Timed(value = "monitoring.test.performance", description = "性能监控测试")
    public Map<String, Object> performanceTest() throws InterruptedException {
        log.info("执行性能监控测试");
        
        // 模拟不同的执行时间
        int delay = random.nextInt(1000) + 100; // 100-1100ms
        Thread.sleep(delay);
        
        // 记录自定义计时器
        Timer.Sample sample = Timer.start(meterRegistry);
        // 模拟业务处理
        Thread.sleep(50);
        sample.stop(Timer.builder("monitoring.business.process")
                .description("业务处理时间")
                .tag("operation", "performance_test")
                .register(meterRegistry));
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "性能监控测试完成");
        result.put("executionTime", delay + "ms");
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }

    /**
     * 错误监控测试
     */
    @GetMapping("/error")
    public Map<String, Object> errorTest(@RequestParam(defaultValue = "false") boolean throwError) {
        log.info("执行错误监控测试，throwError: {}", throwError);
        
        // 增加错误计数器
        Counter errorCounter = Counter.builder("monitoring.test.errors")
                .description("监控测试错误计数")
                .tag("type", "simulated")
                .register(meterRegistry);
        
        if (throwError) {
            errorCounter.increment();
            throw new RuntimeException("模拟错误 - 用于测试错误监控功能");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "错误监控测试完成（无错误）");
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }

    /**
     * 自定义指标测试
     */
    @PostMapping("/metrics")
    @Timed(value = "monitoring.test.custom_metrics", description = "自定义指标测试")
    public Map<String, Object> customMetricsTest(@RequestBody Map<String, Object> request) {
        log.info("执行自定义指标测试，请求参数: {}", request);
        
        String metricName = (String) request.getOrDefault("metricName", "custom.metric");
        String metricType = (String) request.getOrDefault("metricType", "counter");
        Double value = Double.valueOf(request.getOrDefault("value", 1.0).toString());
        
        switch (metricType.toLowerCase()) {
            case "counter":
                Counter.builder(metricName)
                        .description("自定义计数器")
                        .tag("source", "test")
                        .register(meterRegistry)
                        .increment(value);
                break;
            case "gauge":
                meterRegistry.gauge(metricName, value);
                break;
            case "timer":
                Timer.builder(metricName)
                        .description("自定义计时器")
                        .tag("source", "test")
                        .register(meterRegistry)
                        .record(value.longValue(), TimeUnit.MILLISECONDS);
                break;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "自定义指标创建成功");
        result.put("metricName", metricName);
        result.put("metricType", metricType);
        result.put("value", value);
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }

    /**
     * 健康检查测试
     */
    @GetMapping("/health")
    public Map<String, Object> healthTest() {
        log.info("执行健康检查测试");
        
        // 模拟健康状态检查
        boolean isHealthy = random.nextBoolean();
        
        Counter healthCounter = Counter.builder("monitoring.health.checks")
                .description("健康检查计数")
                .tag("status", isHealthy ? "healthy" : "unhealthy")
                .register(meterRegistry);
        healthCounter.increment();
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", isHealthy ? "UP" : "DOWN");
        result.put("message", isHealthy ? "系统健康" : "系统异常");
        result.put("timestamp", LocalDateTime.now());
        result.put("details", Map.of(
                "database", "UP",
                "redis", "UP",
                "disk", isHealthy ? "UP" : "DOWN"
        ));
        
        return result;
    }
}