package com.rui.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志配置类
 * 统一管理日志相关配置
 *
 * @author rui
 */
@Data
@Component
@ConfigurationProperties(prefix = "rui.logging")
public class LoggingConfig {

    /**
     * 是否启用日志功能
     */
    private boolean enabled = true;

    /**
     * 应用名称
     */
    private String applicationName = "rui-cloud";

    /**
     * 环境名称
     */
    private String environment = "dev";

    /**
     * 日志级别配置
     */
    private LevelConfig level = new LevelConfig();

    /**
     * 控制台日志配置
     */
    private ConsoleConfig console = new ConsoleConfig();

    /**
     * 文件日志配置
     */
    private FileConfig file = new FileConfig();

    /**
     * 结构化日志配置
     */
    private StructuredConfig structured = new StructuredConfig();

    /**
     * 审计日志配置
     */
    private AuditConfig audit = new AuditConfig();

    /**
     * 性能日志配置
     */
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * 安全日志配置
     */
    private SecurityConfig security = new SecurityConfig();

    /**
     * 业务日志配置
     */
    private BusinessConfig business = new BusinessConfig();

    /**
     * 异步日志配置
     */
    private AsyncConfig async = new AsyncConfig();

    /**
     * 敏感信息配置
     */
    private SensitiveConfig sensitive = new SensitiveConfig();

    /**
     * 日志级别配置
     */
    @Data
    public static class LevelConfig {
        /**
         * 根日志级别
         */
        private String root = "INFO";
        
        /**
         * 包级别日志配置
         */
        private Map<String, String> packages = new HashMap<>();
        
        /**
         * SQL日志级别
         */
        private String sql = "DEBUG";
        
        /**
         * HTTP日志级别
         */
        private String http = "INFO";
        
        /**
         * 业务日志级别
         */
        private String business = "INFO";
    }

    /**
     * 控制台日志配置
     */
    @Data
    public static class ConsoleConfig {
        /**
         * 是否启用控制台日志
         */
        private boolean enabled = true;
        
        /**
         * 日志格式
         */
        private String pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n";
        
        /**
         * 是否使用彩色输出
         */
        private boolean colorEnabled = true;
        
        /**
         * 字符编码
         */
        private String charset = "UTF-8";
    }

    /**
     * 文件日志配置
     */
    @Data
    public static class FileConfig {
        /**
         * 是否启用文件日志
         */
        private boolean enabled = true;
        
        /**
         * 日志文件路径
         */
        private String path = "./logs";
        
        /**
         * 日志文件名
         */
        private String name = "application.log";
        
        /**
         * 日志格式
         */
        private String pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n";
        
        /**
         * 单个文件最大大小
         */
        private String maxFileSize = "100MB";
        
        /**
         * 最大历史文件数
         */
        private int maxHistory = 30;
        
        /**
         * 总文件大小限制
         */
        private String totalSizeCap = "10GB";
        
        /**
         * 字符编码
         */
        private String charset = "UTF-8";
        
        /**
         * 是否立即刷新
         */
        private boolean immediateFlush = false;
    }

    /**
     * 结构化日志配置
     */
    @Data
    public static class StructuredConfig {
        /**
         * 是否启用结构化日志
         */
        private boolean enabled = true;
        
        /**
         * 日志格式（JSON/LOGSTASH等）
         */
        private String format = "JSON";
        
        /**
         * 是否包含MDC
         */
        private boolean includeMdc = true;
        
        /**
         * 是否包含异常堆栈
         */
        private boolean includeStackTrace = true;
        
        /**
         * 时间戳格式
         */
        private String timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        
        /**
         * 自定义字段
         */
        private Map<String, String> customFields = new HashMap<>();
    }

    /**
     * 审计日志配置
     */
    @Data
    public static class AuditConfig {
        /**
         * 是否启用审计日志
         */
        private boolean enabled = true;
        
