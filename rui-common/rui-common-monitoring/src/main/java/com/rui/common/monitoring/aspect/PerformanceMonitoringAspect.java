package com.rui.common.monitoring.aspect;

import com.rui.common.monitoring.annotation.PerformanceMonitored;
import com.rui.common.monitoring.metrics.MetricsCollector;
import com.rui.common.monitoring.properties.MonitoringProperties;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 性能监控切面
 * 统一处理方法级性能监控，整合原log模块的性能监控功能
 *
 * @author rui
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@ConditionalOnProperty(prefix = "rui.monitoring.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceMonitoringAspect {

    private final MetricsCollector metricsCollector;
    private final MonitoringProperties monitoringProperties;

    /**
     * 拦截@PerformanceMonitored注解的方法
     */
    @Around("@annotation(performanceMonitored) || @within(performanceMonitored)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint, PerformanceMonitored performanceMonitored) throws Throwable {
        // 如果方法上没有@PerformanceMonitored注解，则查找类上的注解
        if (performanceMonitored == null) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            performanceMonitored = AnnotationUtils.findAnnotation(method, PerformanceMonitored.class);
            if (performanceMonitored == null) {
                performanceMonitored = AnnotationUtils.findAnnotation(method.getDeclaringClass(), PerformanceMonitored.class);
            }
        }

        if (performanceMonitored == null) {
            return joinPoint.proceed();
        }

        String methodName = getMethodName(joinPoint);
        String operation = StringUtils.hasText(performanceMonitored.operation()) ? 
            performanceMonitored.operation() : methodName;
        String module = StringUtils.hasText(performanceMonitored.module()) ? 
            performanceMonitored.module() : getClassName(joinPoint);
        
        // 创建Timer用于精确计时
        Timer.Sample sample = Timer.start(metricsCollector.getMeterRegistry());
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;
        
        try {
            // 执行方法
            result = joinPoint.proceed();
            return result;
            
        } catch (Throwable throwable) {
            exception = throwable;
            throw throwable;
            
        } finally {
            // 计算执行时间
            long duration = System.currentTimeMillis() - startTime;
            
            // 停止Timer并记录指标
            sample.stop(Timer.builder("method.execution.time")
                .description("Method execution time")
                .tag("class", getClassName(joinPoint))
                .tag("method", methodName)
                .tag("operation", operation)
                .tag("module", module)
                .tag("status", exception != null ? "error" : "success")
                .register(metricsCollector.getMeterRegistry()));
            
            // 记录性能监控数据
            recordPerformanceMetrics(joinPoint, performanceMonitored, operation, module, duration, result, exception);
        }
    }

    /**
     * 记录性能监控指标
     */
    private void recordPerformanceMetrics(ProceedingJoinPoint joinPoint, PerformanceMonitored performanceMonitored,
                                        String operation, String module, long duration, Object result, Throwable exception) {
        
        // 判断是否为慢操作
        Duration slowThreshold = performanceMonitored.slowThreshold() > 0 ? 
            Duration.ofMillis(performanceMonitored.slowThreshold()) : 
            monitoringProperties.getPerformance().getSlowOperationThreshold();
        
        boolean isSlow = duration > slowThreshold.toMillis();
        
        // 构建性能指标数据
        Map<String, Object> performanceData = new HashMap<>();
        performanceData.put("operation", operation);
        performanceData.put("module", module);
        performanceData.put("duration", duration);
        performanceData.put("slow", isSlow);
        performanceData.put("success", exception == null);
        performanceData.put("timestamp", System.currentTimeMillis());
        
        if (performanceMonitored.includeParameters()) {
            performanceData.put("parameterCount", joinPoint.getArgs().length);
        }
        
        if (exception != null) {
            performanceData.put("exceptionType", exception.getClass().getSimpleName());
        }
        
        // 记录到指标收集器
        metricsCollector.recordPerformanceMetric(operation, duration, performanceData);
        
        // 如果是慢操作，记录慢操作指标
        if (isSlow) {
            metricsCollector.recordSlowOperation(operation, module, duration, performanceData);
            
            // 记录慢操作日志
            log.warn("检测到慢操作: operation={}, module={}, duration={}ms, threshold={}ms", 
                operation, module, duration, slowThreshold.toMillis());
        }
        
        // 记录方法调用计数器
        metricsCollector.getMeterRegistry()
            .counter("method.invocation.count",
                "class", getClassName(joinPoint),
                "method", getMethodName(joinPoint),
                "operation", operation,
                "module", module,
                "status", exception != null ? "error" : "success")
            .increment();
        
        // 如果启用了详细监控，记录更多指标
        if (performanceMonitored.detailedMonitoring()) {
            recordDetailedMetrics(joinPoint, performanceData);
        }
    }
    
    /**
     * 记录详细的性能指标
     */
    private void recordDetailedMetrics(ProceedingJoinPoint joinPoint, Map<String, Object> performanceData) {
        try {
            // 记录内存使用情况
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            performanceData.put("memoryUsed", usedMemory);
            performanceData.put("memoryTotal", totalMemory);
            
            // 记录线程信息
            performanceData.put("activeThreads", Thread.activeCount());
            
            // 记录内存使用指标
            metricsCollector.recordMemoryUsage(usedMemory);
                
        } catch (Exception e) {
            log.debug("记录详细性能指标失败", e);
        }
    }

    /**
     * 获取方法名
     */
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod().getName();
    }

    /**
     * 获取类名
     */
    private String getClassName(ProceedingJoinPoint joinPoint) {
        return joinPoint.getTarget().getClass().getSimpleName();
    }
}