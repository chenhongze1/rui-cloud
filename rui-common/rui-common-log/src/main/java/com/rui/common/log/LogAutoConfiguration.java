package com.rui.common.log;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 日志自动配置类
 * 
 * @author rui
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "rui.log.enabled", havingValue = "true", matchIfMissing = true)
public class LogAutoConfiguration {
    
    /**
     * 注册请求追踪过滤器
     */
    @Bean
    @ConditionalOnProperty(name = "rui.log.trace.enabled", havingValue = "true", matchIfMissing = true)
    public TraceFilter traceFilter() {
        return new TraceFilter();
    }
    
    /**
     * 请求追踪过滤器
     */
    public static class TraceFilter extends OncePerRequestFilter {
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            try {
                // 生成追踪ID
                String traceId = LogUtil.generateAndSetTraceId();
                
                // 添加到响应头
                response.setHeader("X-Trace-Id", traceId);
                
                // 记录请求开始
                LogUtil.info(TraceFilter.class, "Request started: {} {}, TraceId: {}", 
                           request.getMethod(), request.getRequestURI(), traceId);
                
                long startTime = System.currentTimeMillis();
                
                filterChain.doFilter(request, response);
                
                // 记录请求结束
                long duration = System.currentTimeMillis() - startTime;
                LogUtil.info(TraceFilter.class, "Request completed: {} {}, Duration: {}ms, Status: {}", 
                           request.getMethod(), request.getRequestURI(), duration, response.getStatus());
                
            } finally {
                // 清除MDC
                LogUtil.clearMDC();
            }
        }
    }
}