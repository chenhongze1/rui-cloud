# RUI Common Log

## 模块简介

RUI框架的日志管理模块，提供统一的日志记录、操作日志、访问日志、性能监控等功能，支持多种日志输出方式和自定义日志策略。

## 主要功能

### 1. 日志注解
- **@Logged**: 通用日志记录注解
- **@OperationLog**: 操作日志注解，记录用户操作行为

### 2. 日志切面
- **LoggingAspect**: 通用日志切面
- **OperationLogAspect**: 操作日志切面

### 3. 日志过滤器
- **AccessLogFilter**: 访问日志过滤器，记录HTTP请求信息

### 4. 日志拦截器
- **LoggingInterceptor**: 日志拦截器

### 5. 日志服务
- **LogService**: 日志服务接口
- **LogServiceImpl**: 日志服务实现
- **LogManager**: 日志管理器

### 6. 日志实体
- **LogInfo**: 日志信息实体

### 7. 工具类
- **LogUtils**: 日志工具类

### 8. 自动配置
- **LogAutoConfiguration**: 日志自动配置
- **LogProperties**: 日志配置属性

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-log</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 通用日志记录

```java
@Service
public class UserService {
    
    @Logged(value = "获取用户信息", includeArgs = true, includeResult = true)
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
    
    @Logged(value = "创建用户", level = LogLevel.INFO)
    public void createUser(User user) {
        userMapper.insert(user);
    }
}
```

### 3. 操作日志记录

```java
@RestController
public class UserController {
    
    @PostMapping("/users")
    @OperationLog(
        module = "用户管理",
        operation = "创建用户",
        description = "创建新用户: #{#user.username}"
    )
    public R<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return R.ok(createdUser);
    }
    
    @DeleteMapping("/users/{id}")
    @OperationLog(
        module = "用户管理",
        operation = "删除用户",
        description = "删除用户ID: #{#id}",
        riskLevel = RiskLevel.HIGH
    )
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }
}
```

### 4. 手动记录日志

```java
@Service
public class OrderService {
    
    public void processOrder(Order order) {
        // 记录业务日志
        LogUtils.info("开始处理订单", "orderId", order.getId());
        
        try {
            // 业务逻辑
            doProcessOrder(order);
            
            // 记录成功日志
            LogUtils.success("订单处理成功", "orderId", order.getId());
            
        } catch (Exception e) {
            // 记录错误日志
            LogUtils.error("订单处理失败", e, "orderId", order.getId());
            throw e;
        }
    }
}
```

### 5. 性能监控

```java
@Service
public class DataService {
    
    @Logged(value = "数据查询", monitorPerformance = true, slowThreshold = 1000)
    public List<Data> queryData(QueryRequest request) {
        // 查询逻辑
        return dataMapper.selectList(request);
    }
}
```

## 配置属性

```yaml
rui:
  log:
    # 是否启用日志功能
    enabled: true
    # 日志级别
    level: INFO
    # 是否启用访问日志
    access-log-enabled: true
    # 是否启用操作日志
    operation-log-enabled: true
    # 是否启用性能监控
    performance-monitor-enabled: true
    # 慢查询阈值（毫秒）
    slow-threshold: 1000
    # 日志输出配置
    output:
      # 控制台输出
      console-enabled: true
      # 文件输出
      file-enabled: true
      # 数据库输出
      database-enabled: false
      # 消息队列输出
      mq-enabled: false
    # 文件配置
    file:
      # 日志文件路径
      path: "./logs"
      # 日志文件名模式
      name-pattern: "rui-log-%d{yyyy-MM-dd}.log"
      # 最大文件大小
      max-size: "100MB"
      # 保留天数
      max-history: 30
    # 数据库配置
    database:
      # 表名
      table-name: "sys_log"
      # 批量插入大小
      batch-size: 100
      # 异步处理
      async-enabled: true
    # 敏感信息过滤
    sensitive:
      # 是否启用敏感信息过滤
      enabled: true
      # 敏感字段列表
      fields:
        - password
        - token
        - secret
        - key
      # 替换字符
      replacement: "***"
```

