package com.rui.common.logging.config;

import com.rui.common.logging.LogManager;
import com.rui.common.logging.LoggingAspect;
import com.rui.common.logging.LoggingInterceptor;
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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 日志自动配置类
 * 自动配置日志相关组件
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
        return new LoggingInterceptor(logManager);
    }

    /**
     * 内容缓存过滤器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.logging.file", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<ContentCachingFilter> contentCachingFilter() {
        FilterRegistrationBean<ContentCachingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ContentCachingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * 请求日志过滤器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.logging.console", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);
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
                    .excludePathPatterns("/actuator/**", "/health", "/info");
        }
    }

    /**
     * 内容缓存过滤器实现
     */
    public static class ContentCachingFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            // 初始化
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;

                ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
                ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

                chain.doFilter(wrappedRequest, wrappedResponse);
                wrappedResponse.copyBodyToResponse();
            } else {
                chain.doFilter(request, response);
            }
        }

        @Override
        public void destroy() {
            // 销毁
        }
    }

    /**
     * 日志级别配置器
     */
    @Bean
    @ConditionalOnMissingBean
    public LogLevelConfigurer logLevelConfigurer() {
        return new LogLevelConfigurer(loggingConfig);
    }

    /**
     * 日志级别配置器实现
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
                // 配置根日志级别
                ch.qos.logback.classic.Logger rootLogger = 
                    (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                rootLogger.setLevel(ch.qos.logback.classic.Level.valueOf(loggingConfig.getLevel().getRoot()));

                // 配置包级别日志
                loggingConfig.getLevel().getPackages().forEach((packageName, level) -> {
                    ch.qos.logback.classic.Logger logger = 
                        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(packageName);
                    logger.setLevel(ch.qos.logback.classic.Level.valueOf(level));
                });

                // 配置SQL日志级别
                if (loggingConfig.getLevel().getSql() != null) {
                    ch.qos.logback.classic.Logger sqlLogger = 
                        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.springframework.jdbc");
                    sqlLogger.setLevel(ch.qos.logback.classic.Level.valueOf(loggingConfig.getLevel().getSql()));
                }

                // 配置HTTP日志级别
                if (loggingConfig.getLevel().getHttp() != null) {
                    ch.qos.logback.classic.Logger httpLogger = 
                        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.springframework.web");
                    httpLogger.setLevel(ch.qos.logback.classic.Level.valueOf(loggingConfig.getLevel().getHttp()));
                }

            } catch (Exception e) {
                log.warn("Failed to configure log levels: {}", e.getMessage());
            }
        }
    }

    /**
     * 异步日志配置器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.logging.async", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public AsyncLogConfigurer asyncLogConfigurer() {
        return new AsyncLogConfigurer(loggingConfig);
    }

    /**
     * 异步日志配置器实现
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
                
                ch.qos.logback.classic.AsyncAppender asyncAppender = new ch.qos.logback.classic.AsyncAppender();
                asyncAppender.setContext(context);
                asyncAppender.setName("ASYNC");
                asyncAppender.setQueueSize(loggingConfig.getAsync().getQueueSize());
                asyncAppender.setDiscardingThreshold(loggingConfig.getAsync().getDiscardingThreshold());
                asyncAppender.setMaxFlushTime(loggingConfig.getAsync().getMaxFlushTime());
                asyncAppender.setIncludeCallerData(loggingConfig.getAsync().isIncludeCallerData());
                
                // 添加到根日志记录器
                ch.qos.logback.classic.Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                rootLogger.addAppender(asyncAppender);
                asyncAppender.start();
                
                log.info("Async logging configured successfully");
            } catch (Exception e) {
                log.warn("Failed to configure async logging: {}", e.getMessage());
            }
        }
    }
}