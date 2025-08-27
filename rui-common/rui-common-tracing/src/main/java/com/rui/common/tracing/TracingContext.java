package com.rui.common.tracing;

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
}