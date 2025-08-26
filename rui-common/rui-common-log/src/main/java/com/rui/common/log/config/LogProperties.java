package com.rui.common.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志配置属性
 * 
 * @author rui
 */
@Data
@ConfigurationProperties(prefix = "rui.log")
public class LogProperties {
    
    /**
     * 是否启用日志模块
     */
    private Boolean enabled = true;
    
    /**
     * 操作日志配置
     */
    private OperationLogConfig operationLog = new OperationLogConfig();
    
    /**
     * 访问日志配置
     */
    private AccessLogConfig accessLog = new AccessLogConfig();
    
    /**
     * 错误日志配置
     */
    private ErrorLogConfig errorLog = new ErrorLogConfig();
    
    /**
     * ELK配置
     */
    private ElkConfig elk = new ElkConfig();
    
    /**
     * 操作日志配置
     */
    @Data
    public static class OperationLogConfig {
        
        /**
         * 是否启用操作日志
         */
        private Boolean enabled = true;
        
        /**
         * 是否记录请求参数
         */
        private Boolean includeArgs = true;
        
        /**
         * 是否记录返回结果
         */
        private Boolean includeResult = true;
        
        /**
         * 是否记录异常信息
         */
        private Boolean includeException = true;
        
        /**
         * 参数最大长度
         */
        private Integer maxArgsLength = 2000;
        
        /**
         * 结果最大长度
         */
        private Integer maxResultLength = 2000;
        
        /**
         * 忽略的URL列表
         */
        private String[] ignoreUrls = {"/actuator/**", "/swagger-ui/**", "/v3/api-docs/**"};
    }
    
    /**
     * 访问日志配置
     */
    @Data
    public static class AccessLogConfig {
        
        /**
         * 是否启用访问日志
         */
        private Boolean enabled = true;
        
        /**
         * 是否记录请求头
         */
        private Boolean includeHeaders = false;
        
        /**
         * 是否记录请求体
         */
        private Boolean includePayload = true;
        
        /**
         * 是否记录响应体
         */
        private Boolean includeResponse = false;
        
        /**
         * 请求体最大长度
         */
        private Integer maxPayloadLength = 1000;
        
        /**
         * 响应体最大长度
         */
        private Integer maxResponseLength = 1000;
        
        /**
         * 忽略的URL列表
         */
        private String[] ignoreUrls = {"/actuator/**", "/swagger-ui/**", "/v3/api-docs/**"};
    }
    
    /**
     * 错误日志配置
     */
    @Data
    public static class ErrorLogConfig {
        
        /**
         * 是否启用错误日志
         */
        private Boolean enabled = true;
        
        /**
         * 是否记录堆栈信息
         */
        private Boolean includeStackTrace = true;
        
        /**
         * 堆栈信息最大长度
         */
        private Integer maxStackTraceLength = 5000;
        
        /**
         * 是否记录请求信息
         */
        private Boolean includeRequestInfo = true;
    }
    
    /**
     * ELK配置
     */
    @Data
    public static class ElkConfig {
        
        /**
         * 是否启用ELK
         */
        private Boolean enabled = false;
        
        /**
         * Logstash地址
         */
        private String logstashHost = "localhost";
        
        /**
         * Logstash端口
         */
        private Integer logstashPort = 5044;
        
        /**
         * 应用名称
         */
        private String applicationName = "rui-application";
        
        /**
         * 环境
         */
        private String environment = "dev";
        
        /**
         * 版本
         */
        private String version = "1.0.0";
        
        /**
         * 自定义字段
         */
        private java.util.Map<String, String> customFields = new java.util.HashMap<>();
    }
}