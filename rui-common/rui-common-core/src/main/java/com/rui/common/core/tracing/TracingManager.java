package com.rui.common.core.tracing;

import com.rui.common.core.config.TracingConfig;
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
        if (tracingConfig.isEnabled()) {
            this.tracer = openTelemetry.getTracer(tracingConfig.getServiceName());
            log.info("链路追踪管理器初始化完成: serviceName={}", tracingConfig.getServiceName());
        }
    }

    /**
     * 创建新的Span
     */
    public Span createSpan(String operationName) {
        return createSpan(operationName, SpanKind.INTERNAL);
    }

    /**
     * 创建指定类型的Span
     */
    public Span createSpan(String operationName, SpanKind spanKind) {
        if (!tracingConfig.isEnabled() || tracer == null) {
            return Span.getInvalid();
        }
        
        try {
            SpanBuilder spanBuilder = tracer.spanBuilder(operationName)
                .setSpanKind(spanKind)
                .setStartTimestamp(Instant.now());
            
            // 添加默认标签
            addDefaultTags(spanBuilder);
            
            Span span = spanBuilder.startSpan();
            
            // 缓存活跃的Span
            String spanId = span.getSpanContext().getSpanId();
            activeSpans.put(spanId, span);
            
            log.debug("创建Span: operation={}, spanId={}", operationName, spanId);
            return span;
            
        } catch (Exception e) {
            log.error("创建Span失败: operation={}", operationName, e);
            return Span.getInvalid();
        }
    }

    /**
     * 创建子Span
     */
    public Span createChildSpan(String operationName, Span parentSpan) {
        if (!tracingConfig.isEnabled() || tracer == null || parentSpan == null) {
            return Span.getInvalid();
        }
        
        try {
            Context parentContext = Context.current().with(parentSpan);
            
            SpanBuilder spanBuilder = tracer.spanBuilder(operationName)
                .setParent(parentContext)
                .setSpanKind(SpanKind.INTERNAL)
                .setStartTimestamp(Instant.now());
            
            addDefaultTags(spanBuilder);
            
            Span span = spanBuilder.startSpan();
            
            String spanId = span.getSpanContext().getSpanId();
            activeSpans.put(spanId, span);
            
            log.debug("创建子Span: operation={}, spanId={}, parentId={}", 
                operationName, spanId, parentSpan.getSpanContext().getSpanId());
            return span;
            
        } catch (Exception e) {
            log.error("创建子Span失败: operation={}", operationName, e);
            return Span.getInvalid();
        }
    }

    /**
     * 在Span上下文中执行操作
     */
    public <T> T executeInSpan(String operationName, Supplier<T> operation) {
        Span span = createSpan(operationName);
        try (Scope scope = span.makeCurrent()) {
            T result = operation.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            finishSpan(span);
        }
    }

    /**
     * 在Span上下文中执行操作（无返回值）
     */
    public void executeInSpan(String operationName, Runnable operation) {
        Span span = createSpan(operationName);
        try (Scope scope = span.makeCurrent()) {
            operation.run();
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            finishSpan(span);
        }
    }

    /**
     * 添加Span标签
     */
    public void addSpanTag(Span span, String key, String value) {
        if (span != null && !span.getSpanContext().equals(Span.getInvalid().getSpanContext())) {
            span.setAttribute(key, value);
        }
    }

    /**
     * 添加Span标签（数值）
     */
    public void addSpanTag(Span span, String key, long value) {
        if (span != null && !span.getSpanContext().equals(Span.getInvalid().getSpanContext())) {
            span.setAttribute(key, value);
        }
    }

    /**
     * 添加Span标签（布尔值）
     */
    public void addSpanTag(Span span, String key, boolean value) {
        if (span != null && !span.getSpanContext().equals(Span.getInvalid().getSpanContext())) {
            span.setAttribute(key, value);
        }
    }

    /**
     * 添加Span事件
     */
    public void addSpanEvent(Span span, String eventName) {
        if (span != null && !span.getSpanContext().equals(Span.getInvalid().getSpanContext())) {
            span.addEvent(eventName);
        }
    }

    /**
     * 添加Span事件（带属性）
     */
    public void addSpanEvent(Span span, String eventName, Map<String, String> attributes) {
        if (span != null && !span.getSpanContext().equals(Span.getInvalid().getSpanContext())) {
            io.opentelemetry.api.common.AttributesBuilder builder = io.opentelemetry.api.common.Attributes.builder();
            attributes.forEach(builder::put);
            span.addEvent(eventName, builder.build());
        }
    }

    /**
     * 记录异常
     */
    public void recordException(Span span, Throwable exception) {
        if (span != null && !span.getSpanContext().equals(Span.getInvalid().getSpanContext())) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
        }
    }

    /**
     * 设置Span状态
     */
    public void setSpanStatus(Span span, StatusCode statusCode, String description) {
        if (span != null && !span.getSpanContext().equals(Span.getInvalid().getSpanContext())) {
            span.setStatus(statusCode, description);
        }
    }

    /**
     * 完成Span
     */
    public void finishSpan(Span span) {
        if (span != null && !span.getSpanContext().equals(Span.getInvalid().getSpanContext())) {
            try {
                span.end();
                
                // 从缓存中移除
                String spanId = span.getSpanContext().getSpanId();
                activeSpans.remove(spanId);
                
                log.debug("完成Span: spanId={}", spanId);
            } catch (Exception e) {
                log.error("完成Span失败", e);
            }
        }
    }

    /**
     * 获取当前Span
     */
    public Span getCurrentSpan() {
        return Span.current();
    }

    /**
     * 获取当前追踪ID
     */
    public String getCurrentTraceId() {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null && !currentSpan.getSpanContext().equals(Span.getInvalid().getSpanContext())) {
            return currentSpan.getSpanContext().getTraceId();
        }
        return null;
    }

    /**
     * 获取当前SpanID
     */
    public String getCurrentSpanId() {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null && !currentSpan.getSpanContext().equals(Span.getInvalid().getSpanContext())) {
            return currentSpan.getSpanContext().getSpanId();
        }
        return null;
    }

    /**
     * 检查是否在追踪上下文中
     */
    public boolean isInTracingContext() {
        Span currentSpan = getCurrentSpan();
        return currentSpan != null && !currentSpan.getSpanContext().equals(Span.getInvalid().getSpanContext());
    }

    /**
     * 获取活跃Span数量
     */
    public int getActiveSpanCount() {
        return activeSpans.size();
    }

    /**
     * 清理所有活跃Span
     */
    public void cleanupActiveSpans() {
        activeSpans.values().forEach(this::finishSpan);
        activeSpans.clear();
        log.info("清理所有活跃Span完成");
    }

    /**
     * 添加默认标签
     */
    private void addDefaultTags(SpanBuilder spanBuilder) {
        // 添加服务信息
        spanBuilder.setAttribute("service.name", tracingConfig.getServiceName());
        spanBuilder.setAttribute("service.version", tracingConfig.getResource().getServiceVersion());
        spanBuilder.setAttribute("deployment.environment", tracingConfig.getResource().getEnvironment());
        
        // 添加自定义标签
        tracingConfig.getTags().forEach(spanBuilder::setAttribute);
        
        // 添加资源属性
        tracingConfig.getResource().getAttributes().forEach(spanBuilder::setAttribute);
    }
}