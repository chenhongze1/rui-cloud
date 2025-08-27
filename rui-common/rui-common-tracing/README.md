# RUI Common Tracing

基于OpenTelemetry的分布式链路追踪模块，提供完整的链路追踪解决方案。

## 功能特性

### 核心功能
- **自动追踪**: 基于注解和AOP的自动链路追踪
- **HTTP追踪**: 自动追踪HTTP请求和响应
- **数据库追踪**: 自动追踪数据库操作
- **缓存追踪**: 自动追踪缓存操作
- **消息队列追踪**: 自动追踪消息发送和接收
- **异常追踪**: 自动记录和追踪异常信息

### 导出支持
- **Jaeger**: 支持Jaeger分布式追踪系统
- **Zipkin**: 支持Zipkin分布式追踪系统
- **OTLP**: 支持OpenTelemetry Protocol
- **日志**: 支持将追踪信息输出到日志

### 高级特性
- **采样控制**: 支持多种采样策略
- **上下文传播**: 支持多种传播协议
- **性能监控**: 提供性能指标和慢请求检测
- **自定义标签**: 支持自定义标签和属性
- **批量导出**: 支持批量导出优化性能

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-tracing</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 启用追踪

在启动类上添加注解：

```java
@SpringBootApplication
@EnableTracing
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 配置文件

```yaml
rui:
  tracing:
    enabled: true
    service-name: my-service
    export:
      type: jaeger
      endpoint: http://localhost:14268/api/traces
```

## 使用方式

### 注解追踪

使用`@Traced`注解自动追踪方法：

```java
@Service
public class UserService {
    
    @Traced("user.create")
    public User createUser(User user) {
        // 业务逻辑
        return userRepository.save(user);
    }
    
    @Traced(value = "user.query", recordArgs = true, recordResult = true)
    public User findById(Long id) {
        return userRepository.findById(id);
    }
}
```

### 手动追踪

使用TracingUtils进行手动追踪：

```java
@Service
public class OrderService {
    
    public void processOrder(Order order) {
        // 创建新的Span
        TracingUtils.trace("order.process", () -> {
            // 添加标签
            TracingUtils.addTag("order.id", order.getId().toString());
            TracingUtils.addTag("order.amount", order.getAmount());
            
            // 业务逻辑
            validateOrder(order);
            saveOrder(order);
            sendNotification(order);
            
            return null;
        });
    }
    
    private void validateOrder(Order order) {
        TracingUtils.trace("order.validate", () -> {
            // 验证逻辑
            if (order.getAmount() <= 0) {
                TracingUtils.markError("Invalid order amount");
                throw new IllegalArgumentException("Order amount must be positive");
            }
        });
    }
}
```

### HTTP追踪

HTTP请求会自动被追踪，包含以下信息：
- 请求方法和URL
- 请求头和响应头
- 状态码和响应时间
- 客户端IP地址

### 数据库追踪

数据库操作会自动被追踪：

```java
@Repository
public class UserRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public User findById(Long id) {
        // 自动追踪SQL执行
        return jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE id = ?", 
            new Object[]{id}, 
            new UserRowMapper()
        );
    }
}
```

### 缓存追踪

缓存操作会自动被追踪：

```java
@Service
public class CacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public Object get(String key) {
        // 自动追踪缓存操作
        return redisTemplate.opsForValue().get(key);
    }
}
```

## 配置说明

### 基础配置

```yaml
rui:
  tracing:
    # 是否启用追踪
    enabled: true
    # 服务名称
    service-name: my-service
    # 服务版本
    service-version: 1.0.0
```

### 采样配置

```yaml
rui:
  tracing:
    sampling:
      # 采样率 (0.0-1.0)
      rate: 1.0
      # 采样类型
      type: ratio
      # 限流采样每秒最大Span数
      max-spans-per-second: 100
```

### 导出配置

#### Jaeger导出

```yaml
rui:
  tracing:
    export:
      type: jaeger
      endpoint: http://localhost:14268/api/traces
```

#### Zipkin导出

```yaml
rui:
  tracing:
    export:
      type: zipkin
      endpoint: http://localhost:9411/api/v2/spans
