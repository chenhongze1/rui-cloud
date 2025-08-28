package com.rui.common.monitoring.metrics;

import com.rui.common.monitoring.properties.MonitoringProperties;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 监控指标收集器
 * 收集系统、JVM、业务等各种指标
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsCollector {

    private final MeterRegistry meterRegistry;
    private final MonitoringProperties monitoringProperties;
    
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    // 系统指标
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicReference<Double> averageResponseTime = new AtomicReference<>(0.0);

    // 业务指标
    private final AtomicLong activeUsers = new AtomicLong(0);
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong failedTransactions = new AtomicLong(0);

    // 计时器和计数器
    private Timer httpRequestTimer;
    private Timer databaseQueryTimer;
    private Timer redisOperationTimer;
    private Counter businessOperationCounter;
    private Counter errorCounter;
    private Gauge activeSessionsGauge;

    @PostConstruct
    public void initializeMetrics() {
        if (!monitoringProperties.isEnabled() || !monitoringProperties.getMetrics().isEnabled()) {
            log.info("监控指标收集已禁用");
            return;
        }

        log.info("初始化监控指标收集器...");

        // 注册JVM指标
        registerJvmMetrics();

        // 注册系统指标
        registerSystemMetrics();

        // 注册HTTP指标
        registerHttpMetrics();

        // 注册数据库指标
        registerDatabaseMetrics();

        // 注册Redis指标
        registerRedisMetrics();

        // 注册业务指标
        registerBusinessMetrics();

        // 注册自定义指标
        registerCustomMetrics();

        log.info("监控指标收集器初始化完成");
    }

    /**
     * 注册JVM指标
     */
    private void registerJvmMetrics() {
        MonitoringProperties.JvmMetricsConfig jvmConfig = monitoringProperties.getMetrics().getJvm();
        if (!jvmConfig.isEnabled()) {
            return;
        }

        log.debug("注册JVM指标...");

        if (jvmConfig.isMemoryMetrics()) {
            new JvmMemoryMetrics().bindTo(meterRegistry);
        }

        if (jvmConfig.isGcMetrics()) {
            new JvmGcMetrics().bindTo(meterRegistry);
        }

        if (jvmConfig.isThreadMetrics()) {
            new JvmThreadMetrics().bindTo(meterRegistry);
        }

        if (jvmConfig.isClassLoaderMetrics()) {
            new ClassLoaderMetrics().bindTo(meterRegistry);
        }

        // 自定义JVM指标
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Gauge.builder("jvm.memory.heap.utilization", memoryBean, bean -> {
                long used = bean.getHeapMemoryUsage().getUsed();
                long max = bean.getHeapMemoryUsage().getMax();
                return max > 0 ? (double) used / max : 0;
            })
            .description("JVM堆内存使用率")
            .register(meterRegistry);

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Gauge.builder("system.cpu.usage", osBean, bean -> {
            if (bean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) bean).getProcessCpuLoad();
            }
            return 0.0;
        })
            .description("系统CPU使用率")
            .register(meterRegistry);
    }

    /**
     * 注册系统指标
     */
    private void registerSystemMetrics() {
        log.debug("注册系统指标...");

        new ProcessorMetrics().bindTo(meterRegistry);
        new FileDescriptorMetrics().bindTo(meterRegistry);
        new UptimeMetrics().bindTo(meterRegistry);
        java.io.File[] roots = java.io.File.listRoots();
        if (roots.length > 0) {
            new io.micrometer.core.instrument.binder.system.DiskSpaceMetrics(roots[0]).bindTo(meterRegistry);
        }

        // 自定义系统指标
        Gauge.builder("system.load.average", ManagementFactory.getOperatingSystemMXBean(),
                bean -> bean.getSystemLoadAverage())
            .description("系统平均负载")
            .register(meterRegistry);
    }

    /**
     * 注册HTTP指标
     */
    private void registerHttpMetrics() {
        MonitoringProperties.HttpMetricsConfig httpConfig = monitoringProperties.getMetrics().getHttp();
        if (!httpConfig.isEnabled()) {
            return;
        }

        log.debug("注册HTTP指标...");

        // HTTP请求计时器
        httpRequestTimer = Timer.builder("http.requests")
            .description("HTTP请求执行时间")
            .register(meterRegistry);

        // HTTP请求计数器
        Counter.builder("http.requests.total")
            .description("HTTP请求总数")
            .register(meterRegistry);

        // HTTP错误计数器
        Counter.builder("http.requests.errors")
            .description("HTTP请求错误数")
            .register(meterRegistry);

        // 活跃连接数
        Gauge.builder("http.connections.active", this, collector -> (double) (totalRequests.get() - successfulRequests.get() - failedRequests.get()))
            .description("活跃HTTP连接数")
            .register(meterRegistry);

        // 平均响应时间
        Gauge.builder("http.response.time.average", averageResponseTime, AtomicReference::get)
            .description("平均HTTP响应时间")
            .register(meterRegistry);
    }

    /**
     * 注册数据库指标
     */
    private void registerDatabaseMetrics() {
        MonitoringProperties.DatabaseMetricsConfig dbConfig = monitoringProperties.getMetrics().getDatabase();
        if (!dbConfig.isEnabled()) {
            return;
        }

        log.debug("注册数据库指标...");

        // 数据库查询计时器
        databaseQueryTimer = Timer.builder("database.queries")
            .description("数据库查询执行时间")
            .register(meterRegistry);

        // 数据库连接池指标
        if (dbConfig.isConnectionPoolMetrics()) {
            Gauge.builder("database.connections.active", this, collector -> (double) getActiveDbConnections())
                .description("活跃数据库连接数")
                .register(meterRegistry);

            Gauge.builder("database.connections.idle", this, collector -> (double) getIdleDbConnections())
                .description("空闲数据库连接数")
                .register(meterRegistry);
        }

        // 慢查询计数器
        if (dbConfig.isSlowQueryMetrics()) {
            Counter.builder("database.queries.slow")
                .description("慢查询数量")
                .register(meterRegistry);
        }

        // 数据库错误计数器
        Counter.builder("database.errors")
            .description("数据库错误数")
            .register(meterRegistry);
    }

    /**
     * 注册Redis指标
     */
    private void registerRedisMetrics() {
        MonitoringProperties.RedisMetricsConfig redisConfig = monitoringProperties.getMetrics().getRedis();
        if (!redisConfig.isEnabled()) {
            return;
        }

        log.debug("注册Redis指标...");

        // Redis操作计时器
        redisOperationTimer = Timer.builder("redis.operations")
            .description("Redis操作执行时间")
            .register(meterRegistry);

        // Redis连接指标
        if (redisConfig.isConnectionMetrics()) {
            Gauge.builder("redis.connections.active", this, collector -> (double) getActiveRedisConnections())
                .description("活跃Redis连接数")
                .register(meterRegistry);
        }

        // Redis命令指标
        if (redisConfig.isCommandMetrics()) {
            Counter.builder("redis.commands.total")
                .description("Redis命令总数")
                .register(meterRegistry);

            Counter.builder("redis.commands.errors")
                .description("Redis命令错误数")
                .register(meterRegistry);
        }

        // Redis内存指标
        if (redisConfig.isMemoryMetrics()) {
            Gauge.builder("redis.memory.used", this, collector -> (double) getRedisMemoryUsed())
                .description("Redis内存使用量")
                .register(meterRegistry);
        }

        // Redis键空间指标
        if (redisConfig.isKeyspaceMetrics()) {
            Gauge.builder("redis.keyspace.keys", this, collector -> (double) getRedisKeyCount())
                .description("Redis键数量")
                .register(meterRegistry);
        }
    }

    /**
     * 注册业务指标
     */
    private void registerBusinessMetrics() {
        MonitoringProperties.BusinessConfig businessConfig = monitoringProperties.getBusiness();
        if (!businessConfig.isEnabled()) {
            return;
        }

        log.debug("注册业务指标...");

        // 业务操作计数器
        businessOperationCounter = Counter.builder("business.operations")
            .description("业务操作数量")
            .register(meterRegistry);

        // 活跃用户数
        Gauge.builder("business.users.active", activeUsers, AtomicLong::doubleValue)
            .description("活跃用户数")
            .register(meterRegistry);

        // 交易指标
        if (businessConfig.getBusinessMetrics().isTransactionMetrics()) {
            Counter.builder("business.transactions.total")
                .description("交易总数")
                .register(meterRegistry);

            Counter.builder("business.transactions.failed")
                .description("失败交易数")
                .register(meterRegistry);

            Gauge.builder("business.transactions.success.rate", this, collector -> calculateTransactionSuccessRate())
                .description("交易成功率")
                .register(meterRegistry);
        }

        // 错误率指标
        if (businessConfig.getErrorMonitoring().isErrorRateTracking()) {
            errorCounter = Counter.builder("business.errors")
                .description("业务错误数")
                .register(meterRegistry);

            Gauge.builder("business.error.rate", this, collector -> calculateErrorRate())
                .description("业务错误率")
                .register(meterRegistry);
        }

        // 会话指标
        activeSessionsGauge = Gauge.builder("business.sessions.active", this, collector -> (double) getActiveSessionCount())
            .description("活跃会话数")
            .register(meterRegistry);
    }

    /**
     * 注册自定义指标
     */
    private void registerCustomMetrics() {
        log.debug("注册自定义指标...");

        // 应用启动时间
        Gauge.builder("application.uptime", ManagementFactory.getRuntimeMXBean(),
                bean -> bean.getUptime() / 1000.0)
            .description("应用运行时间")
            .register(meterRegistry);

        // 线程池指标
        Gauge.builder("threadpool.active.threads", Thread.class, clazz -> (double) Thread.activeCount())
            .description("活跃线程数")
            .register(meterRegistry);

        // 垃圾回收指标
        Gauge.builder("gc.pause.total", this, collector -> (double) getTotalGcTime())
            .description("GC暂停总时间")
            .register(meterRegistry);
    }

    // ========== 指标记录方法 ==========

    /**
     * 记录HTTP请求
     */
    public void recordHttpRequest(Duration duration, String method, String uri, int status) {
        if (httpRequestTimer != null) {
            httpRequestTimer.record(duration);
        }
        
        totalRequests.incrementAndGet();
        if (status >= 200 && status < 400) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        
        updateAverageResponseTime(duration);
    }

    /**
     * 记录数据库查询
     */
    public void recordDatabaseQuery(Duration duration, String operation, boolean isSlowQuery) {
        if (databaseQueryTimer != null) {
            databaseQueryTimer.record(duration);
        }
        
        if (isSlowQuery) {
            meterRegistry.counter("database.queries.slow", "operation", operation).increment();
        }
    }

    /**
     * 记录Redis操作
     */
    public void recordRedisOperation(Duration duration, String command) {
        if (redisOperationTimer != null) {
            redisOperationTimer.record(duration);
        }
        
        meterRegistry.counter("redis.commands.total", "command", command).increment();
    }

    /**
     * 记录业务操作
     */
    public void recordBusinessOperation(String operation, String result) {
        if (businessOperationCounter != null) {
            Counter.builder("business.operations.total")
                .tags("operation", operation, "result", result)
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * 记录健康状态
     */
    public void recordHealthStatus(String component, boolean healthy) {
        Gauge.builder("health.status", this, collector -> healthy ? 1.0 : 0.0)
            .description("组件健康状态")
            .tags("component", component)
            .register(meterRegistry);
    }

    /**
     * 记录JVM指标
     */
    public void recordJvmMetrics() {
        // JVM内存使用情况
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        Gauge.builder("jvm.memory.used", memoryBean, bean -> (double) bean.getHeapMemoryUsage().getUsed())
                .description("JVM已使用内存")
                .register(meterRegistry);
        
        Gauge.builder("jvm.memory.total", memoryBean, bean -> (double) bean.getHeapMemoryUsage().getMax())
                .description("JVM总内存")
                .register(meterRegistry);
    }

    /**
     * 记录系统指标
     */
    public void recordSystemMetrics() {
        // 系统CPU使用率等指标
        com.sun.management.OperatingSystemMXBean osBean = 
            (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        
        double cpuUsage = osBean.getProcessCpuLoad();
        if (cpuUsage >= 0) {
            Gauge.builder("system.cpu.usage", osBean, bean -> bean.getProcessCpuLoad())
                    .description("系统CPU使用率")
                    .register(meterRegistry);
        }
    }

    /**
     * 记录错误
     */
    public void recordError(String errorType, String errorCode) {
        if (errorCounter != null) {
            Counter.builder("errors.total")
                .tags("type", errorType, "code", errorCode)
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * 更新活跃用户数
     */
    public void updateActiveUsers(long count) {
        activeUsers.set(count);
    }

    /**
     * 增加交易计数
     */
    public void incrementTransactionCount(boolean success) {
        totalTransactions.incrementAndGet();
        if (!success) {
            failedTransactions.incrementAndGet();
        }
    }

    // ========== 私有辅助方法 ==========

    private void updateAverageResponseTime(Duration duration) {
        // 简单的移动平均算法
        double current = averageResponseTime.get();
        double newValue = (current * 0.9) + (duration.toMillis() * 0.1);
        averageResponseTime.set(newValue);
    }

    private double calculateTransactionSuccessRate() {
        long total = totalTransactions.get();
        if (total == 0) return 1.0;
        long failed = failedTransactions.get();
        return (double) (total - failed) / total;
    }

    private double calculateErrorRate() {
        long total = totalRequests.get();
        if (total == 0) return 0.0;
        long errors = failedRequests.get();
        return (double) errors / total;
    }

    // 这些方法需要根据实际的连接池实现来获取真实数据
    private long getActiveDbConnections() {
        // TODO: 从实际的数据库连接池获取活跃连接数
        return 0;
    }

    private long getIdleDbConnections() {
        // TODO: 从实际的数据库连接池获取空闲连接数
        return 0;
    }

    private long getActiveRedisConnections() {
        // TODO: 从实际的Redis连接池获取活跃连接数
        return 0;
    }

    private long getRedisMemoryUsed() {
        // TODO: 从Redis获取内存使用量
        return 0;
    }

    private long getRedisKeyCount() {
        // TODO: 从Redis获取键数量
        return 0;
    }

    private long getActiveSessionCount() {
        // TODO: 从会话管理器获取活跃会话数
        return 0;
    }

    private long getTotalGcTime() {
        // TODO: 计算总GC时间
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(bean -> bean.getCollectionTime())
            .sum();
    }

    /**
     * 记录业务指标
     */
    public void recordBusinessMetric(String name, double value, String... tags) {
        meterRegistry.gauge("business." + name, Tags.of(tags), value);
    }
    
    /**
     * 记录内存使用指标
     */
    public void recordMemoryUsage(long usedMemory) {
        meterRegistry.gauge("method.memory.used", usedMemory);
    }
    
    /**
     * 记录性能指标
     */
    public void recordPerformanceMetric(String operation, long duration, java.util.Map<String, Object> context) {
        // 记录执行时间分布
        Timer.builder("performance.method.duration")
            .description("Method execution duration")
            .tag("operation", operation)
            .tag("module", (String) context.getOrDefault("module", "unknown"))
            .register(meterRegistry)
            .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        // 记录性能指标计数器
        Counter.builder("performance.method.count")
            .description("Method execution count")
            .tag("operation", operation)
            .tag("module", (String) context.getOrDefault("module", "unknown"))
            .tag("status", (Boolean) context.getOrDefault("success", true) ? "success" : "error")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 记录慢操作指标
     */
    public void recordSlowOperation(String operation, String module, long duration, java.util.Map<String, Object> context) {
        // 记录慢操作计数器
        Counter.builder("performance.slow.operation.count")
            .description("Slow operation count")
            .tag("operation", operation)
            .tag("module", module)
            .register(meterRegistry)
            .increment();
        
        // 记录慢操作持续时间
        Timer.builder("performance.slow.operation.duration")
            .description("Slow operation duration")
            .tag("operation", operation)
            .tag("module", module)
            .register(meterRegistry)
            .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        // 记录慢操作详细信息到Gauge（用于实时监控）
        meterRegistry.gauge("performance.slow.operation.latest", 
            Tags.of("operation", operation, "module", module), duration);
    }
    
    /**
     * 记录方法调用统计
     */
    public void recordMethodInvocation(String className, String methodName, boolean success, long duration) {
        // 方法调用计数
        Counter.builder("method.invocation.total")
            .description("Total method invocations")
            .tag("class", className)
            .tag("method", methodName)
            .tag("result", success ? "success" : "error")
            .register(meterRegistry)
            .increment();
        
        // 方法执行时间
        Timer.builder("method.execution.time")
            .description("Method execution time")
            .tag("class", className)
            .tag("method", methodName)
            .register(meterRegistry)
            .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}