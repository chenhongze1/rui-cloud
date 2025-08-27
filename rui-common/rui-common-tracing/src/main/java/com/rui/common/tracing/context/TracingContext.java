package com.rui.common.tracing.context;

import com.rui.common.tracing.model.TraceInfo;

/**
 * 链路追踪上下文
 * 用于在线程本地存储追踪信息
 *
 * @author rui
 * @since 1.0.0
 */
public class TracingContext {
    
    private static final ThreadLocal<TraceInfo> TRACE_CONTEXT = new ThreadLocal<>();
    
    /**
     * 设置当前追踪信息
     */
    public static void setCurrentTrace(TraceInfo traceInfo) {
        TRACE_CONTEXT.set(traceInfo);
    }
    
    /**
     * 获取当前追踪信息
     */
    public static TraceInfo getCurrentTrace() {
        return TRACE_CONTEXT.get();
    }
    
    /**
     * 获取当前追踪ID
     */
    public static String getCurrentTraceId() {
        TraceInfo traceInfo = getCurrentTrace();
        return traceInfo != null ? traceInfo.getTraceId() : null;
    }
    
    /**
     * 获取当前Span ID
     */
    public static String getCurrentSpanId() {
        TraceInfo traceInfo = getCurrentTrace();
        return traceInfo != null ? traceInfo.getSpanId() : null;
    }
    
    /**
     * 清理当前线程的追踪信息
     */
    public static void clear() {
        TRACE_CONTEXT.remove();
    }
    
    /**
     * 检查当前线程是否有追踪信息
     */
    public static boolean hasCurrentTrace() {
        return getCurrentTrace() != null;
    }
    
    /**
     * 获取当前追踪的操作名称
     */
    public static String getCurrentOperationName() {
        TraceInfo traceInfo = getCurrentTrace();
        return traceInfo != null ? traceInfo.getOperationName() : null;
    }
    
    /**
     * 添加标签到当前追踪
     */
    public static void addTag(String key, String value) {
        TraceInfo traceInfo = getCurrentTrace();
        if (traceInfo != null) {
            traceInfo.addTag(key, value);
        }
    }
    
    /**
     * 添加事件到当前追踪
     */
    public static void addEvent(String event) {
        TraceInfo traceInfo = getCurrentTrace();
        if (traceInfo != null) {
            traceInfo.addEvent(event, System.currentTimeMillis());
        }
    }
    
    /**
     * 设置当前追踪的异常
     */
    public static void setException(Throwable exception) {
        TraceInfo traceInfo = getCurrentTrace();
        if (traceInfo != null) {
            traceInfo.setException(exception);
        }
    }
    
    /**
     * 获取追踪上下文的字符串表示
     */
    public static String getTraceContextString() {
        TraceInfo traceInfo = getCurrentTrace();
        if (traceInfo != null) {
            return String.format("[traceId=%s, spanId=%s, operation=%s]", 
                    traceInfo.getTraceId(), 
                    traceInfo.getSpanId(), 
                    traceInfo.getOperationName());
        }
        return "[no-trace]";
    }
}