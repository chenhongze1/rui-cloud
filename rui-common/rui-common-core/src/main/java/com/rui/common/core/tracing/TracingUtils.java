package com.rui.common.core.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 链路追踪工具类
 * 提供便捷的追踪操作方法
 *
 * @author rui
 */
@Slf4j
@Component
public class TracingUtils {

    private static final String INSTRUMENTATION_NAME = "rui-cloud";
    private static final String INSTRUMENTATION_VERSION = "1.0.0";
    
    private static OpenTelemetry openTelemetry;
    private static Tracer tracer;
    
    @Autowired
    public void setOpenTelemetry(OpenTelemetry openTelemetry) {
        TracingUtils.openTelemetry = openTelemetry;
        TracingUtils.tracer = openTelemetry.getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
    }
    
    /**
     * 获取当前活跃的Span
     */
    public static Span getCurrentSpan() {
        return Span.current();
    }
    
    /**
     * 获取当前的追踪ID
     */
    public static String getCurrentTraceId() {
        return getCurrentSpan().getSpanContext().getTraceId();
    }
    
    /**
     * 获取当前的SpanID
     */
    public static String getCurrentSpanId() {
        return getCurrentSpan().getSpanContext().getSpanId();
    }
    
    /**
     * 创建一个新的Span
     */
    public static Span createSpan(String spanName) {
        return createSpan(spanName, SpanKind.INTERNAL);
    }
    
    /**
     * 创建一个新的Span
     */
    public static Span createSpan(String spanName, SpanKind spanKind) {
        if (tracer == null) {
            return Span.getInvalid();
        }
        
        return tracer.spanBuilder(spanName)
            .setSpanKind(spanKind)
            .startSpan();
    }
    
    /**
     * 创建一个子Span
     */
    public static Span createChildSpan(String spanName, Span parentSpan) {
        if (tracer == null) {
            return Span.getInvalid();
        }
        
        return tracer.spanBuilder(spanName)
            .setParent(Context.current().with(parentSpan))
            .startSpan();
    }
    
    /**
     * 为Span添加属性
     */
    public static void addSpanAttributes(Span span, Map<String, String> attributes) {
        if (span != null && attributes != null) {
            attributes.forEach(span::setAttribute);
        }
    }
    
    /**
     * 为Span添加事件
     */
    public static void addSpanEvent(Span span, String eventName) {
        if (span != null) {
            span.addEvent(eventName);
        }
    }
    
    /**
     * 为Span添加事件和属性
     */
    public static void addSpanEvent(Span span, String eventName, Map<String, String> attributes) {
        if (span != null) {
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
    public static void recordException(Span span, Throwable throwable) {
        if (span != null && throwable != null) {
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR, throwable.getMessage());
        }
    }
    
    /**
     * 设置Span状态
     */
    public static void setSpanStatus(Span span, StatusCode statusCode, String description) {
        if (span != null) {
            span.setStatus(statusCode, description);
        }
    }
    
    /**
     * 在Span作用域内执行代码
     */
    public static void runInSpan(String spanName, Runnable runnable) {
        Span span = createSpan(spanName);
        try (Scope scope = span.makeCurrent()) {
            runnable.run();
        } catch (Exception e) {
            recordException(span, e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    /**
     * 在Span作用域内执行代码并返回结果
     */
    public static <T> T runInSpan(String spanName, Supplier<T> supplier) {
        Span span = createSpan(spanName);
        try (Scope scope = span.makeCurrent()) {
            return supplier.get();
        } catch (Exception e) {
            recordException(span, e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    /**
     * 在Span作用域内执行Callable
     */
    public static <T> T runInSpan(String spanName, Callable<T> callable) throws Exception {
        Span span = createSpan(spanName);
        try (Scope scope = span.makeCurrent()) {
            return callable.call();
        } catch (Exception e) {
            recordException(span, e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    /**
     * 在Span作用域内执行代码，并提供Span给回调函数
     */
    public static void runInSpan(String spanName, Consumer<Span> consumer) {
        Span span = createSpan(spanName);
        try (Scope scope = span.makeCurrent()) {
            consumer.accept(span);
        } catch (Exception e) {
            recordException(span, e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    /**
     * 在Span作用域内执行代码并返回结果，并提供Span给回调函数
     */
    public static <T> T runInSpan(String spanName, Function<Span, T> function) {
        Span span = createSpan(spanName);
        try (Scope scope = span.makeCurrent()) {
            return function.apply(span);
        } catch (Exception e) {
            recordException(span, e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    /**
     * 创建数据库操作Span
     */
    public static Span createDatabaseSpan(String operation, String tableName) {
        String spanName = String.format("db.%s.%s", operation.toLowerCase(), tableName);
        Span span = createSpan(spanName, SpanKind.CLIENT);
        
        if (span != null) {
            span.setAttribute("db.operation", operation);
            span.setAttribute("db.sql.table", tableName);
            span.setAttribute("db.system", "mysql"); // 可以根据实际情况配置
        }
        
        return span;
    }
    
    /**
     * 创建HTTP客户端Span
     */
    public static Span createHttpClientSpan(String method, String url) {
        String spanName = String.format("%s %s", method.toUpperCase(), url);
        Span span = createSpan(spanName, SpanKind.CLIENT);
        
        if (span != null) {
            span.setAttribute("http.method", method.toUpperCase());
            span.setAttribute("http.url", url);
        }
        
        return span;
    }
    
    /**
     * 创建Redis操作Span
     */
    public static Span createRedisSpan(String operation, String key) {
        String spanName = String.format("redis.%s", operation.toLowerCase());
        Span span = createSpan(spanName, SpanKind.CLIENT);
        
        if (span != null) {
            span.setAttribute("db.system", "redis");
            span.setAttribute("db.operation", operation);
            if (key != null) {
                span.setAttribute("db.redis.key", key);
            }
        }
        
        return span;
    }
    
    /**
     * 创建消息队列Span
     */
    public static Span createMessagingSpan(String operation, String destination) {
        String spanName = String.format("messaging.%s.%s", operation.toLowerCase(), destination);
        Span span = createSpan(spanName, 
            "send".equalsIgnoreCase(operation) ? SpanKind.PRODUCER : SpanKind.CONSUMER);
        
        if (span != null) {
            span.setAttribute("messaging.system", "rabbitmq"); // 可以根据实际情况配置
            span.setAttribute("messaging.operation", operation);
            span.setAttribute("messaging.destination", destination);
        }
        
        return span;
    }
    
    /**
     * 创建业务操作Span
     */
    public static Span createBusinessSpan(String service, String operation) {
        String spanName = String.format("%s.%s", service, operation);
        Span span = createSpan(spanName, SpanKind.INTERNAL);
        
        if (span != null) {
            span.setAttribute("business.service", service);
            span.setAttribute("business.operation", operation);
        }
        
        return span;
    }
    
    /**
     * 检查追踪是否启用
     */
    public static boolean isTracingEnabled() {
        return tracer != null && openTelemetry != null;
    }
    
    /**
     * 获取追踪上下文信息
     */
    public static String getTraceContext() {
        if (!isTracingEnabled()) {
            return "";
        }
        
        Span currentSpan = getCurrentSpan();
        if (currentSpan.getSpanContext().isValid()) {
            return String.format("traceId=%s,spanId=%s", 
                getCurrentTraceId(), getCurrentSpanId());
        }
        
        return "";
    }
}