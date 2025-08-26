package com.rui.common.feign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Feign配置属性
 * 
 * @author rui
 */
@Data
@ConfigurationProperties(prefix = "rui.feign")
public class FeignProperties {
    
    /**
     * 是否启用Feign
     */
    private Boolean enabled = true;
    
    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectTimeout = 5000;
    
    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout = 10000;
    
    /**
     * 是否启用GZIP压缩
     */
    private Boolean compression = true;
    
    /**
     * 是否启用请求日志
     */
    private Boolean requestLogging = true;
    
    /**
     * 日志级别
     */
    private String logLevel = "BASIC";
    
    /**
     * 是否启用重试
     */
    private Boolean retry = true;
    
    /**
     * 重试配置
     */
    private RetryConfig retryConfig = new RetryConfig();
    
    /**
     * 熔断配置
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    
    /**
     * 默认请求头
     */
    private Map<String, String> defaultHeaders = new HashMap<>();
    
    /**
     * 重试配置
     */
    @Data
    public static class RetryConfig {
        
        /**
         * 最大重试次数
         */
        private Integer maxAttempts = 3;
        
        /**
         * 重试间隔（毫秒）
         */
        private Long period = 1000L;
        
        /**
         * 最大重试间隔（毫秒）
         */
        private Long maxPeriod = 5000L;
    }
    
    /**
     * 熔断配置
     */
    @Data
    public static class CircuitBreakerConfig {
        
        /**
         * 是否启用熔断
         */
        private Boolean enabled = false;
        
        /**
         * 失败率阈值
         */
        private Float failureRateThreshold = 50.0f;
        
        /**
         * 慢调用率阈值
         */
        private Float slowCallRateThreshold = 100.0f;
        
        /**
         * 慢调用时间阈值（毫秒）
         */
        private Long slowCallDurationThreshold = 60000L;
        
        /**
         * 最小调用次数
         */
        private Integer minimumNumberOfCalls = 10;
        
        /**
         * 滑动窗口大小
         */
        private Integer slidingWindowSize = 100;
        
        /**
         * 等待时间（毫秒）
         */
        private Long waitDurationInOpenState = 60000L;
    }
}