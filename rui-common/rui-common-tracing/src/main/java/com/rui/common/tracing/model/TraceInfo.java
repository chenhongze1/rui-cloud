package com.rui.common.tracing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 追踪信息类
 * 用于存储单次追踪的详细信息
 *
 * @author rui
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceInfo {

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * SpanID
     */
    private String spanId;

    /**
     * 父SpanID
     */
    private String parentSpanId;

    /**
     * 操作名称
     */
    private String operationName;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务版本
     */
    private String serviceVersion;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 持续时间（毫秒）
     */
    private Long duration;

    /**
     * 状态
     */
    private TraceStatus status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 异常堆栈
     */
    private String stackTrace;

    /**
     * 标签
     */
    @Builder.Default
    private Map<String, String> tags = new ConcurrentHashMap<>();

    /**
     * 事件列表
     */
    @Builder.Default
    private Map<String, Object> events = new ConcurrentHashMap<>();

    /**
     * 资源信息
     */
    @Builder.Default
    private Map<String, String> resources = new ConcurrentHashMap<>();

    /**
     * HTTP相关信息
     */
    private HttpInfo httpInfo;

    /**
     * 数据库相关信息
     */
    private DatabaseInfo databaseInfo;

    /**
     * 消息队列相关信息
     */
    private MessageInfo messageInfo;

    /**
     * 缓存相关信息
     */
    private CacheInfo cacheInfo;

    /**
     * 追踪状态枚举
     */
    public enum TraceStatus {
        /**
         * 成功
         */
        OK,
        /**
         * 错误
         */
        ERROR,
        /**
         * 超时
         */
        TIMEOUT,
        /**
         * 取消
         */
        CANCELLED,
        /**
         * 未知
         */
        UNKNOWN
    }

    /**
     * HTTP信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HttpInfo {
        /**
         * HTTP方法
         */
        private String method;

        /**
         * URL
         */
        private String url;

        /**
         * 状态码
         */
        private Integer statusCode;

        /**
         * User-Agent
         */
        private String userAgent;

        /**
         * 客户端IP
         */
        private String clientIp;

        /**
         * 请求大小
         */
        private Long requestSize;

        /**
         * 响应大小
         */
        private Long responseSize;

        /**
         * Content-Type
         */
        private String contentType;
    }

    /**
     * 数据库信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseInfo {
        /**
         * 数据库类型
         */
        private String type;

        /**
         * 数据库名称
         */
        private String name;

        /**
         * 表名
         */
        private String table;

        /**
         * SQL语句
         */
        private String sql;

        /**
         * 操作类型
         */
        private String operation;

        /**
         * 影响行数
         */
        private Integer rowsAffected;

        /**
         * 连接池信息
         */
        private String connectionPool;
    }

    /**
     * 消息信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageInfo {
        /**
         * 消息系统类型
         */
        private String system;

        /**
         * 主题/队列名称
         */
        private String destination;

        /**
         * 操作类型（发送/接收）
         */
        private String operation;

        /**
         * 消息ID
         */
        private String messageId;

        /**
         * 消息大小
         */
        private Long messageSize;

        /**
         * 分区
         */
        private String partition;

        /**
         * 偏移量
         */
        private Long offset;
    }

    /**
     * 缓存信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheInfo {
        /**
         * 缓存类型
         */
        private String type;

        /**
         * 缓存名称
         */
        private String name;

        /**
         * 操作类型
         */
        private String operation;

        /**
         * 缓存键
         */
        private String key;

        /**
         * 是否命中
         */
        private Boolean hit;

        /**
         * 数据大小
         */
        private Long dataSize;

        /**
         * TTL
         */
        private Long ttl;
    }

    /**
     * 添加标签
     */
    public void addTag(String key, String value) {
        if (tags == null) {
            tags = new ConcurrentHashMap<>();
        }
        tags.put(key, value);
    }

    /**
     * 添加事件
     */
    public void addEvent(String name, Object data) {
        if (events == null) {
            events = new ConcurrentHashMap<>();
        }
        events.put(name, data);
    }

    /**
     * 添加资源信息
     */
    public void addResource(String key, String value) {
        if (resources == null) {
            resources = new ConcurrentHashMap<>();
        }
        resources.put(key, value);
    }

    /**
     * 计算持续时间
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            duration = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    /**
     * 检查是否为错误状态
     */
    public boolean isError() {
        return status == TraceStatus.ERROR;
    }

    /**
     * 检查是否为成功状态
     */
    public boolean isSuccess() {
        return status == TraceStatus.OK;
    }

    /**
     * 检查是否为慢请求
     */
    public boolean isSlowRequest(long thresholdMs) {
        return duration != null && duration > thresholdMs;
    }

    /**
     * 获取简化的追踪信息
     */
    public String getSimpleInfo() {
        return String.format("[%s] %s - %s (%dms)", 
                traceId, operationName, status, duration);
    }

    /**
     * 获取详细的追踪信息
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("TraceInfo{\n");
        sb.append("  traceId='" + traceId + "'\n");
        sb.append("  spanId='" + spanId + "'\n");
        sb.append("  operationName='" + operationName + "'\n");
        sb.append("  serviceName='" + serviceName + "'\n");
        sb.append("  duration=" + duration + "\n");
        sb.append("  status=" + status + "\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 设置异常信息
     * @param exception 异常对象
     */
    public void setException(Throwable exception) {
        if (exception != null) {
            this.errorMessage = exception.getMessage();
            this.stackTrace = getStackTrace(exception);
            this.status = TraceStatus.ERROR;
        }
    }

    /**
     * 获取异常堆栈信息
     * @param exception 异常对象
     * @return 堆栈信息字符串
     */
    private String getStackTrace(Throwable exception) {
        if (exception == null) {
            return null;
        }
        
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}