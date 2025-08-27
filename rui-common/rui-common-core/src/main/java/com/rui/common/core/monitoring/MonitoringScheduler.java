package com.rui.common.core.monitoring;

import com.rui.common.core.config.MonitoringConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 监控任务调度器
 * 定期收集和处理监控数据
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringScheduler {

    private final MonitoringConfig monitoringConfig;
    private final MetricsCollector metricsCollector;
    private final HealthChecker healthChecker;
    
    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        // 创建线程池用于异步执行监控任务
        this.executorService = Executors.newFixedThreadPool(
            monitoringConfig.getMetrics().getThreadPoolSize()
        );
        log.info("监控任务调度器初始化完成");
    }

    /**
     * 定期收集JVM指标
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void collectJvmMetrics() {
        if (!monitoringConfig.getMetrics().isJvmEnabled()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                metricsCollector.recordJvmMetrics();
                log.debug("JVM指标收集完成");
            } catch (Exception e) {
                log.error("JVM指标收集失败", e);
            }
        }, executorService);
    }

    /**
     * 定期收集系统指标
     * 每60秒执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void collectSystemMetrics() {
        if (!monitoringConfig.getMetrics().isSystemEnabled()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                metricsCollector.recordSystemMetrics();
                log.debug("系统指标收集完成");
            } catch (Exception e) {
                log.error("系统指标收集失败", e);
            }
        }, executorService);
    }

    /**
     * 定期执行健康检查
     * 每2分钟执行一次
     */
    @Scheduled(fixedRate = 120000)
    public void performHealthCheck() {
        if (!monitoringConfig.getHealth().isEnabled()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Health overallHealth = healthChecker.checkOverallHealth();
                
                // 记录健康状态指标
                String status = overallHealth.getStatus().getCode();
                metricsCollector.recordHealthStatus(status, Status.UP.equals(overallHealth.getStatus()));
                
                // 如果健康状态异常，记录详细信息
                if (!Status.UP.equals(overallHealth.getStatus())) {
                    log.warn("系统健康检查异常: status={}, details={}", 
                        status, overallHealth.getDetails());
                }
                
                log.debug("健康检查完成: {}", status);
            } catch (Exception e) {
                log.error("健康检查失败", e);
                metricsCollector.recordHealthStatus("ERROR", false);
            }
        }, executorService);
    }

    /**
     * 定期清理过期数据
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredData() {
        CompletableFuture.runAsync(() -> {
            try {
                // 清理过期的监控数据
                cleanupMetricsData();
                cleanupHealthData();
                
                log.info("过期数据清理完成");
            } catch (Exception e) {
                log.error("过期数据清理失败", e);
            }
        }, executorService);
    }

    /**
     * 定期生成监控报告
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateDailyReport() {
        if (!monitoringConfig.getAlert().isEnabled()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                generateMonitoringReport();
                log.info("每日监控报告生成完成");
            } catch (Exception e) {
                log.error("每日监控报告生成失败", e);
            }
        }, executorService);
    }

    /**
     * 定期检查慢操作
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000)
    public void checkSlowOperations() {
        if (!monitoringConfig.getPerformance().isSlowOperationEnabled()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                checkAndReportSlowOperations();
                log.debug("慢操作检查完成");
            } catch (Exception e) {
                log.error("慢操作检查失败", e);
            }
        }, executorService);
    }

    /**
     * 定期检查资源使用情况
     * 每3分钟执行一次
     */
    @Scheduled(fixedRate = 180000)
    public void checkResourceUsage() {
        if (!monitoringConfig.getPerformance().isResourceUsageEnabled()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                checkAndReportResourceUsage();
                log.debug("资源使用检查完成");
            } catch (Exception e) {
                log.error("资源使用检查失败", e);
            }
        }, executorService);
    }

    /**
     * 清理指标数据
     */
    private void cleanupMetricsData() {
        // 这里可以实现清理逻辑，比如删除Redis中的过期数据
        log.debug("清理指标数据");
    }

    /**
     * 清理健康检查数据
     */
    private void cleanupHealthData() {
        // 这里可以实现清理逻辑
        log.debug("清理健康检查数据");
    }

    /**
     * 生成监控报告
     */
    private void generateMonitoringReport() {
        // 生成包含各种指标的监控报告
        log.info("生成监控报告: {}", LocalDateTime.now());
        
        // 可以发送邮件、保存到文件或发送到外部系统
    }

    /**
     * 检查并报告慢操作
     */
    private void checkAndReportSlowOperations() {
        long threshold = monitoringConfig.getPerformance().getSlowOperationThreshold().toMillis();
        
        // 检查各种操作的执行时间
        // 如果超过阈值，记录并可能发送告警
        
        log.debug("检查慢操作，阈值: {}ms", threshold);
    }

    /**
     * 检查并报告资源使用情况
     */
    private void checkAndReportResourceUsage() {
        // 检查CPU、内存、磁盘等资源使用情况
        // 如果超过阈值，记录并可能发送告警
        
        log.debug("检查资源使用情况");
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            log.info("监控任务调度器已关闭");
        }
    }
}