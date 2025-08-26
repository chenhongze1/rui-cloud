package com.rui.common.log.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rui.common.core.context.SecurityContextHolder;
import com.rui.common.core.utils.IpUtils;
import com.rui.common.log.config.LogProperties;
import com.rui.common.log.entity.LogInfo;
import com.rui.common.log.service.LogService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 访问日志过滤器
 * 
 * @author rui
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AccessLogFilter implements Filter {
    
    private final LogService logService;
    private final LogProperties logProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!logProperties.getEnabled() || !logProperties.getAccessLog().getEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 检查是否需要忽略
        if (shouldIgnore(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        
        // 包装请求和响应以便读取内容
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行请求
            chain.doFilter(requestWrapper, responseWrapper);
            
            // 记录访问日志
            recordAccessLog(requestWrapper, responseWrapper, startTime, null);
            
        } catch (Exception e) {
            // 记录异常访问日志
            recordAccessLog(requestWrapper, responseWrapper, startTime, e);
            throw e;
        } finally {
            // 复制响应内容
            responseWrapper.copyBodyToResponse();
        }
    }
    
    /**
     * 记录访问日志
     */
    private void recordAccessLog(ContentCachingRequestWrapper request, 
                                ContentCachingResponseWrapper response, 
                                long startTime, Exception exception) {
        try {
            LogInfo logInfo = buildAccessLogInfo(request, response, startTime, exception);
            logService.saveAccessLog(logInfo);
        } catch (Exception e) {
            log.error("记录访问日志失败", e);
        }
    }
    
    /**
     * 构建访问日志信息
     */
    private LogInfo buildAccessLogInfo(ContentCachingRequestWrapper request, 
                                      ContentCachingResponseWrapper response, 
                                      long startTime, Exception exception) {
        LogInfo logInfo = new LogInfo();
        
        // 基础信息
        logInfo.setLogId(UUID.randomUUID().toString().replace("-", ""))
                .setLogType("ACCESS")
                .setCreateTime(LocalDateTime.now())
                .setExecutionTime(System.currentTimeMillis() - startTime)
                .setThreadName(Thread.currentThread().getName());
        
        // 请求信息
        logInfo.setRequestUri(request.getRequestURI())
                .setRequestMethod(request.getMethod())
                .setClientIp(IpUtils.getIpAddr(request))
                .setUserAgent(request.getHeader("User-Agent"));
        
        // 响应信息
        logInfo.setResponseStatus(response.getStatus());
        
        // 用户信息
        try {
            logInfo.setUserId(SecurityContextHolder.getUserId())
                    .setUsername(SecurityContextHolder.getUsername())
                    .setTenantId(SecurityContextHolder.getTenantId());
        } catch (Exception e) {
            log.debug("获取用户信息失败: {}", e.getMessage());
        }
        
        // 链路追踪ID
        String traceId = request.getHeader("X-Trace-Id");
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        logInfo.setTraceId(traceId);
        
        // 记录请求头
        if (logProperties.getAccessLog().getIncludeHeaders()) {
            recordRequestHeaders(request, logInfo);
        }
        
        // 记录请求体
        if (logProperties.getAccessLog().getIncludePayload()) {
            recordRequestPayload(request, logInfo);
        }
        
        // 记录响应体
        if (logProperties.getAccessLog().getIncludeResponse()) {
            recordResponsePayload(response, logInfo);
        }
        
        // 记录异常信息
        if (exception != null) {
            logInfo.setExceptionMessage(exception.getMessage());
            
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : exception.getStackTrace()) {
                stackTrace.append(element.toString()).append("\n");
                if (stackTrace.length() > 2000) {
                    stackTrace.append("...");
                    break;
                }
            }
            logInfo.setExceptionStackTrace(stackTrace.toString());
        }
        
        return logInfo;
    }
    
    /**
     * 记录请求头
     */
    private void recordRequestHeaders(HttpServletRequest request, LogInfo logInfo) {
        try {
            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // 过滤敏感头信息
                if (!isSensitiveHeader(headerName)) {
                    headers.put(headerName, request.getHeader(headerName));
                }
            }
            logInfo.setRequestHeaders(objectMapper.writeValueAsString(headers));
        } catch (Exception e) {
            log.warn("记录请求头失败: {}", e.getMessage());
        }
    }
    
    /**
     * 记录请求体
     */
    private void recordRequestPayload(ContentCachingRequestWrapper request, LogInfo logInfo) {
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String payload = new String(content, StandardCharsets.UTF_8);
                if (payload.length() > logProperties.getAccessLog().getMaxPayloadLength()) {
                    payload = payload.substring(0, logProperties.getAccessLog().getMaxPayloadLength()) + "...";
                }
                logInfo.setRequestBody(payload);
            }
        } catch (Exception e) {
            log.warn("记录请求体失败: {}", e.getMessage());
        }
    }
    
    /**
     * 记录响应体
     */
    private void recordResponsePayload(ContentCachingResponseWrapper response, LogInfo logInfo) {
        try {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                String payload = new String(content, StandardCharsets.UTF_8);
                if (payload.length() > logProperties.getAccessLog().getMaxResponseLength()) {
                    payload = payload.substring(0, logProperties.getAccessLog().getMaxResponseLength()) + "...";
                }
                logInfo.setResponseResult(payload);
            }
        } catch (Exception e) {
            log.warn("记录响应体失败: {}", e.getMessage());
        }
    }
    
    /**
     * 检查是否应该忽略该URL
     */
    private boolean shouldIgnore(String uri) {
        String[] ignoreUrls = logProperties.getAccessLog().getIgnoreUrls();
        if (ignoreUrls != null) {
            return Arrays.stream(ignoreUrls)
                    .anyMatch(pattern -> pathMatcher.match(pattern, uri));
        }
        return false;
    }
    
    /**
     * 检查是否为敏感头信息
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.contains("authorization") || 
               lowerName.contains("cookie") || 
               lowerName.contains("token") ||
               lowerName.contains("password");
    }
}