## 日志级别说明

### 1. TRACE
- 最详细的日志信息
- 用于调试和问题排查

### 2. DEBUG
- 调试信息
- 开发和测试环境使用

### 3. INFO
- 一般信息
- 记录重要的业务流程

### 4. WARN
- 警告信息
- 潜在的问题或异常情况

### 5. ERROR
- 错误信息
- 系统错误和异常

## 日志格式

### 1. 访问日志格式
```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "traceId": "abc123def456",
  "method": "POST",
  "uri": "/api/users",
  "params": "{\"name\":\"张三\"}",
  "ip": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "userId": 1001,
  "username": "admin",
  "duration": 156,
  "status": 200,
  "response": "{\"code\":200,\"msg\":\"success\"}"
}
```

### 2. 操作日志格式
```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "traceId": "abc123def456",
  "module": "用户管理",
  "operation": "创建用户",
  "description": "创建新用户: 张三",
  "method": "com.rui.user.controller.UserController.createUser",
  "args": "[{\"name\":\"张三\"}]",
  "result": "{\"id\":1001,\"name\":\"张三\"}",
  "userId": 1001,
  "username": "admin",
  "ip": "192.168.1.100",
  "duration": 156,
  "success": true,
  "riskLevel": "MEDIUM"
}
```

## 高级功能

### 1. 自定义日志处理器

```java
@Component
public class CustomLogHandler implements LogHandler {
    
    @Override
    public void handle(LogInfo logInfo) {
        // 自定义日志处理逻辑
        // 例如：发送到外部系统、特殊格式化等
    }
}
```

### 2. 日志事件监听

```java
@Component
public class LogEventListener {
    
    @EventListener
    public void handleOperationLog(OperationLogEvent event) {
        // 处理操作日志事件
        if (event.getRiskLevel() == RiskLevel.HIGH) {
            // 高风险操作告警
            alertService.sendAlert(event);
        }
    }
    
    @EventListener
    public void handleSlowQuery(SlowQueryEvent event) {
        // 处理慢查询事件
        performanceService.recordSlowQuery(event);
    }
}
```

### 3. 日志脱敏

```java
@Component
public class LogDesensitizer {
    
    public String desensitize(String content, String fieldName) {
        // 根据字段名进行脱敏处理
        if ("phone".equals(fieldName)) {
            return content.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        }
        return content;
    }
}
```

### 4. 日志压缩和归档

```yaml
rui:
  log:
    archive:
      # 是否启用归档
      enabled: true
      # 归档策略：SIZE, TIME, SIZE_AND_TIME
      strategy: SIZE_AND_TIME
      # 文件大小阈值
      max-file-size: "100MB"
      # 时间阈值（天）
      max-days: 7
      # 压缩格式：GZIP, ZIP
      compression: GZIP
      # 归档目录
      archive-dir: "./logs/archive"
```

## 性能优化

### 1. 异步日志
```yaml
rui:
  log:
    async:
      # 是否启用异步日志
      enabled: true
      # 队列大小
      queue-size: 10000
      # 线程池大小
      thread-pool-size: 4
      # 批量处理大小
      batch-size: 100
```

### 2. 日志采样
```yaml
rui:
  log:
    sampling:
      # 是否启用采样
      enabled: true
      # 采样率（0.0-1.0）
      rate: 0.1
      # 采样策略：RANDOM, HASH, TIME
      strategy: RANDOM
```

## 监控和告警

### 1. 日志指标
- 日志产生速率
- 错误日志比例
- 慢查询统计
- 存储空间使用

### 2. 告警规则
- 错误日志激增
- 慢查询超阈值
- 存储空间不足
- 日志处理延迟

## 注意事项

1. **性能影响**: 日志记录会影响性能，合理配置日志级别
2. **存储空间**: 注意日志文件的大小和保留策略
3. **敏感信息**: 避免记录敏感信息，启用脱敏功能
4. **异步处理**: 对于高并发场景，建议启用异步日志
5. **监控告警**: 建立完善的日志监控和告警机制

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x
- Logback版本: 1.4.x