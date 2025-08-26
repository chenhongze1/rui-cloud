package com.rui.common.idempotent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性配置属性
 * 
 * @author rui
 */
@Data
@ConfigurationProperties(prefix = "rui.idempotent")
public class IdempotentProperties {
    
    /**
     * 是否启用幂等性
     */
    private Boolean enabled = true;
    
    /**
     * Redis key前缀
     */
    private String keyPrefix = "idempotent";
    
    /**
     * 默认过期时间
     */
    private Long defaultExpireTime = 300L;
    
    /**
     * 默认时间单位
     */
    private TimeUnit defaultTimeUnit = TimeUnit.SECONDS;
    
    /**
     * 默认提示信息
     */
    private String defaultMessage = "请勿重复操作";
    
    /**
     * 是否默认删除key
     */
    private Boolean defaultDelKey = false;
    
    /**
     * 忽略幂等性的URL列表
     */
    private List<String> ignoreUrls = new ArrayList<>();
    
    /**
     * 全局幂等性配置
     */
    private GlobalConfig global = new GlobalConfig();
    
    /**
     * 全局配置
     */
    @Data
    public static class GlobalConfig {
        
        /**
         * 是否启用全局幂等性
         */
        private Boolean enabled = false;
        
        /**
         * 全局过期时间
         */
        private Long expireTime = 60L;
        
        /**
         * 全局时间单位
         */
        private TimeUnit timeUnit = TimeUnit.SECONDS;
        
        /**
         * 全局提示信息
         */
        private String message = "系统繁忙，请稍后再试";
        
        /**
         * 全局是否删除key
         */
        private Boolean delKey = true;
        
        /**
         * 包含的URL模式列表
         */
        private List<String> includePatterns = new ArrayList<>();
        
        /**
         * 排除的URL模式列表
         */
        private List<String> excludePatterns = new ArrayList<>();
    }
}