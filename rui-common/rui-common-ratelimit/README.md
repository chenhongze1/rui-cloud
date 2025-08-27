# RUI Common Rate Limit

## 模块简介

RUI框架的限流模块，基于Redis+Lua脚本实现，提供高性能的分布式限流功能，支持多种限流算法和策略，保护系统免受流量冲击，确保服务稳定性。

## 主要功能

### 1. 限流注解
- **@RateLimit**: 限流注解，支持方法级别的流量控制

### 2. 限流类型
- **IP**: 基于IP地址限流
- **USER**: 基于用户ID限流
- **GLOBAL**: 全局限流
- **CUSTOM**: 自定义Key限流
- **API**: 基于API接口限流
- **TENANT**: 基于租户限流

### 3. 限流算法
- **令牌桶算法**: 平滑限流，支持突发流量
- **滑动窗口算法**: 精确限流，防止突发流量
- **固定窗口算法**: 简单高效，适用于一般场景
- **漏桶算法**: 恒定速率输出，适用于流量整形

### 4. 核心组件
- **RateLimitAspect**: 限流切面处理
- **RateLimitService**: 限流服务接口
- **RedisRateLimitServiceImpl**: 基于Redis的实现
- **RateLimitAutoConfiguration**: 自动配置类
- **RateLimitProperties**: 配置属性类

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-ratelimit</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Redis依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Spring Boot AOP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 2. 注解式使用

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    /**
     * 基于IP限流
     */
    @RateLimit(
        key = "api:login",
        limitType = LimitType.IP,
        count = 5,           // 5次
        time = 60,           // 60秒
        message = "登录过于频繁，请稍后再试"
    )
    @PostMapping("/login")
    public R<String> login(@RequestBody LoginRequest request) {
        // 登录逻辑
        String token = authService.login(request);
        return R.ok(token);
    }
    
    /**
     * 基于用户限流
     */
    @RateLimit(
        key = "api:order:create",
        limitType = LimitType.USER,
        count = 10,          // 10次
        time = 60,           // 60秒
        algorithm = RateLimitAlgorithm.TOKEN_BUCKET
    )
    @PostMapping("/orders")
    public R<Order> createOrder(@RequestBody OrderRequest request) {
        // 创建订单逻辑
        Order order = orderService.createOrder(request);
        return R.ok(order);
    }
    
    /**
     * 全局限流
     */
    @RateLimit(
        key = "api:global",
        limitType = LimitType.GLOBAL,
        count = 1000,        // 1000次
        time = 60,           // 60秒
        algorithm = RateLimitAlgorithm.SLIDING_WINDOW
    )
    @GetMapping("/products")
    public R<List<Product>> getProducts() {
        // 获取商品列表
        List<Product> products = productService.getProducts();
        return R.ok(products);
    }
    
    /**
     * 自定义Key限流
     */
    @RateLimit(
        key = "api:payment:#{#request.merchantId}",
        limitType = LimitType.CUSTOM,
        count = 100,         // 100次
        time = 60,           // 60秒
        algorithm = RateLimitAlgorithm.LEAKY_BUCKET
    )
    @PostMapping("/payment")
    public R<PaymentResult> processPayment(@RequestBody PaymentRequest request) {
        // 支付处理逻辑
        PaymentResult result = paymentService.process(request);
        return R.ok(result);
    }
    
    /**
     * API接口限流
     */
    @RateLimit(
        key = "api:upload",
        limitType = LimitType.API,
        count = 20,          // 20次
        time = 60,           // 60秒
        fileSize = 10485760, // 10MB
        message = "文件上传过于频繁"
    )
    @PostMapping("/upload")
    public R<String> uploadFile(@RequestParam("file") MultipartFile file) {
        // 文件上传逻辑
        String url = fileService.upload(file);
        return R.ok(url);
    }
    
    /**
     * 租户限流
     */
    @RateLimit(
        key = "api:tenant:#{#tenantId}",
        limitType = LimitType.TENANT,
        count = 500,         // 500次
        time = 60,           // 60秒
        algorithm = RateLimitAlgorithm.FIXED_WINDOW
    )
    @GetMapping("/tenant/{tenantId}/data")
    public R<List<Data>> getTenantData(@PathVariable String tenantId) {
        // 获取租户数据
        List<Data> data = dataService.getTenantData(tenantId);
        return R.ok(data);
    }
    
    /**
     * 分级限流
     */
    @RateLimit(
        key = "api:vip:#{#userId}",
        limitType = LimitType.USER,
        count = 100,         // VIP用户100次
        time = 60,
        fallbackCount = 10,  // 普通用户10次
        condition = "#{@userService.isVip(#userId)}"
    )
    @GetMapping("/vip/data")
    public R<List<VipData>> getVipData(@RequestParam Long userId) {
        // VIP数据获取
        List<VipData> data = vipService.getData(userId);
        return R.ok(data);
    }
    
    /**
     * 动态限流
     */
    @RateLimit(
        key = "api:dynamic",
        limitType = LimitType.GLOBAL,
        count = 0,           // 动态计算
        time = 60,
        dynamic = true,      // 启用动态限流
        dynamicExpression = "#{@rateLimitCalculator.calculate()}"
    )
    @GetMapping("/dynamic")
    public R<String> dynamicLimit() {
        // 动态限流接口
        return R.ok("success");
    }
    
    /**
     * 预热限流
     */
    @RateLimit(
        key = "api:warmup",
        limitType = LimitType.GLOBAL,
        count = 1000,
        time = 60,
        warmupPeriod = 300,  // 5分钟预热期
        warmupCount = 100    // 预热期间限制100次
    )
    @GetMapping("/warmup")
    public R<String> warmupApi() {
        // 需要预热的接口
        return R.ok("success");
    }
}
```

### 3. 编程式使用

```java
@Service
public class BusinessService {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * 编程式限流
     */
    public boolean processRequest(String userId, String operation) {
        String key = "business:" + operation + ":" + userId;
        
        // 检查限流
        if (!rateLimitService.tryAcquire(key, 10, 60, LimitType.USER)) {
            throw new RateLimitException("操作过于频繁，请稍后再试");
        }
        
        // 执行业务逻辑
        doBusinessLogic(userId, operation);
        return true;
    }
    
