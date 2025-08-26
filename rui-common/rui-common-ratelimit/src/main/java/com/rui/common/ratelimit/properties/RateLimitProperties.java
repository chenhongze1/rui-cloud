package com.rui.common.ratelimit.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 限流配置属性
 * 
 * @author rui
 */
@Data
@ConfigurationProperties(prefix = "rui.rate-limit")
public class RateLimitProperties {
    
    /**
     * 是否启用限流
     */
    private boolean enabled = true;
    
    /**
     * Redis key前缀
     */
    private String keyPrefix = "rate_limit";
    
    /**
     * 默认限流次数
     */
    private int defaultCount = 100;
    
    /**
     * 默认限流时间（秒）
     */
    private int defaultTime = 60;
    
    /**
     * 全局限流配置
     */
    private GlobalLimit global = new GlobalLimit();
    
    /**
     * IP限流配置
     */
    private IpLimit ip = new IpLimit();
    
    /**
     * 用户限流配置
     */
    private UserLimit user = new UserLimit();
    
    /**
     * 忽略限流的URL列表
     */
    private List<String> ignoreUrls = new ArrayList<>();
    
    /**
     * 全局限流配置
     */
    @Data
    public static class GlobalLimit {
        /**
         * 是否启用全局限流
         */
        private boolean enabled = false;
        
        /**
         * 全局限流次数
         */
        private int count = 1000;
        
        /**
         * 全局限流时间（秒）
         */
        private int time = 60;
    }
    
    /**
     * IP限流配置
     */
    @Data
    public static class IpLimit {
        /**
         * 是否启用IP限流
         */
        private boolean enabled = false;
        
        /**
         * IP限流次数
         */
        private int count = 100;
        
        /**
         * IP限流时间（秒）
         */
        private int time = 60;
        
        /**
         * IP白名单
         */
        private List<String> whitelist = new ArrayList<>();
    }
    
    /**
     * 用户限流配置
     */
    @Data
    public static class UserLimit {
        /**
         * 是否启用用户限流
         */
        private boolean enabled = false;
        
        /**
         * 用户限流次数
         */
        private int count = 200;
        
        /**
         * 用户限流时间（秒）
         */
        private int time = 60;
        
        /**
         * 用户白名单
         */
        private List<String> whitelist = new ArrayList<>();
    }
}