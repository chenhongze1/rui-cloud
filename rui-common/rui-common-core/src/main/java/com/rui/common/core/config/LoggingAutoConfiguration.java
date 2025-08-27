package com.rui.common.core.config;

import com.rui.common.core.logging.LogManager;
import com.rui.common.core.logging.LoggingAspect;
import com.rui.common.core.logging.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 日志自动配置类
 * 整合所有日志相关组件
 *
 * @author rui
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(LoggingConfig.class)
@ConditionalOnProperty(prefix = "rui.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

    private final LoggingConfig loggingConfig;

    /**
     * 日志管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public LogManager logManager() {
        return new LogManager(loggingConfig);
    }

    /**
     * 日志切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public LoggingAspect loggingAspect(LogManager logManager) {
        return new LoggingAspect(logManager);
    }

    /**
     * 日志拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.web.servlet.HandlerInterceptor")
    public LoggingInterceptor loggingInterceptor(LogManager logManager) {
        return new LoggingInterceptor(loggingConfig, logManager);
    }

    /**
     * 请求响应缓存过滤器
     * 用于缓存请求和响应内容，以便在日志中记录
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.logging.file", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<ContentCachingFilter> contentCachingFilter() {
        FilterRegistrationBean<ContentCachingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ContentCachingFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        registrationBean.setName("contentCachingFilter");
        return registrationBean;
    }

    /**
     * Spring Boot内置的请求日志过滤器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.logging.console", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(1000);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("REQUEST DATA : ");
        return filter;
    }

    /**
     * Web MVC配置
     */
    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")
    static class LoggingWebMvcConfiguration implements WebMvcConfigurer {
        
        private final LoggingInterceptor loggingInterceptor;
        
        public LoggingWebMvcConfiguration(LoggingInterceptor loggingInterceptor) {
            this.loggingInterceptor = loggingInterceptor;
        }
        
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/health", "/metrics", "/favicon.ico", "/static/**");
        }
    }

    /**
     * 内容缓存过滤器
     * 用于缓存HTTP请求和响应的内容，以便在日志中记录
     */
    public static class ContentCachingFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            // 初始化逻辑
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // 包装请求和响应以缓存内容
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);
            
            try {
                chain.doFilter(wrappedRequest, wrappedResponse);
            } finally {
                // 复制响应内容到原始响应
                wrappedResponse.copyBodyToResponse();
            }
        }

        @Override
        public void destroy() {
            // 清理逻辑
        }
    }

    /**
     * 日志级别配置
     */
    @Bean
    @ConditionalOnMissingBean
    public LogLevelConfigurer logLevelConfigurer() {
        return new LogLevelConfigurer(loggingConfig);
    }

    /**
     * 日志级别配置器
     */
    public static class LogLevelConfigurer {
        
        private final LoggingConfig loggingConfig;
        
        public LogLevelConfigurer(LoggingConfig loggingConfig) {
            this.loggingConfig = loggingConfig;
            configureLogLevels();
        }
        
        /**
         * 配置日志级别
         */
        private void configureLogLevels() {
            try {
                // 设置根日志级别
                ch.qos.logback.classic.Logger rootLogger = 
                    (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                rootLogger.setLevel(ch.qos.logback.classic.Level.valueOf(loggingConfig.getLevel().getRoot()));
                
                // 设置包级别日志
                loggingConfig.getLevel().getPackages().forEach((packageName, level) -> {
                    ch.qos.logback.classic.Logger packageLogger = 
                        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(packageName);
                    packageLogger.setLevel(ch.qos.logback.classic.Level.valueOf(level));
                });
                
                // 设置SQL日志级别
                if (loggingConfig.getLevel().getSql() != null) {
                    ch.qos.logback.classic.Logger sqlLogger = 
                        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.springframework.jdbc");
                    sqlLogger.setLevel(ch.qos.logback.classic.Level.valueOf(loggingConfig.getLevel().getSql()));
                    
                    // MyBatis SQL日志
                    ch.qos.logback.classic.Logger mybatisLogger = 
                        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("com.rui.**.mapper");
                    mybatisLogger.setLevel(ch.qos.logback.classic.Level.valueOf(loggingConfig.getLevel().getSql()));
                }
                
                // 设置HTTP日志级别
                if (loggingConfig.getLevel().getHttp() != null) {
                    ch.qos.logback.classic.Logger httpLogger = 
                        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.springframework.web");
                    httpLogger.setLevel(ch.qos.logback.classic.Level.valueOf(loggingConfig.getLevel().getHttp()));
                }
                
                // 设置业务日志级别
                if (loggingConfig.getLevel().getBusiness() != null) {
                    ch.qos.logback.classic.Logger businessLogger = 
                        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("BUSINESS");
                    businessLogger.setLevel(ch.qos.logback.classic.Level.valueOf(loggingConfig.getLevel().getBusiness()));
                }
                
                log.info("Log levels configured successfully");
                
            } catch (Exception e) {
                log.warn("Failed to configure log levels", e);
            }
        }
    }

    /**
     * 异步日志配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.logging.async", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public AsyncLogConfigurer asyncLogConfigurer() {
        return new AsyncLogConfigurer(loggingConfig);
    }

    /**
     * 异步日志配置器
     */
    public static class AsyncLogConfigurer {
        
        private final LoggingConfig loggingConfig;
        
        public AsyncLogConfigurer(LoggingConfig loggingConfig) {
            this.loggingConfig = loggingConfig;
            configureAsyncLogging();
        }
        
        /**
         * 配置异步日志
         */
        private void configureAsyncLogging() {
            try {
                ch.qos.logback.classic.LoggerContext context = 
                    (ch.qos.logback.classic.LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
                
                // 创建异步Appender
                ch.qos.logback.classic.AsyncAppender asyncAppender = new ch.qos.logback.classic.AsyncAppender();
                asyncAppender.setContext(context);
                asyncAppender.setName("ASYNC");
                asyncAppender.setQueueSize(loggingConfig.getAsync().getQueueSize());
                asyncAppender.setDiscardingThreshold(loggingConfig.getAsync().getDiscardingThreshold());
                asyncAppender.setMaxFlushTime(loggingConfig.getAsync().getMaxFlushTime());
                asyncAppender.setIncludeCallerData(loggingConfig.getAsync().isIncludeCallerData());
                
                // 获取现有的文件Appender并添加到异步Appender
                ch.qos.logback.classic.Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                rootLogger.iteratorForAppenders().forEachRemaining(appender -> {
                    if (appender instanceof ch.qos.logback.core.FileAppender) {
                        asyncAppender.addAppender(appender);
                        rootLogger.detachAppender(appender);
                    }
                });
                
                asyncAppender.start();
                rootLogger.addAppender(asyncAppender);
                
                log.info("Async logging configured successfully");
                
            } catch (Exception e) {
                log.warn("Failed to configure async logging", e);
            }
        }
    }
}