    /**
     * 令牌桶限流
     */
    public boolean tokenBucketLimit(String key) {
        return rateLimitService.tryAcquireWithTokenBucket(
            key, 
            100,    // 桶容量
            10,     // 每秒补充令牌数
            1       // 消耗令牌数
        );
    }
    
    /**
     * 滑动窗口限流
     */
    public boolean slidingWindowLimit(String key) {
        return rateLimitService.tryAcquireWithSlidingWindow(
            key,
            100,    // 窗口大小内允许的请求数
            60,     // 窗口大小（秒）
            10      // 窗口分片数
        );
    }
    
    /**
     * 漏桶限流
     */
    public boolean leakyBucketLimit(String key) {
        return rateLimitService.tryAcquireWithLeakyBucket(
            key,
            100,    // 桶容量
            10,     // 每秒漏出速率
            1       // 请求大小
        );
    }
    
    /**
     * 批量限流检查
     */
    public Map<String, Boolean> batchLimit(List<String> keys) {
        Map<String, RateLimitConfig> configs = new HashMap<>();
        for (String key : keys) {
            configs.put(key, RateLimitConfig.builder()
                .count(10)
                .time(60)
                .limitType(LimitType.CUSTOM)
                .build());
        }
        
        return rateLimitService.tryAcquireBatch(configs);
    }
    
    /**
     * 获取限流状态
     */
    public RateLimitStatus getLimitStatus(String key) {
        return rateLimitService.getStatus(key);
    }
    
    /**
     * 重置限流计数
     */
    public void resetLimit(String key) {
        rateLimitService.reset(key);
    }
    
    /**
     * 预扣限流配额
     */
    public boolean preDeductQuota(String key, int count) {
        return rateLimitService.preDeduct(key, count, 60, LimitType.CUSTOM);
    }
    