```

#### OTLP导出

```yaml
rui:
  tracing:
    export:
      type: otlp
      endpoint: http://localhost:4317
```

### 忽略配置

```yaml
rui:
  tracing:
    ignore-patterns:
      - "/health"
      - "/metrics"
      - "/actuator/**"
      - "/static/**"
```

### 自定义标签

```yaml
rui:
  tracing:
    tags:
      application: my-app
      version: 1.0.0
      environment: production
```

## 高级用法

### 自定义Span

```java
@Service
public class PaymentService {
    
    @Autowired
    private TracingManager tracingManager;
    
    public void processPayment(Payment payment) {
        Span span = tracingManager.startSpan("payment.process", SpanKind.INTERNAL);
        try {
            // 添加属性
            span.setAttribute("payment.id", payment.getId());
            span.setAttribute("payment.amount", payment.getAmount());
            span.setAttribute("payment.currency", payment.getCurrency());
            
            // 业务逻辑
            validatePayment(payment);
            chargePayment(payment);
            
            // 标记成功
            span.setStatus(StatusCode.OK);
            
        } catch (Exception e) {
            // 记录异常
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            tracingManager.finishSpan(span);
        }
    }
}
```

### 跨服务追踪

```java
@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private TracingManager tracingManager;
    
    public void createOrder(Order order) {
        // 获取当前追踪上下文
        Map<String, String> traceContext = TracingUtils.getTraceContext();
        
        // 创建HTTP头
        HttpHeaders headers = new HttpHeaders();
        traceContext.forEach(headers::set);
        
        // 发送请求
        HttpEntity<Order> entity = new HttpEntity<>(order, headers);
        restTemplate.postForObject("/api/orders", entity, Order.class);
    }
}
```

### 异步追踪

```java
@Service
public class AsyncService {
    
    @Async
    @Traced("async.process")
    public CompletableFuture<String> processAsync(String data) {
        // 异步处理逻辑
        return CompletableFuture.completedFuture("processed: " + data);
    }
}
```

## 监控和观察

### 健康检查

模块提供健康检查端点：

```
GET /actuator/health/tracing
```

### 指标监控

提供以下指标：
- `tracing.spans.created`: 创建的Span总数
- `tracing.spans.active`: 当前活跃的Span数
- `tracing.spans.exported`: 导出的Span总数
- `tracing.export.duration`: 导出耗时

### 日志集成

追踪信息会自动添加到日志MDC中：

```
2023-12-01 10:30:45.123 [http-nio-8080-exec-1] INFO [1234567890abcdef,fedcba0987654321] com.example.UserController - Processing user request
```

## 性能优化

### 采样策略

```yaml
rui:
  tracing:
    sampling:
      # 生产环境建议使用较低的采样率
      rate: 0.1
      type: ratio
```

### 批量导出

```yaml
rui:
  tracing:
    export:
      batch:
        # 增加批处理大小
        size: 1024
        # 减少导出频率
        timeout: 5000
```

### 忽略静态资源

```yaml
rui:
  tracing:
    ignore-patterns:
      - "/static/**"
      - "/css/**"
      - "/js/**"
      - "/images/**"
```

## 故障排除

### 常见问题

1. **追踪信息不显示**
   - 检查`rui.tracing.enabled`是否为true
   - 检查导出配置是否正确
   - 检查网络连接

2. **性能影响**
   - 降低采样率
   - 增加批处理大小
   - 忽略不必要的请求

3. **内存占用过高**
   - 检查活跃Span数量
   - 调整队列大小
   - 检查是否有Span泄漏

### 调试模式

```yaml
logging:
  level:
    com.rui.common.tracing: DEBUG
    io.opentelemetry: DEBUG
```

## 最佳实践

1. **合理使用采样**: 生产环境使用适当的采样率
2. **标签规范**: 使用有意义的标签名称和值
3. **异常处理**: 确保异常被正确记录
4. **资源清理**: 确保Span被正确关闭
5. **性能监控**: 定期监控追踪系统的性能影响

## 版本兼容性

- Spring Boot 3.x
- OpenTelemetry 1.32+
- Java 21+

## 更新日志

### v1.0.0
- 初始版本发布
- 支持基础链路追踪功能
- 支持多种导出格式
- 提供自动配置