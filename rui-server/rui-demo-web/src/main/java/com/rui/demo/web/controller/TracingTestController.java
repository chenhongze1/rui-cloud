package com.rui.demo.web.controller;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 链路追踪测试控制器
 *
 * @author rui
 */
@Slf4j
@RestController
@RequestMapping("/api/tracing")
public class TracingTestController {

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    public TracingTestController(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("rui-demo-web", "1.0.0");
    }

    /**
     * 基础链路追踪测试
     */
    @GetMapping("/test")
    public Map<String, Object> basicTracingTest() {
        log.info("执行基础链路追踪测试");
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "链路追踪基础功能测试成功");
        result.put("timestamp", System.currentTimeMillis());
        result.put("traceId", getCurrentTraceId());
        result.put("spanId", getCurrentSpanId());
        
        return result;
    }

    /**
     * 自定义Span测试
     */
    @GetMapping("/custom-span")
    public Map<String, Object> customSpanTest() {
        Span span = tracer.spanBuilder("custom-operation")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("operation.type", "custom")
                .setAttribute("operation.name", "custom-span-test")
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            log.info("执行自定义Span测试");
            
            // 模拟业务操作
            simulateBusinessOperation();
            
            span.addEvent("业务操作完成");
            span.setAttribute("operation.result", "success");
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "自定义Span测试成功");
            result.put("spanName", "custom-operation");
            result.put("traceId", getCurrentTraceId());
            result.put("spanId", getCurrentSpanId());
            
            return result;
        } catch (Exception e) {
            span.recordException(e);
            span.setAttribute("operation.result", "error");
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * 嵌套Span测试
     */
    @GetMapping("/nested-span")
    public Map<String, Object> nestedSpanTest() {
        Span parentSpan = tracer.spanBuilder("parent-operation")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("operation.level", "parent")
                .startSpan();
        
        try (Scope parentScope = parentSpan.makeCurrent()) {
            log.info("执行嵌套Span测试 - 父级操作");
            
            // 子Span 1
            Span childSpan1 = tracer.spanBuilder("child-operation-1")
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("operation.level", "child")
                    .setAttribute("child.index", 1)
                    .startSpan();
            
            try (Scope childScope1 = childSpan1.makeCurrent()) {
                log.info("执行子操作1");
                Thread.sleep(100); // 模拟耗时操作
                childSpan1.addEvent("子操作1完成");
            } catch (InterruptedException e) {
                childSpan1.recordException(e);
                Thread.currentThread().interrupt();
            } finally {
                childSpan1.end();
            }
            
            // 子Span 2
            Span childSpan2 = tracer.spanBuilder("child-operation-2")
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("operation.level", "child")
                    .setAttribute("child.index", 2)
                    .startSpan();
            
            try (Scope childScope2 = childSpan2.makeCurrent()) {
                log.info("执行子操作2");
                Thread.sleep(150); // 模拟耗时操作
                childSpan2.addEvent("子操作2完成");
            } catch (InterruptedException e) {
                childSpan2.recordException(e);
                Thread.currentThread().interrupt();
            } finally {
                childSpan2.end();
            }
            
            parentSpan.addEvent("所有子操作完成");
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "嵌套Span测试成功");
            result.put("parentSpan", "parent-operation");
            result.put("childSpans", new String[]{"child-operation-1", "child-operation-2"});
            result.put("traceId", getCurrentTraceId());
            
            return result;
        } finally {
            parentSpan.end();
        }
    }

    /**
     * 异步操作追踪测试
     */
    @GetMapping("/async-span")
    public CompletableFuture<Map<String, Object>> asyncSpanTest() {
        Span span = tracer.spanBuilder("async-operation")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("operation.type", "async")
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            log.info("开始异步Span测试");
            
            return CompletableFuture.supplyAsync(() -> {
                try (Scope asyncScope = span.makeCurrent()) {
                    log.info("执行异步操作");
                    
                    // 模拟异步业务操作
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        span.recordException(e);
                        Thread.currentThread().interrupt();
                    }
                    
                    span.addEvent("异步操作完成");
                    span.setAttribute("async.result", "success");
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("message", "异步Span测试成功");
                    result.put("executionTime", "200ms");
                    result.put("traceId", getCurrentTraceId());
                    result.put("spanId", getCurrentSpanId());
                    
                    return result;
                } finally {
                    span.end();
                }
            });
        }
    }

    /**
     * 错误追踪测试
     */
    @GetMapping("/error-span")
    public Map<String, Object> errorSpanTest(@RequestParam(defaultValue = "false") boolean throwError) {
        Span span = tracer.spanBuilder("error-operation")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("operation.type", "error-test")
                .setAttribute("will.throw.error", throwError)
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            log.info("执行错误追踪测试, throwError: {}", throwError);
            
            if (throwError) {
                RuntimeException exception = new RuntimeException("模拟业务异常");
                span.recordException(exception);
                span.setAttribute("error.occurred", true);
                span.setAttribute("error.message", exception.getMessage());
                throw exception;
            }
            
            span.addEvent("操作正常完成");
            span.setAttribute("error.occurred", false);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "错误追踪测试成功 - 无异常");
            result.put("errorOccurred", false);
            result.put("traceId", getCurrentTraceId());
            
            return result;
        } finally {
            span.end();
        }
    }

    /**
     * 链路追踪信息查询
     */
    @GetMapping("/info")
    public Map<String, Object> getTracingInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("tracerName", "rui-demo-web");
        info.put("tracerVersion", "1.0.0");
        info.put("currentTraceId", getCurrentTraceId());
        info.put("currentSpanId", getCurrentSpanId());
        info.put("openTelemetryVersion", "1.32.0");
        info.put("message", "链路追踪信息获取成功");
        
        return info;
    }

    /**
     * 模拟业务操作
     */
    private void simulateBusinessOperation() {
        try {
            // 模拟随机耗时
            int delay = ThreadLocalRandom.current().nextInt(50, 200);
            Thread.sleep(delay);
            
            // 添加当前Span的属性
            Span currentSpan = Span.current();
            currentSpan.setAttribute("business.operation.delay", delay);
            currentSpan.addEvent("业务操作执行中");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Span.current().recordException(e);
        }
    }

    /**
     * 获取当前TraceId
     */
    private String getCurrentTraceId() {
        Span currentSpan = Span.current();
        return currentSpan.getSpanContext().getTraceId();
    }

    /**
     * 获取当前SpanId
     */
    private String getCurrentSpanId() {
        Span currentSpan = Span.current();
        return currentSpan.getSpanContext().getSpanId();
    }
}