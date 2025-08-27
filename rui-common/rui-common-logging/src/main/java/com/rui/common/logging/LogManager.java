package com.rui.common.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rui.common.logging.config.LoggingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 日志管理器
 * 提供统一的日志记录功能
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogManager {

    private final LoggingConfig loggingConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 不同类型的日志记录器
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");
    private static final Logger PERFORMANCE_LOGGER = LoggerFactory.getLogger("PERFORMANCE");
    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("SECURITY");
    private static final Logger BUSINESS_LOGGER = LoggerFactory.getLogger("BUSINESS");
    
    // MDC键名常量
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String USER_ID = "userId";
    public static final String SESSION_ID = "sessionId";
    public static final String REQUEST_ID = "requestId";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String USER_AGENT = "userAgent";
    public static final String MODULE = "module";
    public static final String OPERATION = "operation";
    
    /**
     * 记录审计日志
     */
    public void audit(String operation, String resource, String result, Object details) {
        if (!loggingConfig.getAudit().isEnabled()) {
            return;
        }
        
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        auditData.put("operation", operation);
        auditData.put("resource", resource);
        auditData.put("result", result);
        auditData.put("userId", MDC.get(USER_ID));
        auditData.put("sessionId", MDC.get(SESSION_ID));
        auditData.put("ipAddress", MDC.get(IP_ADDRESS));
        auditData.put("userAgent", MDC.get(USER_AGENT));
        
        if (details != null) {
            auditData.put("details", sanitizeData(details));
        }
        
        AUDIT_LOGGER.info(formatStructuredLog(auditData));
    }
    
    /**
     * 记录性能日志
     */
    public void performance(String operation, long duration, Map<String, Object> metrics) {
        if (!loggingConfig.getPerformance().isEnabled()) {
            return;
        }
        
        Map<String, Object> perfData = new HashMap<>();
        perfData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        perfData.put("operation", operation);
        perfData.put("duration", duration);
        perfData.put("traceId", MDC.get(TRACE_ID));
        perfData.put("spanId", MDC.get(SPAN_ID));
        
        if (metrics != null) {
            perfData.putAll(metrics);
        }
        
        // 判断是否为慢操作
        boolean isSlow = false;
        if (operation.contains("sql") || operation.contains("query")) {
            isSlow = duration > loggingConfig.getPerformance().getSlowQueryThreshold();
        } else if (operation.contains("api") || operation.contains("http")) {
            isSlow = duration > loggingConfig.getPerformance().getSlowApiThreshold();
        }
        
        if (isSlow) {
            PERFORMANCE_LOGGER.warn("SLOW_OPERATION: " + formatStructuredLog(perfData));
        } else {
            PERFORMANCE_LOGGER.info(formatStructuredLog(perfData));
        }
    }
    
    /**
     * 记录安全日志
     */
    public void security(String event, String level, String description, Object details) {
        if (!loggingConfig.getSecurity().isEnabled()) {
            return;
        }
        
        Map<String, Object> securityData = new HashMap<>();
        securityData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        securityData.put("event", event);
        securityData.put("level", level);
        securityData.put("description", description);
        securityData.put("userId", MDC.get(USER_ID));
        securityData.put("sessionId", MDC.get(SESSION_ID));
        securityData.put("ipAddress", MDC.get(IP_ADDRESS));
        securityData.put("userAgent", MDC.get(USER_AGENT));
        securityData.put("traceId", MDC.get(TRACE_ID));
        
        if (details != null) {
            securityData.put("details", sanitizeData(details));
        }
        
        String logMessage = formatStructuredLog(securityData);
        
        switch (level.toUpperCase()) {
            case "CRITICAL":
            case "HIGH":
                SECURITY_LOGGER.error(logMessage);
                break;
            case "MEDIUM":
                SECURITY_LOGGER.warn(logMessage);
                break;
            case "LOW":
            case "INFO":
            default:
                SECURITY_LOGGER.info(logMessage);
                break;
        }
    }
    
    /**
     * 记录业务日志
     */
    public void business(String module, String operation, String result, Object data) {
        if (!loggingConfig.getBusiness().isEnabled()) {
            return;
        }
        
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        businessData.put("module", module);
        businessData.put("operation", operation);
        businessData.put("result", result);
        businessData.put("userId", MDC.get(USER_ID));
        businessData.put("traceId", MDC.get(TRACE_ID));
        
        if (data != null) {
            businessData.put("data", sanitizeData(data));
        }
        
        BUSINESS_LOGGER.info(formatStructuredLog(businessData));
    }
    
    /**
     * 记录错误日志
     */
    public void error(String message, Throwable throwable, Map<String, Object> context) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorData.put("message", message);
        errorData.put("traceId", MDC.get(TRACE_ID));
        errorData.put("spanId", MDC.get(SPAN_ID));
        errorData.put("userId", MDC.get(USER_ID));
        
        if (throwable != null) {
            errorData.put("exception", throwable.getClass().getSimpleName());
            errorData.put("exceptionMessage", throwable.getMessage());
        }
        
        if (context != null) {
            errorData.putAll(context);
        }
        
        log.error(formatStructuredLog(errorData), throwable);
    }
    
    /**
     * 设置MDC上下文
     */
    public void setContext(String key, String value) {
        if (StringUtils.hasText(value)) {
            MDC.put(key, value);
        }
    }
    
    /**
     * 设置请求上下文
     */
    public void setRequestContext(String userId, String sessionId, String ipAddress, String userAgent) {
        setContext(USER_ID, userId);
        setContext(SESSION_ID, sessionId);
        setContext(IP_ADDRESS, ipAddress);
        setContext(USER_AGENT, userAgent);
        setContext(REQUEST_ID, UUID.randomUUID().toString());
    }
    
    /**
     * 设置追踪上下文
     */
    public void setTraceContext(String traceId, String spanId) {
        setContext(TRACE_ID, traceId);
        setContext(SPAN_ID, spanId);
    }
    
    /**
     * 清除MDC上下文
     */
    public void clearContext() {
        MDC.clear();
    }
    
    /**
     * 清除指定的MDC键
     */
    public void clearContext(String key) {
        MDC.remove(key);
    }
    
    /**
     * 格式化结构化日志
     */
    private String formatStructuredLog(Map<String, Object> data) {
        if (!loggingConfig.getStructured().isEnabled()) {
            return data.toString();
        }
        
        try {
            // 添加自定义字段
            Map<String, Object> logData = new HashMap<>(data);
            logData.putAll(loggingConfig.getStructured().getCustomFields());
            logData.put("application", loggingConfig.getApplicationName());
            logData.put("environment", loggingConfig.getEnvironment());
            
            return objectMapper.writeValueAsString(logData);
        } catch (JsonProcessingException e) {
            log.warn("Failed to format structured log", e);
            return data.toString();
        }
    }
    
    /**
     * 清理敏感数据
     */
    private Object sanitizeData(Object data) {
        if (!loggingConfig.getSensitive().isEnabled() || data == null) {
            return data;
        }
        
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            
            // 替换敏感字段
            for (String sensitiveField : loggingConfig.getSensitive().getSensitiveFields()) {
                if (loggingConfig.getSensitive().isUseRegex()) {
                    jsonString = jsonString.replaceAll(sensitiveField, loggingConfig.getSensitive().getReplacement());
                } else {
                    jsonString = jsonString.replaceAll(
                        "\"" + sensitiveField + "\"\s*:\s*\"[^\"]*\"",
                        "\"" + sensitiveField + "\":\"" + loggingConfig.getSensitive().getReplacement() + "\""
                    );
                }
            }
            
            // 应用自定义正则表达式
            for (String pattern : loggingConfig.getSensitive().getRegexPatterns()) {
                jsonString = jsonString.replaceAll(pattern, loggingConfig.getSensitive().getReplacement());
            }
            
            return objectMapper.readValue(jsonString, Object.class);
        } catch (Exception e) {
            log.warn("Failed to sanitize data", e);
            return "[DATA_SANITIZATION_FAILED]";
        }
    }
    
    /**
     * 记录HTTP请求日志
     */
    public void httpRequest(String method, String uri, int status, long duration, String userAgent, String ip) {
        Map<String, Object> httpData = new HashMap<>();
        httpData.put("method", method);
        httpData.put("uri", uri);
        httpData.put("status", status);
        httpData.put("duration", duration);
        httpData.put("userAgent", userAgent);
        httpData.put("ip", ip);
        
        if (status >= 400) {
            log.warn("HTTP_ERROR: " + formatStructuredLog(httpData));
        } else if (duration > loggingConfig.getPerformance().getSlowApiThreshold()) {
            log.warn("SLOW_HTTP: " + formatStructuredLog(httpData));
        } else {
            log.info("HTTP_REQUEST: " + formatStructuredLog(httpData));
        }
    }
    
    /**
     * 记录SQL执行日志
     */
    public void sqlExecution(String sql, long duration, int rowCount, boolean isSuccess) {
        if (!loggingConfig.getPerformance().isLogSqlTime()) {
            return;
        }
        
        Map<String, Object> sqlData = new HashMap<>();
        sqlData.put("sql", sanitizeSql(sql));
        sqlData.put("duration", duration);
        sqlData.put("rowCount", rowCount);
        sqlData.put("success", isSuccess);
        
        if (!isSuccess) {
            log.error("SQL_ERROR: " + formatStructuredLog(sqlData));
        } else if (duration > loggingConfig.getPerformance().getSlowQueryThreshold()) {
            log.warn("SLOW_SQL: " + formatStructuredLog(sqlData));
        } else {
            log.debug("SQL_EXECUTION: " + formatStructuredLog(sqlData));
        }
    }
    
    /**
     * 清理SQL语句中的敏感信息
     */
    private String sanitizeSql(String sql) {
        if (sql == null) {
            return null;
        }
        
        // 限制SQL长度
        if (sql.length() > 1000) {
            sql = sql.substring(0, 1000) + "...";
        }
        
        // 移除可能的敏感参数值
        return sql.replaceAll("'[^']*'", "'?'").replaceAll("\\?\\s*=\\s*[^\\s,)]+", "? = ?");
    }
}