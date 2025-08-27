package com.rui.common.feign.interceptor;

import com.rui.common.core.constant.SecurityConstants;
import com.rui.common.core.utils.ServletUtils;
import com.rui.common.core.utils.StringUtils;
import com.rui.common.feign.config.FeignProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;

/**
 * Feign请求拦截器
 * 
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeignRequestInterceptor implements RequestInterceptor {
    
    private final FeignProperties feignProperties;
    
    @Override
    public void apply(RequestTemplate template) {
        // 添加默认请求头
        addDefaultHeaders(template);
        
        // 传递当前请求的认证信息
        addAuthenticationHeaders(template);
        
        // 传递链路追踪信息
        addTraceHeaders(template);
        
        // 传递租户信息
        addTenantHeaders(template);
        
        // 记录请求日志
        logRequest(template);
    }
    
    /**
     * 添加默认请求头
     */
    private void addDefaultHeaders(RequestTemplate template) {
        Map<String, String> defaultHeaders = feignProperties.getDefaultHeaders();
        if (defaultHeaders != null && !defaultHeaders.isEmpty()) {
            defaultHeaders.forEach(template::header);
        }
        
        // 添加内部调用标识
        template.header(SecurityConstants.FROM_SOURCE, SecurityConstants.INNER);
    }
    
    /**
     * 添加认证信息
     */
    private void addAuthenticationHeaders(RequestTemplate template) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return;
        }
        
        // 传递Authorization头
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(authorization)) {
            template.header("Authorization", authorization);
        }
        
        // 传递用户信息
        String userId = request.getHeader(SecurityConstants.USER_ID_HEADER);
        if (StringUtils.isNotBlank(userId)) {
            template.header(SecurityConstants.USER_ID_HEADER, userId);
        }
        
        String username = request.getHeader(SecurityConstants.USER_NAME_HEADER);
        if (StringUtils.isNotBlank(username)) {
            template.header(SecurityConstants.USER_NAME_HEADER, username);
        }
        
        String userKey = request.getHeader(SecurityConstants.USER_KEY);
        if (StringUtils.isNotBlank(userKey)) {
            template.header(SecurityConstants.USER_KEY, userKey);
        }
    }
    
    /**
     * 添加链路追踪信息
     */
    private void addTraceHeaders(RequestTemplate template) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return;
        }
        
        // 传递链路追踪ID
        String traceId = request.getHeader("X-Trace-Id");
        if (StringUtils.isNotBlank(traceId)) {
            template.header("X-Trace-Id", traceId);
        }
        
        String spanId = request.getHeader("X-Span-Id");
        if (StringUtils.isNotBlank(spanId)) {
            template.header("X-Span-Id", spanId);
        }
    }
    
    /**
     * 添加租户信息
     */
    private void addTenantHeaders(RequestTemplate template) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return;
        }
        
        // 传递租户ID
        String tenantId = request.getHeader("X-Tenant-Id");
        if (StringUtils.isNotBlank(tenantId)) {
            template.header("X-Tenant-Id", tenantId);
        }
    }
    
    /**
     * 记录请求日志
     */
    private void logRequest(RequestTemplate template) {
        if (feignProperties.getRequestLogging()) {
            log.debug("Feign请求: {} {}", template.method(), template.url());
            if (log.isTraceEnabled()) {
                log.trace("Feign请求头: {}", template.headers());
                if (template.body() != null) {
                    log.trace("Feign请求体: {}", new String(template.body()));
                }
            }
        }
    }
}