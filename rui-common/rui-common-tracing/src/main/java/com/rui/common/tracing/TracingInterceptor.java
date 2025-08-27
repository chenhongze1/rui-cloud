package com.rui.common.tracing;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * 链路追踪拦截器
 * 用于在HTTP请求处理过程中添加链路追踪信息
 *
 * @author rui
 * @since 1.0.0
 */
@Component
public class TracingInterceptor implements HandlerInterceptor {
    
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取或生成trace ID
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }
        
        // 生成span ID
        String spanId = generateSpanId();
        
        // 设置到响应头
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(SPAN_ID_HEADER, spanId);
        
        // 设置到请求属性
        request.setAttribute("traceId", traceId);
        request.setAttribute("spanId", spanId);
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 记录请求处理完成时间
        long endTime = System.currentTimeMillis();
        request.setAttribute("endTime", endTime);
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理线程本地变量
        TracingContext.clear();
    }
    
    /**
     * 生成trace ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成span ID
     */
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}