    /**
     * 释放预扣配额
     */
    public void releaseQuota(String key, int count) {
        rateLimitService.releasePreDeduct(key, count);
    }
}
```

### 4. 自定义限流策略

```java
@Component
public class CustomRateLimitStrategy implements RateLimitStrategy {
    
    @Override
    public boolean tryAcquire(String key, RateLimitConfig config) {
        // 自定义限流逻辑
        if (isHighPriorityUser(key)) {
            // 高优先级用户放宽限制
            return tryAcquireForHighPriority(key, config);
        } else if (isSystemBusy()) {
            // 系统繁忙时收紧限制
            return tryAcquireForBusySystem(key, config);
        } else {
            // 正常限流逻辑
            return tryAcquireNormal(key, config);
        }
    }
    
    private boolean isHighPriorityUser(String key) {
        // 判断是否为高优先级用户
        return key.contains("vip") || key.contains("premium");
    }
    
    private boolean isSystemBusy() {
        // 判断系统是否繁忙
        return systemMetrics.getCpuUsage() > 80 || 
               systemMetrics.getMemoryUsage() > 85;
    }
    
    private boolean tryAcquireForHighPriority(String key, RateLimitConfig config) {
        // 高优先级用户限流逻辑
        RateLimitConfig vipConfig = config.toBuilder()
            .count(config.getCount() * 2)  // 双倍配额
            .build();
        return defaultStrategy.tryAcquire(key, vipConfig);
    }
    
    private boolean tryAcquireForBusySystem(String key, RateLimitConfig config) {
        // 系统繁忙时限流逻辑
        RateLimitConfig busyConfig = config.toBuilder()
            .count(config.getCount() / 2)  // 减半配额
            .build();
        return defaultStrategy.tryAcquire(key, busyConfig);
    }
    
    private boolean tryAcquireNormal(String key, RateLimitConfig config) {
        // 正常限流逻辑
        return defaultStrategy.tryAcquire(key, config);
    }
}
```

### 5. 限流事件监听

```java
@Component
@EventListener
public class RateLimitEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitEventListener.class);
    
    /**
     * 限流触发事件
     */
    @EventListener
    public void onRateLimitTriggered(RateLimitTriggeredEvent event) {
        log.warn("限流触发: key={}, limitType={}, count={}, time={}, ip={}",
            event.getKey(),
            event.getLimitType(),
            event.getCount(),
            event.getTime(),
            event.getIpAddress());
        
        // 发送告警
        alertService.sendRateLimitAlert(event);
        
        // 记录限流日志
        rateLimitLogService.recordLimit(event);
    }
    
    /**
     * 限流恢复事件
     */
    @EventListener
    public void onRateLimitRecovered(RateLimitRecoveredEvent event) {
        log.info("限流恢复: key={}, duration={}ms",
            event.getKey(),
            event.getDuration());
    }
    
    /**
     * 限流配置变更事件
     */
    @EventListener
    public void onRateLimitConfigChanged(RateLimitConfigChangedEvent event) {
        log.info("限流配置变更: key={}, oldConfig={}, newConfig={}",
            event.getKey(),
            event.getOldConfig(),
            event.getNewConfig());
        
        // 刷新缓存
        rateLimitConfigCache.refresh(event.getKey());
    }
    
    /**
     * 限流异常事件
     */
    @EventListener
    public void onRateLimitException(RateLimitExceptionEvent event) {
        log.error("限流异常: key={}, error={}",
            event.getKey(),
            event.getException().getMessage(),
            event.getException());
        
        // 发送异常告警
        alertService.sendExceptionAlert(event);
    }
}
```

### 6. 动态配置管理

```java
@RestController
@RequestMapping("/admin/ratelimit")
public class RateLimitAdminController {
    
    @Autowired
    private RateLimitConfigManager configManager;
    
    /**
     * 获取限流配置
     */
    @GetMapping("/config/{key}")
    public R<RateLimitConfig> getConfig(@PathVariable String key) {
        RateLimitConfig config = configManager.getConfig(key);
        return R.ok(config);
    }
    