        /**
         * 审计日志文件名
         */
        private String fileName = "audit.log";
        
        /**
         * 是否记录登录事件
         */
        private boolean logLogin = true;
        
        /**
         * 是否记录权限检查
         */
        private boolean logPermission = true;
        
        /**
         * 是否记录数据访问
         */
        private boolean logDataAccess = true;
        
        /**
         * 是否记录配置变更
         */
        private boolean logConfigChange = true;
        
        /**
         * 需要审计的操作类型
         */
        private List<String> auditOperations = new ArrayList<>();
    }

    /**
     * 性能日志配置
     */
    @Data
    public static class PerformanceConfig {
        /**
         * 是否启用性能日志
         */
        private boolean enabled = true;
        
        /**
         * 性能日志文件名
         */
        private String fileName = "performance.log";
        
        /**
         * 慢查询阈值（毫秒）
         */
        private long slowQueryThreshold = 1000;
        
        /**
         * 慢接口阈值（毫秒）
         */
        private long slowApiThreshold = 2000;
        
        /**
         * 是否记录方法执行时间
         */
        private boolean logMethodTime = true;
        
        /**
         * 是否记录SQL执行时间
         */
        private boolean logSqlTime = true;
        
        /**
         * 是否记录HTTP请求时间
         */
        private boolean logHttpTime = true;
    }

    /**
     * 安全日志配置
     */
    @Data
    public static class SecurityConfig {
        /**
         * 是否启用安全日志
         */
        private boolean enabled = true;
        
        /**
         * 安全日志文件名
         */
        private String fileName = "security.log";
        
        /**
         * 是否记录认证失败
         */
        private boolean logAuthFailure = true;
        
        /**
         * 是否记录授权失败
         */
        private boolean logAuthzFailure = true;
        
        /**
         * 是否记录可疑活动
         */
        private boolean logSuspiciousActivity = true;
        
        /**
         * 是否记录IP封禁
         */
        private boolean logIpBlocking = true;
        
        /**
         * 是否记录令牌操作
         */
        private boolean logTokenOperation = true;
    }

    /**
     * 业务日志配置
     */
    @Data
    public static class BusinessConfig {
        /**
         * 是否启用业务日志
         */
        private boolean enabled = true;
        
        /**
         * 业务日志文件名
         */
        private String fileName = "business.log";
        
        /**
         * 是否记录用户操作
         */
        private boolean logUserOperation = true;
        
        /**
         * 是否记录数据变更
         */
        private boolean logDataChange = true;
        
        /**
         * 是否记录业务异常
         */
        private boolean logBusinessException = true;
        
        /**
         * 需要记录的业务模块
         */
        private List<String> modules = new ArrayList<>();
    }

    /**
     * 异步日志配置
     */
    @Data
    public static class AsyncConfig {
        /**
         * 是否启用异步日志
         */
        private boolean enabled = true;
        
        /**
         * 队列大小
         */
        private int queueSize = 1024;
        
        /**
         * 丢弃阈值
         */
        private int discardingThreshold = 20;
        
        /**
         * 工作线程数
         */
        private int workerThreads = 1;
        
        /**
         * 最大刷新时间（毫秒）
         */
        private int maxFlushTime = 1000;
        
        /**
         * 是否包含调用者信息
         */
        private boolean includeCallerData = false;
    }

    /**
     * 敏感信息配置
     */
    @Data
    public static class SensitiveConfig {
        /**
         * 是否启用敏感信息过滤
         */
        private boolean enabled = true;
        
        /**
         * 敏感字段列表
         */
        private List<String> sensitiveFields = new ArrayList<>();
        
        /**
         * 替换字符
         */
        private String replacement = "***";
        
        /**
         * 是否使用正则表达式
         */
        private boolean useRegex = false;
        
        /**
         * 自定义正则表达式
         */
        private List<String> regexPatterns = new ArrayList<>();
    }
}