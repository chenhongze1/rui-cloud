package com.rui.common.logging.interceptor;

import com.rui.common.logging.autoconfigure.LoggingConfig;
import com.rui.common.logging.manager.LogManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 日志拦截器
 * 自动记录HTTP请求和响应日志
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rui.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingInterceptor implements HandlerInterceptor {

    private final LoggingConfig loggingConfig;
    private final LogManager logManager;
    
    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final String REQUEST_ID_ATTRIBUTE = "requestId";
    
    // 忽略的URL模式
    private static final List<String> IGNORED_PATHS = Arrays.asList(
        "/actuator", "/health", "/metrics", "/favicon.ico", "/static", "/css", "/js", "/images"
    );
    
    // 敏感的请求头
    private static final List<String> SENSITIVE_HEADERS = Arrays.asList(
        "authorization", "cookie", "set-cookie", "x-auth-token", "x-api-key"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查是否需要忽略
        if (shouldIgnore(request)) {
            return true;
        }
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);
        
        // 生成请求ID
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        
        // 设置日志上下文
        setupLoggingContext(request, requestId);
        
        // 记录请求开始日志
        logRequestStart(request);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 检查是否需要忽略
        if (shouldIgnore(request)) {
            return;
        }
        
        try {
            // 计算执行时间
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
            
            // 记录请求完成日志
            logRequestCompletion(request, response, duration, ex);
            
            // 记录HTTP请求日志
            logManager.httpRequest(
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration,
                request.getHeader("User-Agent"),
                getClientIpAddress(request)
            );
            
        } finally {
            // 清理日志上下文
            logManager.clearContext();
        }
    }

    /**
     * 检查是否应该忽略该请求
     */
    private boolean shouldIgnore(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return IGNORED_PATHS.stream().anyMatch(uri::startsWith);
    }

    /**
     * 设置日志上下文
     */
    private void setupLoggingContext(HttpServletRequest request, String requestId) {
        // 设置请求上下文
        logManager.setRequestContext(
            extractUserId(request),
            request.getSession(false) != null ? request.getSession().getId() : null,
            getClientIpAddress(request),
            request.getHeader("User-Agent")
        );
        
        // 设置请求ID
        logManager.setContext(LogManager.REQUEST_ID, requestId);
        
        // 从请求头中提取追踪信息
        String traceId = request.getHeader("X-Trace-Id");
        String spanId = request.getHeader("X-Span-Id");
        if (StringUtils.hasText(traceId)) {
            logManager.setTraceContext(traceId, spanId);
        }
    }

    /**
     * 记录请求开始日志
     */
    private void logRequestStart(HttpServletRequest request) {
        if (!loggingConfig.getLevel().getHttp().equals("DEBUG")) {
            return;
        }
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("method", request.getMethod());
        requestData.put("uri", request.getRequestURI());
        requestData.put("queryString", request.getQueryString());
        requestData.put("remoteAddr", getClientIpAddress(request));
        requestData.put("userAgent", request.getHeader("User-Agent"));
        requestData.put("contentType", request.getContentType());
        requestData.put("contentLength", request.getContentLength());
        
        // 记录请求头（过滤敏感信息）
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!isSensitiveHeader(headerName)) {
                headers.put(headerName, request.getHeader(headerName));
            } else {
                headers.put(headerName, "[HIDDEN]");
            }
        }
        requestData.put("headers", headers);
        
        // 记录请求参数
        if (!request.getParameterMap().isEmpty()) {
            Map<String, String[]> parameters = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (!isSensitiveParameter(key)) {
                    parameters.put(key, values);
                } else {
                    parameters.put(key, new String[]{"[HIDDEN]"});
                }
            });
            requestData.put("parameters", parameters);
        }
        
        // 记录请求体（如果是POST/PUT等）
        if (shouldLogRequestBody(request)) {
            String requestBody = getRequestBody(request);
            if (StringUtils.hasText(requestBody)) {
                requestData.put("body", sanitizeRequestBody(requestBody));
            }
        }
        
        log.debug("HTTP_REQUEST_START: {}", requestData);
    }

    /**
     * 记录请求完成日志
     */
    private void logRequestCompletion(HttpServletRequest request, HttpServletResponse response, long duration, Exception ex) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("method", request.getMethod());
        responseData.put("uri", request.getRequestURI());
        responseData.put("status", response.getStatus());
        responseData.put("duration", duration);
        responseData.put("contentType", response.getContentType());
        
        // 记录响应头（过滤敏感信息）
        Map<String, String> responseHeaders = new HashMap<>();
        for (String headerName : response.getHeaderNames()) {
            if (!isSensitiveHeader(headerName)) {
                responseHeaders.put(headerName, response.getHeader(headerName));
            } else {
                responseHeaders.put(headerName, "[HIDDEN]");
            }
        }
        responseData.put("headers", responseHeaders);
        
        // 记录异常信息
        if (ex != null) {
            responseData.put("exception", ex.getClass().getSimpleName());
            responseData.put("exceptionMessage", ex.getMessage());
        }
        
        // 记录响应体（如果需要）
        if (shouldLogResponseBody(request, response)) {
            String responseBody = getResponseBody(response);
            if (StringUtils.hasText(responseBody)) {
                responseData.put("body", sanitizeResponseBody(responseBody));
            }
        }
        
        // 根据状态码选择日志级别
        if (response.getStatus() >= 500) {
            log.error("HTTP_REQUEST_ERROR: {}", responseData);
        } else if (response.getStatus() >= 400) {
            log.warn("HTTP_REQUEST_CLIENT_ERROR: {}", responseData);
        } else if (duration > loggingConfig.getPerformance().getSlowApiThreshold()) {
            log.warn("HTTP_REQUEST_SLOW: {}", responseData);
        } else {
            log.info("HTTP_REQUEST_COMPLETED: {}", responseData);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
            "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 取第一个IP地址
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 提取用户ID
     */
    private String extractUserId(HttpServletRequest request) {
        // 从JWT token中提取用户ID
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            // 这里可以解析JWT token获取用户ID
            // 暂时返回null，实际实现需要根据具体的JWT解析逻辑
            return null;
        }
        
        // 从session中获取用户ID
        if (request.getSession(false) != null) {
            Object userId = request.getSession().getAttribute("userId");
            return userId != null ? userId.toString() : null;
        }
        
        return null;
    }

    /**
     * 检查是否为敏感请求头
     */
    private boolean isSensitiveHeader(String headerName) {
        return SENSITIVE_HEADERS.stream()
            .anyMatch(sensitive -> sensitive.equalsIgnoreCase(headerName));
    }

    /**
     * 检查是否为敏感参数
     */
    private boolean isSensitiveParameter(String paramName) {
        String lowerName = paramName.toLowerCase();
        return lowerName.contains("password") || lowerName.contains("token") ||
               lowerName.contains("secret") || lowerName.contains("key");
    }

    /**
     * 是否应该记录请求体
     */
    private boolean shouldLogRequestBody(HttpServletRequest request) {
        String method = request.getMethod();
        String contentType = request.getContentType();
        
        return ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) &&
               contentType != null &&
               (contentType.contains("application/json") || contentType.contains("application/xml") ||
                contentType.contains("text/"));
    }

    /**
     * 是否应该记录响应体
     */
    private boolean shouldLogResponseBody(HttpServletRequest request, HttpServletResponse response) {
        String contentType = response.getContentType();
        return contentType != null &&
               (contentType.contains("application/json") || contentType.contains("application/xml") ||
                contentType.contains("text/")) &&
               response.getStatus() < 300;
    }

    /**
     * 获取请求体内容
     */
    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * 获取响应体内容
     */
    private String getResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * 清理请求体中的敏感信息
     */
    private String sanitizeRequestBody(String body) {
        if (body == null) {
            return null;
        }
        
        // 限制长度
        if (body.length() > 2000) {
            body = body.substring(0, 2000) + "...";
        }
        
        // 替换敏感字段
        return body.replaceAll("(\"password\"\s*:\s*\")[^\"]*(\")?", "$1[HIDDEN]$2")
                  .replaceAll("(\"token\"\s*:\s*\")[^\"]*(\")?", "$1[HIDDEN]$2")
                  .replaceAll("(\"secret\"\s*:\s*\")[^\"]*(\")?", "$1[HIDDEN]$2");
    }

    /**
     * 清理响应体中的敏感信息
     */
    private String sanitizeResponseBody(String body) {
        if (body == null) {
            return null;
        }
        
        // 限制长度
        if (body.length() > 2000) {
            body = body.substring(0, 2000) + "...";
        }
        
        // 替换敏感字段
        return body.replaceAll("(\"token\"\s*:\s*\")[^\"]*(\")?", "$1[HIDDEN]$2")
                  .replaceAll("(\"secret\"\s*:\s*\")[^\"]*(\")?", "$1[HIDDEN]$2");
    }
}