    /**
     * 更新限流配置
     */
    @PutMapping("/config/{key}")
    public R<Void> updateConfig(@PathVariable String key, 
                               @RequestBody RateLimitConfig config) {
        configManager.updateConfig(key, config);
        return R.ok();
    }
    
    /**
     * 删除限流配置
     */
    @DeleteMapping("/config/{key}")
    public R<Void> deleteConfig(@PathVariable String key) {
        configManager.deleteConfig(key);
        return R.ok();
    }
    
    /**
     * 获取限流状态
     */
    @GetMapping("/status/{key}")
    public R<RateLimitStatus> getStatus(@PathVariable String key) {
        RateLimitStatus status = rateLimitService.getStatus(key);
        return R.ok(status);
    }
    
    /**
     * 重置限流计数
     */
    @PostMapping("/reset/{key}")
    public R<Void> resetLimit(@PathVariable String key) {
        rateLimitService.reset(key);
        return R.ok();
    }
    
    /**
     * 批量重置限流
     */
    @PostMapping("/reset/batch")
    public R<Void> batchReset(@RequestBody List<String> keys) {
        for (String key : keys) {
            rateLimitService.reset(key);
        }
        return R.ok();
    }
    
    /**
     * 获取限流统计
     */
    @GetMapping("/statistics")
    public R<RateLimitStatistics> getStatistics(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        RateLimitStatistics statistics = rateLimitStatisticsService
            .getStatistics(key, startTime, endTime);
        return R.ok(statistics);
    }
    
    /**
     * 导出限流日志
     */
    @GetMapping("/export")
    public void exportLogs(HttpServletResponse response,
                          @RequestParam(required = false) String key,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        List<RateLimitLog> logs = rateLimitLogService.getLogs(key, date);
        ExcelUtil<RateLimitLog> util = new ExcelUtil<>(RateLimitLog.class);
        util.exportExcel(response, logs, "限流日志");
    }
}
```

## 配置属性

```yaml
# Redis配置
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms

# RUI限流配置
rui:
  rate-limit:
    # 是否启用限流
    enabled: true
    
    # 默认配置
    default:
      # 默认限流次数
      count: 100
      # 默认时间窗口（秒）
      time: 60
      # 默认限流类型
      limit-type: IP
      # 默认算法
      algorithm: TOKEN_BUCKET
      # 默认错误消息
      message: "请求过于频繁，请稍后再试"
    
    # Redis配置
    redis:
      # Key前缀
      key-prefix: "rui:rate_limit:"
      # Key过期时间（秒）
      key-expire: 3600
      # 是否启用Key压缩
      key-compression: true
      # Lua脚本缓存
      script-cache: true
    
    # 算法配置
    algorithms:
      # 令牌桶算法
      token-bucket:
        # 默认桶容量
        default-capacity: 100
        # 默认补充速率（每秒）
        default-refill-rate: 10
        # 最大桶容量
        max-capacity: 10000
      
      # 滑动窗口算法
      sliding-window:
        # 默认窗口分片数
        default-window-size: 10
        # 最大窗口分片数
        max-window-size: 100
        # 窗口精度（毫秒）
        window-precision: 1000
      
      # 漏桶算法
      leaky-bucket:
        # 默认桶容量
        default-capacity: 100
        # 默认漏出速率（每秒）
        default-leak-rate: 10
        # 最大桶容量
        max-capacity: 10000
    
    # 监控配置
    monitor:
      # 是否启用监控
      enabled: true
      # 是否记录限流事件
      log-events: true
      # 是否启用指标收集
      metrics-enabled: true
      # 统计窗口大小（分钟）
      statistics-window: 60
      # 是否启用慢查询检测
      slow-query-detection: true
      # 慢查询阈值（毫秒）
      slow-query-threshold: 100
    
    # 告警配置
    alert:
      # 是否启用告警
      enabled: true
      # 告警阈值（限流触发次数）
      threshold: 100
      # 告警时间窗口（分钟）
      time-window: 5
      # 告警冷却时间（分钟）
      cooldown: 10
      # 告警接收者
      recipients:
        - admin@example.com
        - ops@example.com
    
    # 动态配置
    dynamic:
      # 是否启用动态配置
      enabled: true
      # 配置刷新间隔（秒）
      refresh-interval: 30
      # 配置来源
      config-source: NACOS
      # Nacos配置
      nacos:
        data-id: rate-limit-config
        group: DEFAULT_GROUP
    
    # 预热配置
    warmup:
      # 是否启用预热
      enabled: false
      # 默认预热时间（秒）
      default-period: 300
      # 预热期间限流比例
      warmup-ratio: 0.1
    
    # 降级配置
    fallback:
      # 是否启用降级
      enabled: true
      # 降级策略
      strategy: REJECT
      # 降级消息
      message: "系统繁忙，请稍后再试"
      # 降级后允许的请求比例
      allow-ratio: 0.1
    
    # 白名单配置
    whitelist:
      # 是否启用白名单
      enabled: true
      # IP白名单
      ips:
        - 127.0.0.1
        - 192.168.1.0/24
      # 用户白名单
      users:
        - admin
        - system
      # API白名单
      apis:
        - /health
        - /metrics
    
    # 黑名单配置
    blacklist:
      # 是否启用黑名单
      enabled: true
      # 自动加入黑名单的阈值
      auto-add-threshold: 1000
      # 黑名单过期时间（秒）
      expire-time: 3600
      # 黑名单检查间隔（秒）
      check-interval: 60
