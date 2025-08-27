package com.rui.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置管理属性类
 *
 * @author rui
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "rui.config")
public class ConfigProperties {

    /**
     * 是否启用配置管理
     */
    private boolean enabled = true;

    /**
     * 配置加密属性
     */
    @NestedConfigurationProperty
    private EncryptionProperties encryption = new EncryptionProperties();

    /**
     * 动态配置属性
     */
    @NestedConfigurationProperty
    private DynamicProperties dynamic = new DynamicProperties();

    /**
     * 配置验证属性
     */
    @NestedConfigurationProperty
    private ValidationProperties validation = new ValidationProperties();

    /**
     * 配置监控属性
     */
    @NestedConfigurationProperty
    private MonitoringProperties monitoring = new MonitoringProperties();

    /**
     * Nacos配置属性
     */
    @NestedConfigurationProperty
    private NacosProperties nacos = new NacosProperties();

    /**
     * Apollo配置属性
     */
    @NestedConfigurationProperty
    private ApolloProperties apollo = new ApolloProperties();

    /**
     * 配置缓存属性
     */
    @NestedConfigurationProperty
    private CacheProperties cache = new CacheProperties();

    /**
     * 配置备份属性
     */
    @NestedConfigurationProperty
    private BackupProperties backup = new BackupProperties();

    /**
     * 配置历史属性
     */
    @NestedConfigurationProperty
    private HistoryProperties history = new HistoryProperties();

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public EncryptionProperties getEncryption() {
        return encryption;
    }

    public void setEncryption(EncryptionProperties encryption) {
        this.encryption = encryption;
    }

    public DynamicProperties getDynamic() {
        return dynamic;
    }

    public void setDynamic(DynamicProperties dynamic) {
        this.dynamic = dynamic;
    }

    public ValidationProperties getValidation() {
        return validation;
    }

    public void setValidation(ValidationProperties validation) {
        this.validation = validation;
    }

    public MonitoringProperties getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(MonitoringProperties monitoring) {
        this.monitoring = monitoring;
    }

    public NacosProperties getNacos() {
        return nacos;
    }

    public void setNacos(NacosProperties nacos) {
        this.nacos = nacos;
    }

    public ApolloProperties getApollo() {
        return apollo;
    }

    public void setApollo(ApolloProperties apollo) {
        this.apollo = apollo;
    }

    public CacheProperties getCache() {
        return cache;
    }

    public void setCache(CacheProperties cache) {
        this.cache = cache;
    }

    public BackupProperties getBackup() {
        return backup;
    }

    public void setBackup(BackupProperties backup) {
        this.backup = backup;
    }

    public HistoryProperties getHistory() {
        return history;
    }

    public void setHistory(HistoryProperties history) {
        this.history = history;
    }

    /**
     * 配置加密属性
     */
    public static class EncryptionProperties {
        /**
         * 是否启用配置加密
         */
        private boolean enabled = false;

        /**
         * 加密算法
         */
        private String algorithm = "AES";

        /**
         * 密钥
         */
        private String secretKey = "rui-config-default-key";

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }

    /**
     * 动态配置属性
     */
    public static class DynamicProperties {
        /**
         * 是否启用动态配置
         */
        private boolean enabled = true;

        /**
         * 配置刷新间隔（秒）
         */
        private int refreshInterval = 30;

        /**
         * 配置源类型
         */
        private String source = "NACOS";

        /**
         * 线程池大小
         */
        private int threadPoolSize = 2;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRefreshInterval() {
            return refreshInterval;
        }

        public void setRefreshInterval(int refreshInterval) {
            this.refreshInterval = refreshInterval;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }
    }

    /**
     * 配置验证属性
     */
    public static class ValidationProperties {
        /**
         * 是否启用配置验证
         */
        private boolean enabled = true;

        /**
         * 验证模式
         */
        private String mode = "STRICT";

        /**
         * 是否在启动时验证
         */
        private boolean validateOnStartup = true;

        /**
         * 允许的配置键列表
         */
        private List<String> allowedKeys = new ArrayList<>();

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public boolean isValidateOnStartup() {
            return validateOnStartup;
        }

        public void setValidateOnStartup(boolean validateOnStartup) {
            this.validateOnStartup = validateOnStartup;
        }

        public List<String> getAllowedKeys() {
            return allowedKeys;
        }

        public void setAllowedKeys(List<String> allowedKeys) {
            this.allowedKeys = allowedKeys;
        }
    }

