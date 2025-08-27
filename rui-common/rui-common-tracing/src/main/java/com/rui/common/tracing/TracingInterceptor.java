package com.rui.common.tracing;

import com.rui.common.core.config.TracingConfig;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Enumeration;

/**
 * 链路追踪拦截器
 * 自动为HTTP请求添加追踪信息
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TracingInterceptor implements HandlerInterceptor {

    private final TracingConfig tracingConfig;
    private final TracingManager tracingManager;
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    private static final String SPAN_ATTRIBUTE = "_tracing_span";
    private static final String SCOPE_ATTRIBUTE = "_tracing_scope";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!tracingConfig.isEnabled()) {
            return true;
        }
        
        // 检查是否需要忽略此请求
        if (shouldIgnoreRequest(request)) {
            log.debug("忽略追踪请求: {}", request.getRequestURI());
            return true;
        }
        
        try {
            // 创建HTTP服务端Span
            String operationName = buildOperationName(request);
            Span span = tracingManager.createSpan(operationName, SpanKind.SERVER);
            
            // 添加HTTP相关标签
            addHttpTags(span, request);
            
            // 激活Span
            Scope scope = span.makeCurrent();
            
            // 保存到请求属性中
            request.setAttribute(SPAN_ATTRIBUTE, span);
            request.setAttribute(SCOPE_ATTRIBUTE, scope);
            
            log.debug("开始追踪HTTP请求: uri={}, traceId={}, spanId={}", 
                request.getRequestURI(), 
                tracingManager.getCurrentTraceId(), 
                tracingManager.getCurrentSpanId());
            
        } catch (Exception e) {
            log.error("创建HTTP追踪失败", e);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (!tracingConfig.isEnabled()) {
            return;
        }
        
        try {
            Span span = (Span) request.getAttribute(SPAN_ATTRIBUTE);
            Scope scope = (Scope) request.getAttribute(SCOPE_ATTRIBUTE);
            
            if (span != null) {
                // 添加响应信息
                addResponseTags(span, response);
                
                // 处理异常
                if (ex != null) {
                    tracingManager.recordException(span, ex);
                } else {
                    // 根据HTTP状态码设置Span状态
                    setSpanStatusFromHttpCode(span, response.getStatus());
                }
                
                // 完成Span
                tracingManager.finishSpan(span);
                
                log.debug("完成HTTP请求追踪: uri={}, status={}", 
                    request.getRequestURI(), response.getStatus());
            }
            
            // 关闭Scope
            if (scope != null) {
                scope.close();
            }
            
        } catch (Exception e) {
            log.error("完成HTTP追踪失败", e);
        }
    }

    /**
     * 检查是否应该忽略此请求
     */
    private boolean shouldIgnoreRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // 检查忽略模式
        for (String pattern : tracingConfig.getIgnorePatterns()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        
        // 默认忽略的路径
        return uri.startsWith("/actuator") || 
               uri.startsWith("/health") || 
               uri.startsWith("/metrics") ||
               uri.startsWith("/favicon.ico") ||
               uri.startsWith("/static") ||
               uri.startsWith("/css") ||
               uri.startsWith("/js") ||
               uri.startsWith("/images");
    }

    /**
     * 构建操作名称
     */
    private String buildOperationName(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // 简化URI，移除路径参数
        String simplifiedUri = simplifyUri(uri);
        
        return method + " " + simplifiedUri;
    }

    /**
     * 简化URI，将路径参数替换为占位符
     */
    private String simplifyUri(String uri) {
        // 这里可以实现更复杂的URI简化逻辑
        // 例如：/user/123 -> /user/{id}
        return uri.replaceAll("/\\d+", "/{id}")
                 .replaceAll("/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "/{uuid}");
    }

    /**
     * 添加HTTP请求标签
     */
    private void addHttpTags(Span span, HttpServletRequest request) {
        // HTTP基本信息
        tracingManager.addSpanTag(span, "http.method", request.getMethod());
        tracingManager.addSpanTag(span, "http.url", request.getRequestURL().toString());
        tracingManager.addSpanTag(span, "http.scheme", request.getScheme());
        tracingManager.addSpanTag(span, "http.host", request.getServerName());
        tracingManager.addSpanTag(span, "http.target", request.getRequestURI());
        
        // 客户端信息
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            tracingManager.addSpanTag(span, "http.user_agent", userAgent);
        }
        
        String clientIp = getClientIpAddress(request);
        if (clientIp != null) {
            tracingManager.addSpanTag(span, "http.client_ip", clientIp);
        }
        
        // 请求头信息（选择性添加）
        addSelectedHeaders(span, request);
        
        // 查询参数
        String queryString = request.getQueryString();
        if (queryString != null) {
            tracingManager.addSpanTag(span, "http.query_string", queryString);
        }
    }

    /**
     * 添加HTTP响应标签
     */
    private void addResponseTags(Span span, HttpServletResponse response) {
        tracingManager.addSpanTag(span, "http.status_code", response.getStatus());
        
        // 响应头信息（选择性添加）
        String contentType = response.getContentType();
        if (contentType != null) {
            tracingManager.addSpanTag(span, "http.response.content_type", contentType);
        }
    }

    /**
     * 添加选择性的请求头
     */
    private void addSelectedHeaders(Span span, HttpServletRequest request) {
        // 只添加重要的请求头，避免敏感信息
        String[] importantHeaders = {
            "Content-Type", "Accept", "Accept-Language", 
            "Accept-Encoding", "Cache-Control", "Connection"
        };
        
        for (String headerName : importantHeaders) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null) {
                tracingManager.addSpanTag(span, "http.request.header." + headerName.toLowerCase(), headerValue);
            }
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", 
            "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 根据HTTP状态码设置Span状态
     */
    private void setSpanStatusFromHttpCode(Span span, int httpStatus) {
        if (httpStatus >= 400) {
            tracingManager.setSpanStatus(span, StatusCode.ERROR, "HTTP " + httpStatus);
        } else {
            tracingManager.setSpanStatus(span, StatusCode.OK, null);
        }
    }
}