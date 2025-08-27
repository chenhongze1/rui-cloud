# RUI Common Monitoring

## 概述

RUI Common Monitoring 是 RUI Cloud 微服务框架的监控模块，提供全面的应用监控、指标收集、健康检查和告警功能。

## 功能特性

### 🔍 指标收集
- **JVM指标**: 内存使用、GC统计、线程状态、类加载器信息
- **系统指标**: CPU使用率、内存使用率、磁盘使用率、网络流量
- **HTTP指标**: 请求数量、响应时间、错误率、状态码分布
- **数据库指标**: 连接池状态、查询性能、慢查询监控
- **Redis指标**: 连接状态、命令执行、键空间信息、内存使用
- **业务指标**: 自定义业务指标、用户行为追踪

### 🏥 健康检查
- **数据库健康检查**: 连接状态、查询响应时间
- **Redis健康检查**: 连接状态、响应时间
- **磁盘健康检查**: 磁盘使用率监控
- **内存健康检查**: 内存使用率监控
- **自定义健康检查**: 支持业务自定义健康检查逻辑

### ⚡ 性能监控
- **方法执行时间**: 基于AOP的方法性能监控
- **慢操作检测**: HTTP请求、数据库查询、Redis操作慢操作检测
- **资源使用监控**: 实时监控系统资源使用情况

### 🚨 告警系统
- **规则引擎**: 灵活的告警规则配置
- **多渠道告警**: 邮件、短信、Webhook、钉钉等多种告警方式
- **告警抑制**: 防止告警风暴，支持冷却期和频率限制
- **告警分级**: 支持信息、警告、错误、严重等多个级别

### 📊 可视化端点
- **自定义指标端点**: `/actuator/custom-metrics`
- **自定义健康检查端点**: `/actuator/custom-health`
- **与Spring Boot Actuator完美集成**

## 快速开始

### 1. 添加依赖

在你的项目 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-monitoring</artifactId>
    <version>${rui.version}</version>
</dependency>
```

### 2. 启用监控

在 `application.yml` 中添加配置：

```yaml
rui:
  monitoring:
    enabled: true
```

或者直接引入预设配置：

```yaml
spring:
  profiles:
    include: monitoring
```

### 3. 访问监控端点

启动应用后，可以访问以下端点：

- 健康检查: `http://localhost:8080/actuator/health`
- 自定义健康检查: `http://localhost:8080/actuator/custom-health`
- 指标信息: `http://localhost:8080/actuator/metrics`
- 自定义指标: `http://localhost:8080/actuator/custom-metrics`

## 配置说明

### 基础配置

```yaml
rui:
  monitoring:
    enabled: true  # 是否启用监控
    
    # 指标收集配置
    metrics:
      enabled: true
      export-interval: 30s  # 指标导出间隔
      retention-period: 7d  # 指标保留期
      enabled-metrics:      # 启用的指标类型
        - jvm
        - system
        - http
        - database
        - redis
        - business
```

### 健康检查配置

```yaml
rui:
  monitoring:
    health:
      enabled: true
      check-interval: 30s   # 检查间隔
      timeout: 10s          # 超时时间
      enabled-checkers:     # 启用的检查器
        - database
        - redis
        - disk
        - memory
      
      # 磁盘健康检查
      disk:
        warning-threshold: 0.8  # 警告阈值 80%
        error-threshold: 0.9    # 错误阈值 90%
```

### 告警配置

```yaml
rui:
  monitoring:
    alert:
      enabled: true
      rules:
        - name: "CPU使用率过高"
          metric: "cpu_usage"
          condition: ">"
          threshold: 80.0
          duration: 5m
          severity: "warning"
      
      channels:
        email:
          enabled: true
          recipients:
            - "admin@example.com"
```

## 自定义扩展

### 自定义指标

```java
@Component
public class CustomBusinessMetrics {
    
    @Autowired
    private MetricsCollector metricsCollector;
    
    public void recordOrderCount(int count) {
        metricsCollector.recordCustomMetric("business.order.count", count);
    }
    
    public void recordPaymentAmount(double amount) {
        metricsCollector.recordCustomMetric("business.payment.amount", amount);
    }
}
```

### 自定义健康检查

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // 自定义健康检查逻辑
        boolean isHealthy = checkCustomService();
        
        if (isHealthy) {
            return Health.up()
                .withDetail("custom.service", "运行正常")
                .build();
        } else {
            return Health.down()
                .withDetail("custom.service", "服务异常")
                .build();
        }
    }
    
    private boolean checkCustomService() {
        // 实现自定义检查逻辑
        return true;
    }
}
```

### 自定义告警规则

```java
@Component
public class CustomAlertRules {
    
    @Autowired
    private AlertRuleEngine alertRuleEngine;
    
    @EventListener
    public void onCustomEvent(CustomBusinessEvent event) {
        // 检查自定义业务指标
        double businessMetric = event.getMetricValue();
        alertRuleEngine.checkBusinessMetricRule("custom.business.metric", businessMetric);
    }
}
```

## 集成第三方监控

### Prometheus 集成

添加 Prometheus 依赖：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

配置 Prometheus 端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "prometheus"
  metrics:
    export:
      prometheus:
        enabled: true
```

### Grafana 仪表板

可以使用预设的 Grafana 仪表板模板来可视化监控数据。仪表板配置文件位于 `src/main/resources/grafana/` 目录。

## 最佳实践

### 1. 监控策略
- **分层监控**: 基础设施 → 应用 → 业务
- **关键指标**: 专注于影响用户体验的核心指标
- **告警分级**: 合理设置告警级别，避免告警疲劳

### 2. 性能优化
- **异步处理**: 监控数据收集和处理使用异步方式
- **批量操作**: 指标数据批量导出，减少I/O开销
- **缓存策略**: 合理使用缓存，减少重复计算

### 3. 安全考虑
- **敏感信息**: 避免在监控数据中暴露敏感信息
- **访问控制**: 限制监控端点的访问权限
- **数据加密**: 传输和存储监控数据时使用加密

## 故障排查

### 常见问题

1. **监控数据不更新**
   - 检查 `rui.monitoring.enabled` 配置
   - 确认相关组件的健康状态
   - 查看应用日志中的错误信息

2. **告警不生效**
   - 验证告警规则配置
   - 检查告警渠道配置
   - 确认告警抑制设置

3. **性能影响**
   - 调整指标收集频率
   - 优化自定义监控逻辑
   - 检查监控数据存储

### 调试模式

启用调试日志：

```yaml
logging:
  level:
    com.rui.common.monitoring: DEBUG
```

## 版本历史

- **v1.0.0**: 初始版本，提供基础监控功能
- **v1.1.0**: 增加告警系统和自定义指标支持
- **v1.2.0**: 优化性能监控和健康检查

## 贡献指南

欢迎提交 Issue 和 Pull Request 来改进这个模块。请确保：

1. 遵循项目的编码规范
2. 添加适当的测试用例
3. 更新相关文档

## 许可证

本项目采用 MIT 许可证，详情请参阅 [LICENSE](../../../LICENSE) 文件。