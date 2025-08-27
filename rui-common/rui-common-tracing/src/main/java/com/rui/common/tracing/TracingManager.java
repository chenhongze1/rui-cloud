package com.rui.common.tracing;

import com.rui.common.tracing.config.TracingConfig;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 链路追踪管理器
 * 处理追踪的创建、管理和上下文传播
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TracingManager {

    private final TracingConfig tracingConfig;
    private final OpenTelemetry openTelemetry;
    
    private Tracer tracer;
    
    // 活跃的Span缓存
    private final Map<String, Span> activeSpans = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        if (openTelemetry != null) {
            this.tracer = openTelemetry.getTracer("rui-cloud", "1.0.0");
            log.info("TracingManager initialized with OpenTelemetry");
        }
    }
    
    /**
     * 创建一个新的Span
     */
    public Span createSpan(String operationName) {
        return createSpan(operationName, SpanKind.INTERNAL);
    }
    
    /**
     * 创建一个新的Span
     */
    public Span createSpan(String operationName, SpanKind spanKind) {
        if (tracer == null) {
            return Span.getInvalid();
        }
        
        try {
            SpanBuilder spanBuilder = tracer.spanBuilder(operationName)
                .setSpanKind(spanKind);
            
            // 添加默认标签
            addDefaultTags(spanBuilder);
            
            Span span = spanBuilder.startSpan();
            
            // 缓存活跃的Span
            String spanId = span.getSpanContext().getSpanId();
            activeSpans.put(spanId, span);
            
            log.debug("Created span: {} with kind: {}", operationName, spanKind);
            return span;
            
        } catch (Exception e) {
            log.error("Failed to create span: {}", operationName, e);
            return Span.getInvalid();
        }
    }
    
    /**
     * 创建子Span
     */
    public Span createChildSpan(String operationName, Span parentSpan) {
        if (tracer == null || parentSpan == null) {
            return createSpan(operationName);
        }
        
        try {
            SpanBuilder spanBuilder = tracer.spanBuilder(operationName)
                .setParent(Context.current().with(parentSpan))
                .setSpanKind(SpanKind.INTERNAL);
            
            // 添加默认标签
            addDefaultTags(spanBuilder);
            
            Span span = spanBuilder.startSpan();
            
            // 缓存活跃的Span
            String spanId = span.getSpanContext().getSpanId();
            activeSpans.put(spanId, span);
            
            log.debug("Created child span: {} under parent: {}", 
                operationName, parentSpan.getSpanContext().getSpanId());
            return span;
            
        } catch (Exception e) {
            log.error("Failed to create child span: {}", operationName, e);
            return createSpan(operationName);
        }
    }
    
    /**
     * 在Span作用域内执行操作并返回结果
     */
    public <T> T executeInSpan(String operationName, Supplier<T> operation) {
        Span span = createSpan(operationName);
        try (Scope scope = span.makeCurrent()) {
            T result = operation.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            recordException(span, e);
            throw e;
        } finally {
            finishSpan(span);
        }
    }
    
    /**
     * 在Span作用域内执行操作
     */
    public void executeInSpan(String operationName, Runnable operation) {
        Span span = createSpan(operationName);
        try (Scope scope = span.makeCurrent()) {
            operation.run();
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            recordException(span, e);
            throw e;
        } finally {
            finishSpan(span);
        }
    }
    
    /**
     * 为Span添加字符串标签
     */
    public void addSpanTag(Span span, String key, String value) {
        if (span != null && key != null && value != null) {
            span.setAttribute(key, value);
        }
    }
    
    /**
     * 为Span添加数字标签
     */
    public void addSpanTag(Span span, String key, long value) {
        if (span != null && key != null) {
            span.setAttribute(key, value);
        }
    }
    
    /**
     * 为Span添加布尔标签
     */
    public void addSpanTag(Span span, String key, boolean value) {
        if (span != null && key != null) {
            span.setAttribute(key, value);
        }
    }
    
    /**
     * 为Span添加事件
     */
    public void addSpanEvent(Span span, String eventName) {
        if (span != null && eventName != null) {
            span.addEvent(eventName);
        }
    }
    
    /**
     * 为Span添加事件和属性
     */
    public void addSpanEvent(Span span, String eventName, Map<String, String> attributes) {
        if (span != null && eventName != null) {
            if (attributes != null && !attributes.isEmpty()) {
                io.opentelemetry.api.common.AttributesBuilder builder = io.opentelemetry.api.common.Attributes.builder();
                attributes.forEach(builder::put);
                span.addEvent(eventName, builder.build());
            } else {
                span.addEvent(eventName);
            }
        }
    }
    
    /**
     * 记录异常到Span
     */
    public void recordException(Span span, Throwable exception) {
        if (span != null && exception != null) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
            log.debug("Recorded exception in span: {}", exception.getMessage());
        }
    }
    
    /**
     * 设置Span状态
     */
    public void setSpanStatus(Span span, StatusCode statusCode, String description) {
        if (span != null) {
            if (description != null) {
                span.setStatus(statusCode, description);
            } else {
                span.setStatus(statusCode);
            }
        }
    }
    
    /**
     * 完成Span
     */
    public void finishSpan(Span span) {
        if (span != null && span.getSpanContext().isValid()) {
            try {
                // 从缓存中移除
                String spanId = span.getSpanContext().getSpanId();
                activeSpans.remove(spanId);
                
                // 结束Span
                span.end();
                log.debug("Finished span: {}", spanId);
                
            } catch (Exception e) {
                log.error("Failed to finish span", e);
            }
        }
    }
    
    /**
     * 获取当前活跃的Span
     */
    public Span getCurrentSpan() {
        try {
            return Span.current();
        } catch (Exception e) {
            log.error("Failed to get current span", e);
            return Span.getInvalid();
        }
    }
    
    /**
     * 获取当前的追踪ID
     */
    public String getCurrentTraceId() {
        try {
            Span currentSpan = getCurrentSpan();
            if (currentSpan.getSpanContext().isValid()) {
                return currentSpan.getSpanContext().getTraceId();
            }
        } catch (Exception e) {
            log.error("Failed to get current trace ID", e);
        }
        return "";
    }
    
    /**
     * 获取当前的SpanID
     */
    public String getCurrentSpanId() {
        try {
            Span currentSpan = getCurrentSpan();
            if (currentSpan.getSpanContext().isValid()) {
                return currentSpan.getSpanContext().getSpanId();
            }
        } catch (Exception e) {
            log.error("Failed to get current span ID", e);
        }
        return "";
    }
    
    /**
     * 检查是否在追踪上下文中
     */
    public boolean isInTracingContext() {
        try {
            return getCurrentSpan().getSpanContext().isValid();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取活跃Span数量
     */
    public int getActiveSpanCount() {
        return activeSpans.size();
    }
    
    /**
     * 清理活跃的Span缓存
     */
    public void cleanupActiveSpans() {
        try {
            activeSpans.clear();
            log.debug("Cleaned up active spans cache");
        } catch (Exception e) {
            log.error("Failed to cleanup active spans", e);
        }
    }
    
    /**
     * 添加默认标签
     */
    private void addDefaultTags(SpanBuilder spanBuilder) {
        if (tracingConfig != null) {
            spanBuilder.setAttribute("service.name", tracingConfig.getServiceName());
            spanBuilder.setAttribute("service.version", tracingConfig.getServiceVersion());
        }
        spanBuilder.setAttribute("timestamp", Instant.now().toString());
    }
}