    /**
     * 配置监控属性
     */
    public static class MonitoringProperties {
        /**
         * 是否启用配置监控
         */
        private boolean enabled = true;

        /**
         * 监控指标收集间隔（秒）
         */
        private int metricsInterval = 60;

        /**
         * 是否记录配置变更日志
         */
        private boolean logChanges = true;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMetricsInterval() {
            return metricsInterval;
        }

        public void setMetricsInterval(int metricsInterval) {
            this.metricsInterval = metricsInterval;
        }

        public boolean isLogChanges() {
            return logChanges;
        }

        public void setLogChanges(boolean logChanges) {
            this.logChanges = logChanges;
        }
    }

    /**
     * Nacos配置属性
     */
    public static class NacosProperties {
        /**
         * 服务器地址
         */
        private String serverAddr = "localhost:8848";

        /**
         * 命名空间
         */
        private String namespace = "";

        /**
         * 用户名
         */
        private String username = "nacos";

        /**
         * 密码
         */
        private String password = "nacos";

        /**
         * 配置组
         */
        private String group = "DEFAULT_GROUP";

        /**
         * 配置数据ID
         */
        private String dataId = "application.yml";

        /**
         * 配置格式
         */
        private String type = "yaml";

        /**
         * 是否自动刷新
         */
        private boolean autoRefresh = true;

        /**
         * 刷新间隔（毫秒）
         */
        private long refreshInterval = 30000;

        // Getters and Setters
        public String getServerAddr() {
            return serverAddr;
        }

        public void setServerAddr(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getDataId() {
            return dataId;
        }

        public void setDataId(String dataId) {
            this.dataId = dataId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isAutoRefresh() {
            return autoRefresh;
        }

        public void setAutoRefresh(boolean autoRefresh) {
            this.autoRefresh = autoRefresh;
        }

        public long getRefreshInterval() {
            return refreshInterval;
        }

        public void setRefreshInterval(long refreshInterval) {
            this.refreshInterval = refreshInterval;
        }
    }

    /**
     * Apollo配置属性
     */
    public static class ApolloProperties {
        /**
         * 应用ID
         */
        private String appId = "rui-framework";

        /**
         * Meta服务器地址
         */
        private String meta = "http://localhost:8080";

        /**
         * 集群名称
         */
        private String cluster = "default";

        /**
         * 命名空间
         */
        private String namespaces = "application";

        /**
         * 是否启用自动更新
         */
        private boolean autoUpdateInjectedSpringProperties = true;

        // Getters and Setters
        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getMeta() {
            return meta;
        }

        public void setMeta(String meta) {
            this.meta = meta;
        }

        public String getCluster() {
            return cluster;
        }

        public void setCluster(String cluster) {
            this.cluster = cluster;
        }

        public String getNamespaces() {
            return namespaces;
        }

        public void setNamespaces(String namespaces) {
            this.namespaces = namespaces;
        }

        public boolean isAutoUpdateInjectedSpringProperties() {
            return autoUpdateInjectedSpringProperties;
        }

        public void setAutoUpdateInjectedSpringProperties(boolean autoUpdateInjectedSpringProperties) {
            this.autoUpdateInjectedSpringProperties = autoUpdateInjectedSpringProperties;
        }
    }

    /**
     * 配置缓存属性
     */
    public static class CacheProperties {
        /**
         * 是否启用配置缓存
         */
        private boolean enabled = true;

        /**
         * 缓存类型
         */
        private String type = "MEMORY";

        /**
         * 缓存过期时间（秒）
         */
        private int expireTime = 3600;

        /**
         * 最大缓存条目数
         */
        private int maxSize = 1000;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(int expireTime) {
            this.expireTime = expireTime;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }

    /**
     * 配置备份属性
     */
    public static class BackupProperties {
        /**
         * 是否启用配置备份
         */
        private boolean enabled = false;

        /**
         * 备份目录
         */
        private String directory = System.getProperty("user.home") + "/.rui/config/backup";

        /**
         * 备份保留天数
         */
        private int retentionDays = 30;

        /**
         * 备份间隔（小时）
         */
        private int intervalHours = 24;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }

        public int getIntervalHours() {
            return intervalHours;
        }

        public void setIntervalHours(int intervalHours) {
            this.intervalHours = intervalHours;
        }
    }

    /**
     * 配置历史属性
     */
    public static class HistoryProperties {
        /**
         * 是否启用历史记录
         */
        private boolean enabled = true;

        /**
         * 最大历史记录数量
         */
        private int maxSize = 1000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }
}