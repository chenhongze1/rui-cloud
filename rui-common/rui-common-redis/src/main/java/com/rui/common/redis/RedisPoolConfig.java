package com.rui.common.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Redis连接池配置
 * 提供高性能的Redis连接管理配置
 *
 * @author rui
 */
@Data
@Validated
@ConfigurationProperties(prefix = "rui.redis.pool")
public class RedisPoolConfig {

    /**
     * 是否启用连接池优化
     */
    private boolean enabled = true;

    /**
     * 连接池配置
     */
    private Pool pool = new Pool();

    /**
     * 连接配置
     */
    private Connection connection = new Connection();

    /**
     * 性能配置
     */
    private Performance performance = new Performance();

    /**
     * 监控配置
     */
    private Monitoring monitoring = new Monitoring();

    /**
     * 连接池配置
     */
    @Data
    public static class Pool {
        
        /**
         * 最大连接数
         */
        @Min(1)
        @Max(1000)
        private int maxTotal = 50;
        
        /**
         * 最大空闲连接数
         */
        @Min(0)
        @Max(100)
        private int maxIdle = 20;
        
        /**
         * 最小空闲连接数
         */
        @Min(0)
        @Max(50)
        private int minIdle = 5;
        
        /**
         * 获取连接时的最大等待时间
         */
        @NotNull
        private Duration maxWait = Duration.ofSeconds(10);
        
        /**
         * 是否在获取连接时验证连接
         */
        private boolean testOnBorrow = true;
        
        /**
         * 是否在归还连接时验证连接
         */
        private boolean testOnReturn = false;
        
        /**
         * 是否在空闲时验证连接
         */
        private boolean testWhileIdle = true;
        
        /**
         * 空闲连接检测间隔
         */
        @NotNull
        private Duration timeBetweenEvictionRuns = Duration.ofMinutes(1);
        
        /**
         * 连接空闲多久后被驱逐
         */
        @NotNull
        private Duration minEvictableIdleTime = Duration.ofMinutes(30);
        
        /**
         * 软驱逐空闲时间
         */
        @NotNull
        private Duration softMinEvictableIdleTime = Duration.ofMinutes(10);
        
        /**
         * 每次驱逐检查的连接数
         */
        @Min(1)
        @Max(10)
        private int numTestsPerEvictionRun = 3;
        
        /**
         * 是否启用LIFO（后进先出）
         */
        private boolean lifo = true;
        
        /**
         * 是否启用公平锁
         */
        private boolean fairness = false;
        
        /**
         * 是否阻塞耗尽时的获取请求
         */
        private boolean blockWhenExhausted = true;
    }

    /**
     * 连接配置
     */
    @Data
    public static class Connection {
        
        /**
         * 连接超时时间
         */
        @NotNull
        private Duration connectTimeout = Duration.ofSeconds(5);
        
        /**
         * 读取超时时间
         */
        @NotNull
        private Duration readTimeout = Duration.ofSeconds(3);
        
        /**
         * 是否启用TCP_NODELAY
         */
        private boolean tcpNoDelay = true;
        
        /**
         * 是否启用TCP_KEEPALIVE
         */
        private boolean tcpKeepAlive = true;
        
        /**
         * Socket发送缓冲区大小
         */
        @Min(1024)
        @Max(1048576)
        private int sendBufferSize = 8192;
        
        /**
         * Socket接收缓冲区大小
         */
        @Min(1024)
        @Max(1048576)
        private int receiveBufferSize = 8192;
        
        /**
         * 连接重试次数
         */
        @Min(0)
        @Max(10)
        private int retryAttempts = 3;
        
        /**
         * 重试间隔
         */
        @NotNull
        private Duration retryDelay = Duration.ofMillis(100);
        
        /**
         * 是否启用SSL
         */
        private boolean ssl = false;
        
        /**
         * SSL配置
         */
        private Ssl sslConfig = new Ssl();
    }

    /**
     * SSL配置
     */
    @Data
    public static class Ssl {
        
        /**
         * 密钥库路径
         */
        private String keyStore;
        
        /**
         * 密钥库密码
         */
        private String keyStorePassword;
        
        /**
         * 信任库路径
         */
        private String trustStore;
        
        /**
         * 信任库密码
         */
        private String trustStorePassword;
        
        /**
         * SSL协议
         */
        private String protocol = "TLS";
        
        /**
         * 是否验证主机名
         */
        private boolean verifyPeer = true;
    }

    /**
     * 性能配置
     */
    @Data
    public static class Performance {
        
        /**
         * 是否启用管道
         */
        private boolean pipelining = false;
        
        /**
         * 管道批次大小
         */
        @Min(1)
        @Max(1000)
        private int pipelineBatchSize = 100;
        
        /**
         * 是否启用压缩
         */
        private boolean compression = false;
        
        /**
         * 压缩算法
         */
        private CompressionType compressionType = CompressionType.GZIP;
        
        /**
         * 压缩阈值（字节）
         */
        @Min(100)
        @Max(10240)
        private int compressionThreshold = 1024;
        
        /**
         * 是否启用序列化缓存
         */
        private boolean serializationCache = true;
        
        /**
         * 序列化缓存大小
         */
        @Min(100)
        @Max(10000)
        private int serializationCacheSize = 1000;
        
        /**
         * 是否启用连接预热
         */
        private boolean connectionWarmup = true;
        
