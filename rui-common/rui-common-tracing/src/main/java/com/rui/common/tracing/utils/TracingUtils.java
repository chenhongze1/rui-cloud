package com.rui.common.tracing.utils;

import com.rui.common.tracing.context.TracingContext;
import com.rui.common.tracing.manager.TracingManager;
import com.rui.common.tracing.model.TraceInfo;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
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

    private static TracingManager tracingManager;
    private static TracingContext tracingContext;

    @Autowired
    public void setTracingManager(TracingManager tracingManager) {
        TracingUtils.tracingManager = tracingManager;
    }

    @Autowired
    public void setTracingContext(TracingContext tracingContext) {
        TracingUtils.tracingContext = tracingContext;
    }

    /**
     * 获取当前追踪ID
     */
    public static String getCurrentTraceId() {
        try {
            if (tracingManager != null) {
                return tracingManager.getCurrentTraceId();
            }
            
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
                return currentSpan.getSpanContext().getTraceId();
            }
        } catch (Exception e) {
            log.debug("Error getting current trace ID", e);
        }
        return null;
    }

    /**
     * 获取当前SpanID
     */
    public static String getCurrentSpanId() {
        try {
            if (tracingManager != null) {
                return tracingManager.getCurrentSpanId();
            }
            
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
                return currentSpan.getSpanContext().getSpanId();
            }
        } catch (Exception e) {
            log.debug("Error getting current span ID", e);
        }
        return null;
    }

    /**
     * 获取当前Span
     */
    public static Span getCurrentSpan() {
        try {
            if (tracingManager != null) {
                return tracingManager.getCurrentSpan();
            }
            return Span.current();
        } catch (Exception e) {
            log.debug("Error getting current span", e);
            return null;
        }
    }

    /**
     * 检查当前是否有活跃的追踪
     */
    public static boolean hasActiveTrace() {
        try {
            Span currentSpan = getCurrentSpan();
            return currentSpan != null && currentSpan.getSpanContext().isValid();
        } catch (Exception e) {
            log.debug("Error checking active trace", e);
            return false;
        }
    }

    /**
     * 添加标签到当前Span
     */
    public static void addTag(String key, String value) {
        try {
            if (tracingManager != null) {
                tracingManager.addSpanAttribute(key, value);
            } else {
                Span currentSpan = getCurrentSpan();
                if (currentSpan != null) {
                    currentSpan.setAttribute(key, value);
                }
            }
        } catch (Exception e) {
            log.debug("Error adding tag: {}={}", key, value, e);
        }
    }

    /**
     * 添加数值标签到当前Span
     */
    public static void addTag(String key, long value) {
        try {
            if (tracingManager != null) {
                tracingManager.addSpanAttribute(key, value);
            } else {
                Span currentSpan = getCurrentSpan();
                if (currentSpan != null) {
                    currentSpan.setAttribute(key, value);
                }
            }
        } catch (Exception e) {
            log.debug("Error adding tag: {}={}", key, value, e);
        }
    }

    /**
     * 添加布尔标签到当前Span
     */
    public static void addTag(String key, boolean value) {
        try {
            if (tracingManager != null) {
                tracingManager.addSpanAttribute(key, value);
            } else {
                Span currentSpan = getCurrentSpan();
                if (currentSpan != null) {
                    currentSpan.setAttribute(key, value);
                }
            }
        } catch (Exception e) {
            log.debug("Error adding tag: {}={}", key, value, e);
        }
    }

    /**
     * 添加多个标签到当前Span
     */
    public static void addTags(Map<String, String> tags) {
        if (tags != null && !tags.isEmpty()) {
            tags.forEach(TracingUtils::addTag);
        }
    }

    /**
     * 记录异常到当前Span
     */
    public static void recordException(Throwable exception) {
        try {
            if (tracingManager != null) {
                tracingManager.recordException(exception);
            } else {
                Span currentSpan = getCurrentSpan();
                if (currentSpan != null) {
                    currentSpan.recordException(exception);
                }
            }
        } catch (Exception e) {
            log.debug("Error recording exception", e);
        }
    }

    /**
     * 添加事件到当前Span
     */
    public static void addEvent(String eventName) {
        try {
            if (tracingManager != null) {
                tracingManager.addEvent(eventName);
            } else {
                Span currentSpan = getCurrentSpan();
                if (currentSpan != null) {
                    currentSpan.addEvent(eventName);
                }
            }
        } catch (Exception e) {
            log.debug("Error adding event: {}", eventName, e);
        }
    }

    /**
     * 添加事件到当前Span，带属性
     */
    public static void addEvent(String eventName, Map<String, String> attributes) {
        try {
            if (tracingManager != null) {
                tracingManager.addEvent(eventName, attributes);
            } else {
                Span currentSpan = getCurrentSpan();
                if (currentSpan != null) {
                    io.opentelemetry.api.common.AttributesBuilder builder = io.opentelemetry.api.common.Attributes.builder();
                    attributes.forEach(builder::put);
                    currentSpan.addEvent(eventName, builder.build());
                }
            }
        } catch (Exception e) {
            log.debug("Error adding event: {}", eventName, e);
        }
    }

    /**
     * 在新的Span中执行操作
     */
    public static <T> T trace(String operationName, Supplier<T> operation) {
        return trace(operationName, SpanKind.INTERNAL, operation);
    }

    /**
     * 在新的Span中执行操作，指定Span类型
     */
    public static <T> T trace(String operationName, SpanKind spanKind, Supplier<T> operation) {
        if (tracingManager == null) {
            return operation.get();
        }

        Span span = tracingManager.startSpan(operationName, spanKind);
        try {
            return tracingManager.executeInSpan(span, operation::get);
        } finally {
            tracingManager.finishSpan(span);
        }
    }

    /**
     * 在新的Span中执行可能抛出异常的操作
     */
    public static <T> T traceCallable(String operationName, Callable<T> operation) throws Exception {
        return traceCallable(operationName, SpanKind.INTERNAL, operation);
    }

    /**
     * 在新的Span中执行可能抛出异常的操作，指定Span类型
     */
    public static <T> T traceCallable(String operationName, SpanKind spanKind, Callable<T> operation) throws Exception {
        if (tracingManager == null) {
            return operation.call();
        }

        Span span = tracingManager.startSpan(operationName, spanKind);
        try {
            return tracingManager.executeInSpan(span, operation::call);
        } finally {
            tracingManager.finishSpan(span);
        }
    }

    /**
     * 在新的Span中执行无返回值的操作
     */
    public static void trace(String operationName, Runnable operation) {
        trace(operationName, SpanKind.INTERNAL, operation);
    }

    /**
     * 在新的Span中执行无返回值的操作，指定Span类型
     */
    public static void trace(String operationName, SpanKind spanKind, Runnable operation) {
        if (tracingManager == null) {
            operation.run();
            return;
        }

        Span span = tracingManager.startSpan(operationName, spanKind);
        try {
            tracingManager.executeInSpan(span, () -> {
                operation.run();
                return null;
            });
        } finally {
            tracingManager.finishSpan(span);
        }
    }

    /**
     * 创建TraceInfo对象
     */
    public static TraceInfo createTraceInfo(Span span) {
        if (span == null) {
            return null;
        }

        try {
            SpanContext spanContext = span.getSpanContext();
            return TraceInfo.builder()
                    .traceId(spanContext.getTraceId())
                    .spanId(spanContext.getSpanId())
                    .startTime(LocalDateTime.now())
                    .status(TraceInfo.TraceStatus.OK)
                    .build();
        } catch (Exception e) {
            log.debug("Error creating trace info", e);
            return null;
        }
    }

    /**
     * 获取当前TraceInfo
     */
    public static Optional<TraceInfo> getCurrentTraceInfo() {
        try {
            Span currentSpan = getCurrentSpan();
            if (currentSpan != null) {
                return Optional.of(createTraceInfo(currentSpan));
            }
        } catch (Exception e) {
            log.debug("Error getting current trace info", e);
        }
        return Optional.empty();
    }

    /**
     * 生成唯一的追踪ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成唯一的SpanID
     */
    public static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 检查追踪ID是否有效
     */
    public static boolean isValidTraceId(String traceId) {
        return traceId != null && traceId.length() == 32 && traceId.matches("[0-9a-f]+");
    }

    /**
     * 检查SpanID是否有效
     */
    public static boolean isValidSpanId(String spanId) {
        return spanId != null && spanId.length() == 16 && spanId.matches("[0-9a-f]+");
    }

    /**
     * 格式化追踪信息用于日志输出
     */
    public static String formatTraceInfo() {
        try {
            String traceId = getCurrentTraceId();
            String spanId = getCurrentSpanId();
            
            if (traceId != null && spanId != null) {
                return String.format("[trace=%s,span=%s]", traceId, spanId);
            } else if (traceId != null) {
                return String.format("[trace=%s]", traceId);
            }
        } catch (Exception e) {
            log.debug("Error formatting trace info", e);
        }
        return "[trace=none]";
    }

    /**
     * 获取追踪上下文信息
     */
    public static Map<String, String> getTraceContext() {
        try {
            if (tracingManager != null) {
                Map<String, String> context = new java.util.HashMap<>();
                tracingManager.propagateContext(context);
                return context;
            }
        } catch (Exception e) {
            log.debug("Error getting trace context", e);
        }
        return new java.util.HashMap<>();
    }

    /**
     * 从上下文恢复追踪
     */
    public static Context restoreTraceContext(Map<String, String> context) {
        try {
            if (tracingManager != null && context != null) {
                return tracingManager.extractContext(context);
            }
        } catch (Exception e) {
            log.debug("Error restoring trace context", e);
        }
        return Context.current();
    }

    /**
     * 标记当前Span为错误状态
     */
    public static void markError(String errorMessage) {
        try {
            Span currentSpan = getCurrentSpan();
            if (currentSpan != null) {
                currentSpan.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, errorMessage);
                currentSpan.setAttribute("error", true);
                currentSpan.setAttribute("error.message", errorMessage);
            }
        } catch (Exception e) {
            log.debug("Error marking span as error", e);
        }
    }

    /**
     * 标记当前Span为成功状态
     */
    public static void markSuccess() {
        try {
            Span currentSpan = getCurrentSpan();
            if (currentSpan != null) {
                currentSpan.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            }
        } catch (Exception e) {
            log.debug("Error marking span as success", e);
        }
    }

    /**
     * 添加HTTP相关标签
     */
    public static void addHttpTags(String method, String url, int statusCode) {
        addTag("http.method", method);
        addTag("http.url", url);
        addTag("http.status_code", statusCode);
    }

    /**
     * 添加数据库相关标签
     */
    public static void addDatabaseTags(String type, String name, String operation) {
        addTag("db.type", type);
        addTag("db.name", name);
        addTag("db.operation", operation);
    }

    /**
     * 添加缓存相关标签
     */
    public static void addCacheTags(String type, String operation, String key, boolean hit) {
        addTag("cache.type", type);
        addTag("cache.operation", operation);
        addTag("cache.key", key);
        addTag("cache.hit", hit);
    }

    /**
     * 添加消息队列相关标签
     */
    public static void addMessageTags(String system, String destination, String operation) {
        addTag("messaging.system", system);
        addTag("messaging.destination", destination);
        addTag("messaging.operation", operation);
    }

    /**
     * 检查追踪是否启用
     */
    public static boolean isTracingEnabled() {
        try {
            return tracingManager != null && tracingManager.isTracingEnabled();
        } catch (Exception e) {
            log.debug("Error checking if tracing is enabled", e);
            return false;
        }
    }

    /**
     * 获取活跃Span数量
     */
    public static int getActiveSpanCount() {
        try {
            return tracingManager != null ? tracingManager.getActiveSpanCount() : 0;
        } catch (Exception e) {
            log.debug("Error getting active span count", e);
            return 0;
        }
    }

    /**
     * 获取总Span数量
     */
    public static long getTotalSpanCount() {
        try {
            return tracingManager != null ? tracingManager.getTotalSpanCount() : 0;
        } catch (Exception e) {
            log.debug("Error getting total span count", e);
            return 0;
        }
    }
}