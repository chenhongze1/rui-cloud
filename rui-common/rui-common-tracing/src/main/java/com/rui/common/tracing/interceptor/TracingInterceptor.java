package com.rui.common.tracing.interceptor;

import com.rui.common.tracing.config.TracingProperties;
import com.rui.common.tracing.manager.TracingManager;
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

    private final TracingProperties tracingProperties;
    private final TracingManager tracingManager;
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    private static final String SPAN_ATTRIBUTE = "_tracing_span";
    private static final String SCOPE_ATTRIBUTE = "_tracing_scope";
    private static final String START_TIME_ATTRIBUTE = "_tracing_start_time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 检查是否应该忽略此请求
        if (shouldIgnoreRequest(request)) {
            return true;
        }

        try {
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            request.setAttribute(START_TIME_ATTRIBUTE, startTime);

            // 创建Span
            String operationName = buildOperationName(request);
            Span span = tracingManager.startSpan(operationName, SpanKind.SERVER);
            
            // 添加HTTP相关属性
            addHttpAttributes(span, request);
            
            // 添加自定义标签
            addCustomTags(span, request);
            
            // 激活Span
            Scope scope = span.makeCurrent();
            
            // 保存到请求属性中
            request.setAttribute(SPAN_ATTRIBUTE, span);
            request.setAttribute(SCOPE_ATTRIBUTE, scope);
            
            log.debug("Started tracing for request: {} {}", request.getMethod(), request.getRequestURI());
            
        } catch (Exception e) {
            log.error("Error starting trace for request", e);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        try {
            Span span = (Span) request.getAttribute(SPAN_ATTRIBUTE);
            Scope scope = (Scope) request.getAttribute(SCOPE_ATTRIBUTE);
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            
            if (span != null) {
                // 添加响应信息
                addResponseAttributes(span, response, startTime);
                
                // 处理异常
                if (ex != null) {
                    span.recordException(ex);
                    span.setStatus(StatusCode.ERROR, ex.getMessage());
                    span.setAttribute("error.type", ex.getClass().getSimpleName());
                    span.setAttribute("error.message", ex.getMessage());
                } else {
                    // 根据HTTP状态码设置状态
                    int statusCode = response.getStatus();
                    if (statusCode >= 400) {
                        span.setStatus(StatusCode.ERROR, "HTTP " + statusCode);
                    } else {
                        span.setStatus(StatusCode.OK);
                    }
                }
                
                // 结束Span
                tracingManager.finishSpan(span);
                
                log.debug("Finished tracing for request: {} {} - Status: {}", 
                         request.getMethod(), request.getRequestURI(), response.getStatus());
            }
            
            // 关闭Scope
            if (scope != null) {
                scope.close();
            }
            
        } catch (Exception e) {
            log.error("Error finishing trace for request", e);
        }
    }

    /**
     * 检查是否应该忽略此请求
     */
    private boolean shouldIgnoreRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // 检查配置的忽略模式
        for (String pattern : tracingProperties.getIgnorePatterns()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        
        // 默认忽略的路径
        String[] defaultIgnorePatterns = {
            "/health", "/metrics", "/actuator/**", "/favicon.ico",
            "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**"
        };
        
        for (String pattern : defaultIgnorePatterns) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 添加HTTP相关属性
     */
    private void addHttpAttributes(Span span, HttpServletRequest request) {
        // HTTP基本信息
        span.setAttribute("http.method", request.getMethod());
        span.setAttribute("http.url", request.getRequestURL().toString());
        span.setAttribute("http.scheme", request.getScheme());
        span.setAttribute("http.host", request.getServerName());
        span.setAttribute("http.target", request.getRequestURI());
        
        // 查询参数
        if (request.getQueryString() != null) {
            span.setAttribute("http.query", request.getQueryString());
        }
        
        // User-Agent
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            span.setAttribute("http.user_agent", userAgent);
        }
        
        // 客户端IP
        String clientIp = getClientIpAddress(request);
        if (clientIp != null) {
            span.setAttribute("http.client_ip", clientIp);
        }
        
        // Content-Type
        String contentType = request.getContentType();
        if (contentType != null) {
            span.setAttribute("http.request.content_type", contentType);
        }
        
        // Content-Length
        int contentLength = request.getContentLength();
        if (contentLength > 0) {
            span.setAttribute("http.request.content_length", contentLength);
        }
    }

    /**
     * 添加响应属性
     */
    private void addResponseAttributes(Span span, HttpServletResponse response, Long startTime) {
        // HTTP状态码
        span.setAttribute("http.status_code", response.getStatus());
        
        // 响应Content-Type
        String contentType = response.getContentType();
        if (contentType != null) {
            span.setAttribute("http.response.content_type", contentType);
        }
        
        // 计算请求处理时间
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            span.setAttribute("http.duration_ms", duration);
            
            // 标记慢请求
            if (duration > 1000) {
                span.setAttribute("http.slow_request", true);
            }
        }
    }

    /**
     * 添加自定义标签
     */
    private void addCustomTags(Span span, HttpServletRequest request) {
        // 添加配置的自定义标签
        tracingProperties.getTags().forEach(span::setAttribute);
        
        // 添加请求头中的追踪相关信息
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName.toLowerCase().startsWith("x-trace-") || 
                headerName.toLowerCase().startsWith("x-request-")) {
                String headerValue = request.getHeader(headerName);
                if (headerValue != null) {
                    span.setAttribute("http.header." + headerName.toLowerCase(), headerValue);
                }
            }
        }
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
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
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
     * 检查是否为静态资源请求
     */
    private boolean isStaticResource(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String[] staticExtensions = {".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg", ".woff", ".woff2", ".ttf"};
        
        for (String extension : staticExtensions) {
            if (uri.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 获取请求的业务标识
     */
    private String getBusinessIdentifier(HttpServletRequest request) {
        // 可以从请求头、参数或路径中提取业务标识
        String businessId = request.getHeader("X-Business-ID");
        if (businessId != null) {
            return businessId;
        }
        
        // 从路径中提取
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/")) {
            String[] parts = uri.split("/");
            if (parts.length > 3) {
                return parts[3]; // 业务模块名
            }
        }
        
        return "unknown";
    }
}