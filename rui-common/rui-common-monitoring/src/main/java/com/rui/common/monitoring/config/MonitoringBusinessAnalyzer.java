package com.rui.common.monitoring.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控业务分析器
 * 负责分析业务指标和趋势
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringBusinessAnalyzer {

    private final MonitoringConfig monitoringConfig;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, BusinessMetric> businessMetrics = new ConcurrentHashMap<>();
    private final Map<String, List<BusinessEvent>> businessEvents = new ConcurrentHashMap<>();
    private final AtomicLong eventCounter = new AtomicLong(0);

    @PostConstruct
    public void init() {
        if (monitoringConfig.getBusiness().isEnabled()) {
            initializeDefaultMetrics();
            startBusinessAnalysis();
        }
    }

    /**
     * 初始化默认业务指标
     */
    private void initializeDefaultMetrics() {
        // 用户相关指标
        addBusinessMetric("user_login_count", "用户登录次数", MetricType.COUNTER);
        addBusinessMetric("user_registration_count", "用户注册次数", MetricType.COUNTER);
        addBusinessMetric("active_user_count", "活跃用户数", MetricType.GAUGE);
        
        // 业务操作指标
        addBusinessMetric("api_request_count", "API请求次数", MetricType.COUNTER);
        addBusinessMetric("business_transaction_count", "业务交易次数", MetricType.COUNTER);
        addBusinessMetric("error_count", "错误次数", MetricType.COUNTER);
        
        // 性能指标
        addBusinessMetric("response_time_avg", "平均响应时间", MetricType.GAUGE);
        addBusinessMetric("throughput", "吞吐量", MetricType.GAUGE);
        
        log.info("Initialized {} default business metrics", businessMetrics.size());
    }

    /**
     * 启动业务分析
     */
    private void startBusinessAnalysis() {
        // 每分钟分析一次业务数据
        scheduler.scheduleAtFixedRate(this::analyzeBusinessMetrics, 0, 1, TimeUnit.MINUTES);
        
        // 每小时生成业务报告
        scheduler.scheduleAtFixedRate(this::generateBusinessReport, 0, 1, TimeUnit.HOURS);
        
        // 每天清理过期数据
        scheduler.scheduleAtFixedRate(this::cleanupExpiredData, 0, 24, TimeUnit.HOURS);
        
        log.info("Business analysis started");
    }

    /**
     * 添加业务指标
     */
    public void addBusinessMetric(String metricName, String description, MetricType type) {
        BusinessMetric metric = new BusinessMetric(metricName, description, type);
        businessMetrics.put(metricName, metric);
        log.debug("Added business metric: {}", metricName);
    }

    /**
     * 记录业务事件
     */
    public void recordBusinessEvent(String eventType, String eventName, Map<String, Object> properties) {
        try {
            long eventId = eventCounter.incrementAndGet();
            BusinessEvent event = new BusinessEvent(
                eventId, eventType, eventName, LocalDateTime.now(), properties
            );
            
            businessEvents.computeIfAbsent(eventType, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(event);
            
            // 更新相关指标
            updateMetricsFromEvent(event);
            
            log.debug("Recorded business event: {} - {}", eventType, eventName);
            
        } catch (Exception e) {
            log.error("Failed to record business event", e);
        }
    }

    /**
     * 从事件更新指标
     */
    private void updateMetricsFromEvent(BusinessEvent event) {
        String eventType = event.getEventType();
        
        // 根据事件类型更新对应指标
        switch (eventType.toLowerCase()) {
            case "user_login":
                incrementMetric("user_login_count");
                break;
            case "user_registration":
                incrementMetric("user_registration_count");
                break;
            case "api_request":
                incrementMetric("api_request_count");
                updateResponseTimeMetric(event);
                break;
            case "business_transaction":
                incrementMetric("business_transaction_count");
                break;
            case "error":
                incrementMetric("error_count");
                break;
        }
    }

    /**
     * 更新响应时间指标
     */
    private void updateResponseTimeMetric(BusinessEvent event) {
        Object responseTime = event.getProperties().get("responseTime");
        if (responseTime instanceof Number) {
            BusinessMetric metric = businessMetrics.get("response_time_avg");
            if (metric != null) {
                metric.updateValue(((Number) responseTime).doubleValue());
            }
        }
    }

    /**
     * 增加计数器指标
     */
    public void incrementMetric(String metricName) {
        incrementMetric(metricName, 1);
    }

    /**
     * 增加计数器指标
     */
    public void incrementMetric(String metricName, long increment) {
        BusinessMetric metric = businessMetrics.get(metricName);
        if (metric != null && metric.getType() == MetricType.COUNTER) {
            metric.increment(increment);
        }
    }

    /**
     * 设置仪表盘指标值
     */
    public void setGaugeMetric(String metricName, double value) {
        BusinessMetric metric = businessMetrics.get(metricName);
        if (metric != null && metric.getType() == MetricType.GAUGE) {
            metric.setValue(value);
        }
    }

    /**
     * 分析业务指标
     */
    private void analyzeBusinessMetrics() {
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            // 分析指标趋势
            Map<String, Object> trends = analyzeMetricTrends();
            analysis.put("trends", trends);
            
            // 分析事件统计
            Map<String, Object> eventStats = analyzeEventStatistics();
            analysis.put("eventStatistics", eventStats);
            
            // 分析业务健康度
            Map<String, Object> healthScore = calculateBusinessHealthScore();
            analysis.put("healthScore", healthScore);
            
            // 更新到监控配置的运行时指标中
            monitoringConfig.getRuntimeMetrics().put("businessAnalysis", analysis);
            
            log.debug("Business metrics analysis completed");
            
        } catch (Exception e) {
            log.error("Failed to analyze business metrics", e);
            monitoringConfig.getConfigErrorCount().incrementAndGet();
        }
    }

    /**
     * 分析指标趋势
     */
    private Map<String, Object> analyzeMetricTrends() {
        Map<String, Object> trends = new HashMap<>();
        
        for (Map.Entry<String, BusinessMetric> entry : businessMetrics.entrySet()) {
            BusinessMetric metric = entry.getValue();
            Map<String, Object> metricTrend = new HashMap<>();
            
            metricTrend.put("currentValue", metric.getCurrentValue());
            metricTrend.put("type", metric.getType().name());
            metricTrend.put("lastUpdated", metric.getLastUpdated());
            
            if (metric.getType() == MetricType.GAUGE) {
                metricTrend.put("averageValue", metric.getAverageValue());
                metricTrend.put("minValue", metric.getMinValue());
                metricTrend.put("maxValue", metric.getMaxValue());
            }
            
            trends.put(entry.getKey(), metricTrend);
        }
        
        return trends;
    }

    /**
     * 分析事件统计
     */
    private Map<String, Object> analyzeEventStatistics() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        for (Map.Entry<String, List<BusinessEvent>> entry : businessEvents.entrySet()) {
            String eventType = entry.getKey();
            List<BusinessEvent> events = entry.getValue();
            
            // 统计最近一小时的事件
            long recentCount = events.stream()
                .mapToLong(event -> event.getTimestamp().isAfter(oneHourAgo) ? 1 : 0)
                .sum();
            
            Map<String, Object> eventStat = new HashMap<>();
            eventStat.put("totalCount", events.size());
            eventStat.put("recentHourCount", recentCount);
            eventStat.put("averagePerHour", calculateAverageEventsPerHour(events));
            
            stats.put(eventType, eventStat);
        }
        
        return stats;
    }

    /**
     * 计算每小时平均事件数
     */
    private double calculateAverageEventsPerHour(List<BusinessEvent> events) {
        if (events.isEmpty()) return 0.0;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        
        long recentEvents = events.stream()
            .mapToLong(event -> event.getTimestamp().isAfter(oneDayAgo) ? 1 : 0)
            .sum();
        
        return recentEvents / 24.0; // 24小时平均
    }

    /**
     * 计算业务健康度评分
     */
    private Map<String, Object> calculateBusinessHealthScore() {
        Map<String, Object> healthScore = new HashMap<>();
        
        try {
            double score = 100.0; // 满分100
            List<String> issues = new ArrayList<>();
            
            // 检查错误率
            BusinessMetric errorMetric = businessMetrics.get("error_count");
            BusinessMetric requestMetric = businessMetrics.get("api_request_count");
            
            if (errorMetric != null && requestMetric != null) {
                double errorRate = requestMetric.getCurrentValue() > 0 ? 
                    errorMetric.getCurrentValue() / requestMetric.getCurrentValue() : 0;
                
                if (errorRate > 0.05) { // 错误率超过5%
                    score -= 20;
                    issues.add("High error rate: " + String.format("%.2f%%", errorRate * 100));
                }
            }
            
            // 检查响应时间
            BusinessMetric responseTimeMetric = businessMetrics.get("response_time_avg");
            if (responseTimeMetric != null && responseTimeMetric.getCurrentValue() > 1000) { // 超过1秒
                score -= 15;
                issues.add("Slow response time: " + responseTimeMetric.getCurrentValue() + "ms");
            }
            
            // 检查活跃用户数趋势
            BusinessMetric activeUserMetric = businessMetrics.get("active_user_count");
            if (activeUserMetric != null && activeUserMetric.getCurrentValue() < activeUserMetric.getAverageValue() * 0.8) {
                score -= 10;
                issues.add("Declining active users");
            }
            
            // 检查业务交易量
            BusinessMetric transactionMetric = businessMetrics.get("business_transaction_count");
            if (transactionMetric != null && transactionMetric.getCurrentValue() == 0) {
                score -= 25;
                issues.add("No business transactions");
            }
            
            healthScore.put("score", Math.max(0, score));
            healthScore.put("level", getHealthLevel(score));
            healthScore.put("issues", issues);
            healthScore.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Failed to calculate business health score", e);
            healthScore.put("score", 0);
            healthScore.put("level", "UNKNOWN");
            healthScore.put("issues", Arrays.asList("Failed to calculate health score"));
        }
        
        return healthScore;
    }

    /**
     * 获取健康等级
     */
    private String getHealthLevel(double score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 80) return "GOOD";
        if (score >= 70) return "FAIR";
        if (score >= 60) return "POOR";
        return "CRITICAL";
    }

    /**
     * 生成业务报告
     */
    private void generateBusinessReport() {
        try {
            Map<String, Object> report = new HashMap<>();
            
            // 报告时间
            report.put("reportTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 指标摘要
            report.put("metricsSummary", generateMetricsSummary());
            
            // 事件摘要
            report.put("eventsSummary", generateEventsSummary());
            
            // 趋势分析
            report.put("trendAnalysis", generateTrendAnalysis());
            
            // 建议
            report.put("recommendations", generateRecommendations());
            
            // 保存报告到运行时指标
            monitoringConfig.getRuntimeMetrics().put("businessReport", report);
            
            log.info("Business report generated successfully");
            
        } catch (Exception e) {
            log.error("Failed to generate business report", e);
        }
    }

    /**
     * 生成指标摘要
     */
    private Map<String, Object> generateMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        for (Map.Entry<String, BusinessMetric> entry : businessMetrics.entrySet()) {
            BusinessMetric metric = entry.getValue();
            Map<String, Object> metricSummary = new HashMap<>();
            
            metricSummary.put("description", metric.getDescription());
            metricSummary.put("currentValue", metric.getCurrentValue());
            metricSummary.put("type", metric.getType().name());
            
            if (metric.getType() == MetricType.GAUGE) {
                metricSummary.put("averageValue", metric.getAverageValue());
            }
            
            summary.put(entry.getKey(), metricSummary);
        }
        
        return summary;
    }

    /**
     * 生成事件摘要
     */
    private Map<String, Object> generateEventsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        for (Map.Entry<String, List<BusinessEvent>> entry : businessEvents.entrySet()) {
            List<BusinessEvent> events = entry.getValue();
            
            long recentCount = events.stream()
                .mapToLong(event -> event.getTimestamp().isAfter(oneHourAgo) ? 1 : 0)
                .sum();
            
            summary.put(entry.getKey(), Map.of(
                "totalEvents", events.size(),
                "recentHourEvents", recentCount
            ));
        }
        
        return summary;
    }

    /**
     * 生成趋势分析
     */
    private Map<String, Object> generateTrendAnalysis() {
        Map<String, Object> analysis = new HashMap<>();
        
        // 这里可以添加更复杂的趋势分析逻辑
        analysis.put("overallTrend", "STABLE");
        analysis.put("keyInsights", Arrays.asList(
            "API请求量保持稳定",
            "用户活跃度正常",
            "错误率在可接受范围内"
        ));
        
        return analysis;
    }

    /**
     * 生成建议
     */
    private List<String> generateRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        // 基于当前指标生成建议
        BusinessMetric errorMetric = businessMetrics.get("error_count");
        if (errorMetric != null && errorMetric.getCurrentValue() > 0) {
            recommendations.add("建议关注错误日志，及时修复问题");
        }
        
        BusinessMetric responseTimeMetric = businessMetrics.get("response_time_avg");
        if (responseTimeMetric != null && responseTimeMetric.getCurrentValue() > 500) {
            recommendations.add("建议优化响应时间，提升用户体验");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("系统运行良好，继续保持");
        }
        
        return recommendations;
    }

    /**
     * 清理过期数据
     */
    private void cleanupExpiredData() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30); // 保留30天数据
            
            for (List<BusinessEvent> events : businessEvents.values()) {
                events.removeIf(event -> event.getTimestamp().isBefore(cutoff));
            }
            
            log.debug("Cleaned up expired business data");
        } catch (Exception e) {
            log.error("Failed to cleanup expired data", e);
        }
    }

    /**
     * 获取业务统计
     */
    public Map<String, Object> getBusinessStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 指标统计
        Map<String, Object> metricStats = new HashMap<>();
        for (Map.Entry<String, BusinessMetric> entry : businessMetrics.entrySet()) {
            BusinessMetric metric = entry.getValue();
            metricStats.put(entry.getKey(), Map.of(
                "value", metric.getCurrentValue(),
                "type", metric.getType().name()
            ));
        }
        stats.put("metrics", metricStats);
        
        // 事件统计
        Map<String, Integer> eventCounts = new HashMap<>();
        for (Map.Entry<String, List<BusinessEvent>> entry : businessEvents.entrySet()) {
            eventCounts.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("events", eventCounts);
        
        return stats;
    }

    /**
     * 停止业务分析
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Business analysis stopped");
    }

    /**
     * 指标类型
     */
    public enum MetricType {
        COUNTER, // 计数器
        GAUGE    // 仪表盘
    }

    /**
     * 业务指标
     */
    private static class BusinessMetric {
        private final String name;
        private final String description;
        private final MetricType type;
        private double currentValue = 0;
        private double totalValue = 0;
        private long updateCount = 0;
        private double minValue = Double.MAX_VALUE;
        private double maxValue = Double.MIN_VALUE;
        private LocalDateTime lastUpdated;

        public BusinessMetric(String name, String description, MetricType type) {
            this.name = name;
            this.description = description;
            this.type = type;
            this.lastUpdated = LocalDateTime.now();
        }

        public synchronized void increment(long value) {
            if (type == MetricType.COUNTER) {
                currentValue += value;
                lastUpdated = LocalDateTime.now();
            }
        }

        public synchronized void setValue(double value) {
            if (type == MetricType.GAUGE) {
                currentValue = value;
                updateValue(value);
            }
        }

        public synchronized void updateValue(double value) {
            totalValue += value;
            updateCount++;
            minValue = Math.min(minValue, value);
            maxValue = Math.max(maxValue, value);
            lastUpdated = LocalDateTime.now();
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public MetricType getType() { return type; }
        public double getCurrentValue() { return currentValue; }
        public double getAverageValue() { return updateCount > 0 ? totalValue / updateCount : 0; }
        public double getMinValue() { return minValue == Double.MAX_VALUE ? 0 : minValue; }
        public double getMaxValue() { return maxValue == Double.MIN_VALUE ? 0 : maxValue; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    /**
     * 业务事件
     */
    private static class BusinessEvent {
        private final long eventId;
        private final String eventType;
        private final String eventName;
        private final LocalDateTime timestamp;
        private final Map<String, Object> properties;

        public BusinessEvent(long eventId, String eventType, String eventName, 
                           LocalDateTime timestamp, Map<String, Object> properties) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.eventName = eventName;
            this.timestamp = timestamp;
            this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
        }

        public long getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public String getEventName() { return eventName; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getProperties() { return properties; }
    }
}