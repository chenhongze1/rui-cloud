package com.rui.common.tracing;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 追踪信息
 * 用于存储单次追踪的详细信息
 *
 * @author rui
 * @since 1.0.0
 */
public class TraceInfo {
    
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String operationName;
    private long startTime;
    private long endTime;
    private Map<String, String> tags;
    private Map<String, Long> events;
    private Throwable exception;
    
    public TraceInfo(String traceId, String spanId, String operationName) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.operationName = operationName;
        this.tags = new ConcurrentHashMap<>();
        this.events = new ConcurrentHashMap<>();
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public String getSpanId() {
        return spanId;
    }
    
    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }
    
    public String getParentSpanId() {
        return parentSpanId;
    }
    
    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }
    
    public String getOperationName() {
        return operationName;
    }
    
    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public long getDuration() {
        return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
    }
    
    public Map<String, String> getTags() {
        return new HashMap<>(tags);
    }
    
    public void addTag(String key, String value) {
        this.tags.put(key, value);
    }
    
    public Map<String, Long> getEvents() {
        return new HashMap<>(events);
    }
    
    public void addEvent(String event, long timestamp) {
        this.events.put(event, timestamp);
    }
    
    public Throwable getException() {
        return exception;
    }
    
    public void setException(Throwable exception) {
        this.exception = exception;
    }
    
    @Override
    public String toString() {
        return "TraceInfo{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", operationName='" + operationName + '\'' +
                ", duration=" + getDuration() +
                ", tags=" + tags +
                '}';
    }
}