```

## 限流算法详解

### 1. 令牌桶算法 (Token Bucket)
- **原理**: 以固定速率向桶中添加令牌，请求消耗令牌
- **特点**: 允许突发流量，平滑限流
- **适用场景**: 需要处理突发流量的场景
- **参数**: 桶容量、补充速率、消耗令牌数

### 2. 滑动窗口算法 (Sliding Window)
- **原理**: 将时间窗口分成多个小窗口，统计请求数
- **特点**: 精确限流，防止突发流量
- **适用场景**: 需要精确控制流量的场景
- **参数**: 窗口大小、窗口分片数、允许请求数

### 3. 固定窗口算法 (Fixed Window)
- **原理**: 在固定时间窗口内统计请求数
- **特点**: 简单高效，但可能有边界突发问题
- **适用场景**: 一般的限流场景
- **参数**: 窗口大小、允许请求数

### 4. 漏桶算法 (Leaky Bucket)
- **原理**: 请求进入桶中，以固定速率流出
- **特点**: 恒定速率输出，适用于流量整形
- **适用场景**: 需要平滑输出的场景
- **参数**: 桶容量、漏出速率、请求大小

## 限流类型说明

### 1. IP限流 (IP)
- **Key格式**: `ip:{ip_address}`
- **适用场景**: 防止单个IP恶意请求
- **获取方式**: 从请求头或代理头获取真实IP

### 2. 用户限流 (USER)
- **Key格式**: `user:{user_id}`
- **适用场景**: 限制单个用户的请求频率
- **获取方式**: 从认证信息中获取用户ID

### 3. 全局限流 (GLOBAL)
- **Key格式**: `global:{api_path}`
- **适用场景**: 保护系统整体性能
- **获取方式**: 使用固定的全局Key

### 4. 自定义限流 (CUSTOM)
- **Key格式**: 自定义表达式
- **适用场景**: 复杂的限流需求
- **获取方式**: 通过SpEL表达式动态生成

### 5. API限流 (API)
- **Key格式**: `api:{method}:{path}`
- **适用场景**: 限制特定API的调用频率
- **获取方式**: 从请求方法和路径生成

### 6. 租户限流 (TENANT)
- **Key格式**: `tenant:{tenant_id}`
- **适用场景**: 多租户系统的资源隔离
- **获取方式**: 从租户上下文获取租户ID

## 高级功能

### 1. 分布式限流
```java
@RateLimit(
    key = "distributed:#{#clusterId}",
    limitType = LimitType.CUSTOM,
    count = 1000,
    time = 60,
    distributed = true,  // 启用分布式限流
    nodes = {"node1", "node2", "node3"}  // 参与节点
)
public void distributedOperation(String clusterId) {
    // 分布式限流操作
}
```

### 2. 限流熔断
```java
@RateLimit(
    key = "circuit:breaker",
    limitType = LimitType.GLOBAL,
    count = 100,
    time = 60,
    circuitBreaker = true,  // 启用熔断
    failureThreshold = 50,  // 失败阈值
    recoveryTime = 300      // 恢复时间
)
public void circuitBreakerOperation() {
    // 带熔断的限流操作
}
```

### 3. 限流降级
```java
@RateLimit(
    key = "degradable:operation",
    limitType = LimitType.GLOBAL,
    count = 100,
    time = 60,
    degradable = true,      // 启用降级
    degradeRatio = 0.5,     // 降级比例
    degradeMessage = "系统繁忙，已降级处理"
)
public void degradableOperation() {
    // 可降级的限流操作
}
```

### 4. 限流预测
```java
@Component
public class RateLimitPredictor {
    
