package com.rui.common.tracing.manager;

import com.rui.common.tracing.config.TracingProperties;
import com.rui.common.tracing.context.TracingContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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

    private final OpenTelemetry openTelemetry;
    private final TracingProperties tracingProperties;
    private final TracingContext tracingContext;
    
    private final Tracer tracer;
    
    // 活跃Span的缓存
    private final Map<String, Span> activeSpans = new ConcurrentHashMap<>();
    
    // Span计数器
    private final AtomicLong spanCounter = new AtomicLong(0);
    
    public TracingManager(OpenTelemetry openTelemetry, TracingProperties tracingProperties, TracingContext tracingContext) {
        this.openTelemetry = openTelemetry;
        this.tracingProperties = tracingProperties;
        this.tracingContext = tracingContext;
        this.tracer = openTelemetry.getTracer(tracingProperties.getServiceName(), tracingProperties.getServiceVersion());
    }

    /**
     * 开始一个新的Span
     */
    public Span startSpan(String operationName) {
        return startSpan(operationName, SpanKind.INTERNAL);
    }

    /**
     * 开始一个新的Span，指定类型
     */
    public Span startSpan(String operationName, SpanKind spanKind) {
        return startSpan(operationName, spanKind, null);
    }

    /**
     * 开始一个新的Span，指定父Span
     */
    public Span startSpan(String operationName, SpanKind spanKind, Span parentSpan) {
        try {
            SpanBuilder spanBuilder = tracer.spanBuilder(operationName)
                    .setSpanKind(spanKind);
            
            // 设置父Span
            if (parentSpan != null) {
                spanBuilder.setParent(Context.current().with(parentSpan));
            }
            
            // 创建Span
            Span span = spanBuilder.startSpan();
            
            // 添加默认标签
            addDefaultTags(span);
            
            // 添加到活跃Span缓存
            String spanId = span.getSpanContext().getSpanId();
            activeSpans.put(spanId, span);
            
            // 更新上下文
            tracingContext.setCurrentSpan(span);
            
            // 增加计数器
            spanCounter.incrementAndGet();
            
            log.debug("Started span: {} [{}]", operationName, spanId);
            
            return span;
            
        } catch (Exception e) {
            log.error("Error starting span: {}", operationName, e);
            // 返回一个无操作的Span
            return tracer.spanBuilder("error-span").startSpan();
        }
    }

    /**
     * 结束Span
     */
    public void finishSpan(Span span) {
        if (span == null) {
            return;
        }
        
        try {
            String spanId = span.getSpanContext().getSpanId();
            
            // 从活跃Span缓存中移除
            activeSpans.remove(spanId);
            
            // 结束Span
            span.end();
            
            // 清理上下文
            if (tracingContext.getCurrentSpan() == span) {
                tracingContext.clearCurrentSpan();
            }
            
            log.debug("Finished span: [{}]", spanId);
            
        } catch (Exception e) {
            log.error("Error finishing span", e);
        }
    }

    /**
     * 获取当前活跃的Span
     */
    public Span getCurrentSpan() {
        return tracingContext.getCurrentSpan();
    }

    /**
     * 设置当前Span
     */
    public void setCurrentSpan(Span span) {
        tracingContext.setCurrentSpan(span);
    }

    /**
     * 清除当前Span
     */
    public void clearCurrentSpan() {
        tracingContext.clearCurrentSpan();
    }

    /**
     * 在指定Span的上下文中执行操作
     */
    public <T> T executeInSpan(Span span, SpanOperation<T> operation) {
        if (span == null) {
            try {
                return operation.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        try (Scope scope = span.makeCurrent()) {
            Span previousSpan = tracingContext.getCurrentSpan();
            tracingContext.setCurrentSpan(span);
            
            try {
                return operation.execute();
            } catch (Exception e) {
                span.recordException(e);
                throw new RuntimeException(e);
            } finally {
                tracingContext.setCurrentSpan(previousSpan);
            }
        }
    }

    /**
     * 创建子Span
     */
    public Span createChildSpan(String operationName) {
        Span currentSpan = getCurrentSpan();
        return startSpan(operationName, SpanKind.INTERNAL, currentSpan);
    }

    /**
     * 创建子Span，指定类型
     */
    public Span createChildSpan(String operationName, SpanKind spanKind) {
        Span currentSpan = getCurrentSpan();
        return startSpan(operationName, spanKind, currentSpan);
    }

    /**
     * 添加Span属性
     */
    public void addSpanAttribute(String key, String value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.setAttribute(key, value);
        }
    }

    /**
     * 添加Span属性
     */
    public void addSpanAttribute(String key, long value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.setAttribute(key, value);
        }
    }

    /**
     * 添加Span属性
     */
    public void addSpanAttribute(String key, double value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.setAttribute(key, value);
        }
    }

    /**
     * 添加Span属性
     */
    public void addSpanAttribute(String key, boolean value) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.setAttribute(key, value);
        }
    }

    /**
     * 记录异常
     */
    public void recordException(Throwable exception) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.recordException(exception);
        }
    }

    /**
     * 添加事件
     */
    public void addEvent(String eventName) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.addEvent(eventName);
        }
    }

    /**
     * 添加事件，带属性
     */
    public void addEvent(String eventName, Map<String, String> attributes) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            io.opentelemetry.api.common.AttributesBuilder builder = io.opentelemetry.api.common.Attributes.builder();
            attributes.forEach(builder::put);
            currentSpan.addEvent(eventName, builder.build());
        }
    }

    /**
     * 获取当前追踪ID
     */
    public String getCurrentTraceId() {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            return currentSpan.getSpanContext().getTraceId();
        }
        return null;
    }

    /**
     * 获取当前SpanID
     */
    public String getCurrentSpanId() {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            return currentSpan.getSpanContext().getSpanId();
        }
        return null;
    }

    /**
     * 获取活跃Span数量
     */
    public int getActiveSpanCount() {
        return activeSpans.size();
    }

    /**
     * 获取总Span数量
     */
    public long getTotalSpanCount() {
        return spanCounter.get();
    }

    /**
     * 清理所有活跃Span
     */
    public void clearAllActiveSpans() {
        activeSpans.values().forEach(this::finishSpan);
        activeSpans.clear();
    }

    /**
     * 添加默认标签
     */
    private void addDefaultTags(Span span) {
        // 添加服务信息
        span.setAttribute("service.name", tracingProperties.getServiceName());
        span.setAttribute("service.version", tracingProperties.getServiceVersion());
        
        // 添加配置的自定义标签
        tracingProperties.getTags().forEach(span::setAttribute);
        
        // 添加环境信息
        String environment = System.getProperty("spring.profiles.active", "default");
        span.setAttribute("environment", environment);
        
        // 添加主机信息
        try {
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            span.setAttribute("host.name", hostname);
        } catch (Exception e) {
            // 忽略异常
        }
    }

    /**
     * Span操作接口
     */
    @FunctionalInterface
    public interface SpanOperation<T> {
        T execute() throws Exception;
    }

    /**
     * 获取追踪器
     */
    public Tracer getTracer() {
        return tracer;
    }

    /**
     * 获取OpenTelemetry实例
     */
    public OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    /**
     * 检查追踪是否启用
     */
    public boolean isTracingEnabled() {
        return tracingProperties.isEnabled();
    }

    /**
     * 获取采样率
     */
    public double getSamplingRate() {
        return tracingProperties.getSampling().getRate();
    }

    /**
     * 创建根Span
     */
    public Span createRootSpan(String operationName) {
        return tracer.spanBuilder(operationName)
                .setNoParent()
                .startSpan();
    }

    /**
     * 从上下文创建Span
     */
    public Span createSpanFromContext(String operationName, Context context) {
        return tracer.spanBuilder(operationName)
                .setParent(context)
                .startSpan();
    }

    /**
     * 获取当前上下文
     */
    public Context getCurrentContext() {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            return Context.current().with(currentSpan);
        }
        return Context.current();
    }

    // TextMapSetter实现
    private static final TextMapSetter<Map<String, String>> TEXT_MAP_SETTER = 
            new TextMapSetter<Map<String, String>>() {
                @Override
                public void set(Map<String, String> carrier, String key, String value) {
                    if (carrier != null) {
                        carrier.put(key, value);
                    }
                }
            };
    
    // TextMapGetter实现
    private static final TextMapGetter<Map<String, String>> TEXT_MAP_GETTER = 
            new TextMapGetter<Map<String, String>>() {
                @Override
                public Iterable<String> keys(Map<String, String> carrier) {
                    return carrier.keySet();
                }
                
                @Override
                public String get(Map<String, String> carrier, String key) {
                    return carrier.get(key);
                }
            };

    /**
     * 传播上下文到头部
     */
    public void propagateContext(Map<String, String> headers) {
        Context context = getCurrentContext();
        openTelemetry.getPropagators().getTextMapPropagator()
                .inject(context, headers, TEXT_MAP_SETTER);
    }

    /**
     * 从头部提取上下文
     */
    public Context extractContext(Map<String, String> headers) {
        return openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), headers, TEXT_MAP_GETTER);
    }
}