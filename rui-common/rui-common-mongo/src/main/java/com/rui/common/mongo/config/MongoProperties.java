package com.rui.common.mongo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MongoDB配置属性
 * 
 * @author rui
 */
@Data
@ConfigurationProperties(prefix = "rui.mongo")
public class MongoProperties {
    
    /**
     * 是否启用MongoDB模块
     */
    private Boolean enabled = true;
    
    /**
     * 审计配置
     */
    private AuditConfig audit = new AuditConfig();
    
    /**
     * 分页配置
     */
    private PageConfig page = new PageConfig();
    
    /**
     * 索引配置
     */
    private IndexConfig index = new IndexConfig();
    
    /**
     * 事务配置
     */
    private TransactionConfig transaction = new TransactionConfig();
    
    /**
     * 审计配置
     */
    @Data
    public static class AuditConfig {
        
        /**
         * 是否启用审计
         */
        private Boolean enabled = true;
        
        /**
         * 创建时间字段名
         */
        private String createTimeField = "createTime";
        
        /**
         * 更新时间字段名
         */
        private String updateTimeField = "updateTime";
        
        /**
         * 创建人字段名
         */
        private String createByField = "createBy";
        
        /**
         * 更新人字段名
         */
        private String updateByField = "updateBy";
        
        /**
         * 版本字段名
         */
        private String versionField = "version";
        
        /**
         * 是否启用乐观锁
         */
        private Boolean optimisticLocking = true;
    }
    
    /**
     * 分页配置
     */
    @Data
    public static class PageConfig {
        
        /**
         * 默认页大小
         */
        private Integer defaultPageSize = 20;
        
        /**
         * 最大页大小
         */
        private Integer maxPageSize = 1000;
        
        /**
         * 页码参数名
         */
        private String pageParameter = "page";
        
        /**
         * 页大小参数名
         */
        private String sizeParameter = "size";
        
        /**
         * 排序参数名
         */
        private String sortParameter = "sort";
        
        /**
         * 是否启用分页合理化
         */
        private Boolean reasonable = true;
    }
    
    /**
     * 索引配置
     */
    @Data
    public static class IndexConfig {
        
        /**
         * 是否自动创建索引
         */
        private Boolean autoCreateIndex = true;
        
        /**
         * 索引创建超时时间（秒）
         */
        private Integer createTimeout = 30;
        
        /**
         * 是否在启动时检查索引
         */
        private Boolean checkOnStartup = true;
    }
    
    /**
     * 事务配置
     */
    @Data
    public static class TransactionConfig {
        
        /**
         * 是否启用事务
         */
        private Boolean enabled = false;
        
        /**
         * 事务超时时间（秒）
         */
        private Integer timeout = 30;
        
        /**
         * 事务隔离级别
         */
        private String isolation = "READ_COMMITTED";
        
        /**
         * 事务传播行为
         */
        private String propagation = "REQUIRED";
    }
}