    /**
     * 预测未来流量
     */
    public RateLimitPrediction predictTraffic(String key, int minutes) {
        // 基于历史数据预测未来流量
        List<RateLimitRecord> history = getHistoryData(key, minutes * 2);
        
        // 使用机器学习算法预测
        return mlService.predict(history, minutes);
    }
    
    /**
     * 动态调整限流参数
     */
    public void adjustRateLimit(String key, RateLimitPrediction prediction) {
        if (prediction.getExpectedQps() > getCurrentLimit(key) * 0.8) {
            // 预期流量接近限制，提前调整
            RateLimitConfig newConfig = RateLimitConfig.builder()
                .count((int) (prediction.getExpectedQps() * 1.2))
                .time(60)
                .build();
            
            configManager.updateConfig(key, newConfig);
        }
    }
}
```

## 性能优化

### 1. Lua脚本优化
- 使用原子性Lua脚本减少网络往返
- 脚本缓存避免重复编译
- 批量操作减少Redis调用次数

### 2. 内存优化
- 使用压缩算法减少Key大小
- 设置合理的过期时间
- 定期清理过期数据

### 3. 网络优化
- 使用连接池复用连接
- 启用Pipeline批量操作
- 选择合适的序列化方式

### 4. 算法优化
- 根据场景选择合适的算法
- 调整算法参数提高性能
- 使用近似算法降低复杂度

## 监控指标

### 1. 限流指标
- 限流触发次数
- 限流成功率
- 平均响应时间
- 峰值QPS

### 2. 系统指标
- Redis连接数
- 内存使用率
- CPU使用率
- 网络延迟

### 3. 业务指标
- 业务成功率
- 用户体验指标
- 系统可用性
- 错误率分布

## 最佳实践

### 1. 限流策略设计
- 根据业务特点选择合适的限流类型
- 设置合理的限流阈值
- 考虑突发流量的处理
- 实现多层限流保护

### 2. 性能考虑
- 选择高性能的限流算法
- 优化Redis配置和网络
- 使用本地缓存减少远程调用
- 监控限流性能指标

### 3. 可用性保障
- 实现限流降级机制
- 设置合理的超时时间
- 处理Redis故障场景
- 建立监控和告警

### 4. 运维管理
- 建立限流配置管理流程
- 定期分析限流数据
- 优化限流参数配置
- 制定应急处理预案

## 注意事项

1. **时钟同步**: 确保各节点时钟同步，避免时间窗口计算错误
2. **Redis可用性**: 考虑Redis故障对限流的影响，实现降级机制
3. **性能影响**: 限流会增加响应时间，需要权衡性能和保护效果
4. **配置管理**: 建立完善的配置管理和变更流程
5. **监控告警**: 建立完善的监控和告警机制
6. **测试验证**: 在生产环境前进行充分的压力测试
7. **容量规划**: 根据业务增长合理规划限流容量
8. **用户体验**: 提供友好的限流提示信息

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x
- Redis版本: 7.x+
- Lua脚本版本: 5.1+