        /**
         * 预热连接数
         */
        @Min(1)
        @Max(20)
        private int warmupConnections = 5;
        
        /**
         * 是否启用慢查询检测
         */
        private boolean slowQueryDetection = true;
        
        /**
         * 慢查询阈值
         */
        @NotNull
        private Duration slowQueryThreshold = Duration.ofMillis(100);
    }

    /**
     * 压缩类型
     */
    public enum CompressionType {
        GZIP, DEFLATE, LZ4, SNAPPY
    }

    /**
     * 监控配置
     */
    @Data
    public static class Monitoring {
        
        /**
         * 是否启用监控
         */
        private boolean enabled = true;
        
        /**
         * 是否收集连接池指标
         */
        private boolean poolMetrics = true;
        
        /**
         * 是否收集命令指标
         */
        private boolean commandMetrics = true;
        
        /**
         * 是否收集延迟指标
         */
        private boolean latencyMetrics = true;
        
        /**
         * 指标收集间隔
         */
        @NotNull
        private Duration metricsInterval = Duration.ofSeconds(30);
        
        /**
         * 是否启用健康检查
         */
        private boolean healthCheck = true;
        
        /**
         * 健康检查间隔
         */
        @NotNull
        private Duration healthCheckInterval = Duration.ofMinutes(1);
        
        /**
         * 健康检查超时
         */
        @NotNull
        private Duration healthCheckTimeout = Duration.ofSeconds(5);
        
        /**
         * 是否记录慢查询
         */
        private boolean logSlowQueries = true;
        
        /**
         * 慢查询日志阈值
         */
        @NotNull
        private Duration slowQueryLogThreshold = Duration.ofMillis(200);
        
        /**
         * 是否启用连接泄漏检测
         */
        private boolean leakDetection = true;
        
        /**
         * 连接泄漏检测阈值
         */
        @NotNull
        private Duration leakDetectionThreshold = Duration.ofMinutes(5);
    }

    /**
     * 集群配置
     */
    @Data
    public static class Cluster {
        
        /**
         * 是否启用集群模式
         */
        private boolean enabled = false;
        
        /**
         * 集群节点
         */
        private String nodes;
        
        /**
         * 最大重定向次数
         */
        @Min(1)
        @Max(10)
        private int maxRedirects = 3;
        
        /**
         * 是否启用自适应刷新
         */
        private boolean adaptiveRefresh = true;
        
        /**
         * 拓扑刷新间隔
         */
        @NotNull
        private Duration topologyRefreshPeriod = Duration.ofMinutes(30);
        
        /**
         * 是否启用动态刷新源
         */
        private boolean dynamicRefreshSources = true;
        
        /**
         * 是否关闭过时连接
         */
        private boolean closeStaleConnections = true;
    }

    /**
     * 哨兵配置
     */
    @Data
    public static class Sentinel {
        
        /**
         * 是否启用哨兵模式
         */
        private boolean enabled = false;
        
        /**
         * 主节点名称
         */
        private String master;
        
        /**
         * 哨兵节点
         */
        private String nodes;
        
        /**
         * 哨兵密码
         */
        private String password;
        
        /**
         * 数据库索引
         */
        @Min(0)
        @Max(15)
        private int database = 0;
    }

    /**
     * 故障转移配置
     */
    @Data
    public static class Failover {
        
        /**
         * 是否启用故障转移
         */
        private boolean enabled = true;
        
        /**
         * 故障检测间隔
         */
        @NotNull
        private Duration failureDetectionTimeout = Duration.ofSeconds(10);
        
        /**
         * 重连间隔
         */
        @NotNull
        private Duration reconnectDelay = Duration.ofSeconds(1);
        
        /**
         * 最大重连次数
         */
        @Min(1)
        @Max(100)
        private int maxReconnectAttempts = 10;
        
        /**
         * 是否启用断路器
         */
        private boolean circuitBreaker = true;
        
        /**
         * 断路器失败阈值
         */
        @Min(1)
        @Max(100)
        private int circuitBreakerFailureThreshold = 5;
        
        /**
         * 断路器恢复时间
         */
        @NotNull
        private Duration circuitBreakerRecoveryTimeout = Duration.ofMinutes(1);
    }

    /**
     * 缓存配置
     */
    @Data
    public static class Cache {
        
        /**
         * 默认过期时间
         */
        @NotNull
        private Duration defaultExpiration = Duration.ofHours(1);
        
        /**
         * 是否启用空值缓存
         */
        private boolean cacheNullValues = false;
        
        /**
         * 空值缓存过期时间
         */
        @NotNull
        private Duration nullValueExpiration = Duration.ofMinutes(5);
        
        /**
         * 键前缀
         */
        private String keyPrefix = "rui:";
        
        /**
         * 是否使用键前缀
         */
        private boolean useKeyPrefix = true;
        
        /**
         * 序列化类型
         */
        private SerializationType serializationType = SerializationType.JSON;
    }

    /**
     * 序列化类型
     */
    public enum SerializationType {
        JSON, KRYO, PROTOBUF, JAVA
    }

    /**
     * 获取集群配置
     */
    private Cluster cluster = new Cluster();

    /**
     * 获取哨兵配置
     */
    private Sentinel sentinel = new Sentinel();

    /**
     * 获取故障转移配置
     */
    private Failover failover = new Failover();

    /**
     * 获取缓存配置
     */
    private Cache cache = new Cache();
}