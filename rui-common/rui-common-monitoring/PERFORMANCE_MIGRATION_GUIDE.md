# 性能监控功能迁移指南

## 概述

性能监控功能已从 `rui-common-log` 模块迁移到 `rui-common-monitoring` 模块，以实现更好的功能分离和模块化设计。

## 迁移对照表

### 1. 注解迁移

**旧方式 (已废弃):**
```java
@Logged(type = LogType.PERFORMANCE)
public void someMethod() {
    // 业务逻辑
}
```

**新方式:**
```java
@PerformanceMonitored(operation = "someOperation", module = "someModule")
public void someMethod() {
    // 业务逻辑
}
```

### 2. 编程式API迁移

**旧方式 (已废弃):**
```java
@Autowired
private LogManager logManager;

public void someMethod() {
    long startTime = System.currentTimeMillis();
    try {
        // 业务逻辑
    } finally {
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("customMetric", "value");
        logManager.performance("operation", duration, metrics);
    }
}
```

**新方式:**
```java
@Autowired
private PerformanceMonitoringService performanceService;

public void someMethod() {
    long startTime = System.currentTimeMillis();
    try {
        // 业务逻辑
    } finally {
        long duration = System.currentTimeMillis() - startTime;
        Map<String, String> tags = new HashMap<>();
        tags.put("customTag", "value");
        performanceService.recordMethodPerformance("operation", "module", duration, tags);
    }
}
```

### 3. 兼容性适配器

为了平滑迁移，提供了兼容性适配器 `LogPerformanceAdapter`：

```java
@Autowired
private LogPerformanceAdapter adapter;

public void someMethod() {
    // 使用适配器保持原有API调用方式
    adapter.performance("operation", duration, metrics);
}
```

## 配置迁移

### 旧配置 (application.yml)
```yaml
rui:
  logging:
    enabled: true
    slowLogThreshold: 1000
```

### 新配置 (application.yml)
```yaml
rui:
  monitoring:
    performance:
      enabled: true
      slowThreshold: 1000
      detailedMonitoring: true
```

## 依赖更新

### Maven 依赖

**添加新依赖:**
```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-monitoring</artifactId>
    <version>${rui.version}</version>
</dependency>
```

**保留日志依赖 (用于其他日志功能):**
```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-log</artifactId>
    <version>${rui.version}</version>
</dependency>
```

## 新功能特性

### 1. 增强的注解配置

```java
@PerformanceMonitored(
    operation = "userQuery",
    module = "userService",
    slowThreshold = 2000,
    includeParameters = true,
    detailedMonitoring = true,
    customTags = {"service=user", "version=v1"}
)
public List<User> queryUsers(String keyword) {
    // 业务逻辑
}
```

### 2. 丰富的监控级别

```java
@PerformanceMonitored(
    operation = "criticalOperation",
    level = MonitoringLevel.DETAILED  // BASIC, STANDARD, DETAILED
)
public void criticalMethod() {
    // 业务逻辑
}
```

### 3. 集成告警功能

性能监控现在与告警系统集成，可以自动触发慢操作告警。

### 4. 更丰富的指标收集

- 方法执行时间
- 调用次数统计
- 慢操作检测
- 错误率统计
- 自定义标签支持

## 迁移步骤

1. **添加依赖**: 在项目中添加 `rui-common-monitoring` 依赖
2. **更新注解**: 将 `@Logged(type = LogType.PERFORMANCE)` 替换为 `@PerformanceMonitored`
3. **更新API调用**: 将 `LogManager.performance()` 调用替换为 `PerformanceMonitoringService` 的相应方法
4. **更新配置**: 迁移性能监控相关配置到新的配置路径
5. **测试验证**: 确保迁移后功能正常工作
6. **清理代码**: 移除对废弃API的调用

## 注意事项

1. **向后兼容**: 旧的API暂时保留但已标记为废弃，建议尽快迁移
2. **配置隔离**: 新的性能监控配置独立于日志配置
3. **功能增强**: 新模块提供了更丰富的监控功能和更好的性能
4. **告警集成**: 性能监控现在可以直接触发告警，无需额外配置

## 常见问题

### Q: 迁移后原有的性能日志还能看到吗？
A: 是的，新的监控模块会继续记录性能日志，格式更加标准化。

### Q: 可以同时使用新旧API吗？
A: 可以，但不推荐。建议统一使用新的API以获得更好的功能和性能。

### Q: 迁移会影响现有的业务日志吗？
A: 不会，业务日志功能保持不变，只有性能监控功能被迁移。

### Q: 新模块的性能开销如何？
A: 新模块经过优化，性能开销更低，且提供了更精细的控制选项。

## 技术支持

如果在迁移过程中遇到问题，请联系技术团队或查看相关文档。