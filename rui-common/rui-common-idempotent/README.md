# RUI Common Idempotent

## 模块简介

RUI框架的幂等性处理模块，提供基于注解的幂等性控制，支持多种幂等性策略，防止重复提交和重复处理。

## 主要功能

### 1. 幂等性注解
- **@Idempotent**: 幂等性注解，支持多种幂等性类型

### 2. 幂等性切面
- **IdempotentAspect**: AOP切面，拦截带有@Idempotent注解的方法

### 3. 幂等性服务
- **IdempotentService**: 幂等性服务接口
- **RedisIdempotentServiceImpl**: 基于Redis的幂等性服务实现

### 4. 幂等性类型
- **IdempotentType**: 幂等性类型枚举

### 5. 工具类
- **IdempotentUtils**: 幂等性工具类

### 6. 自动配置
- **IdempotentAutoConfiguration**: 自动配置类
- **IdempotentProperties**: 配置属性类

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-idempotent</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Redis依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. 基本使用示例

```java
@RestController
public class OrderController {
    
    @PostMapping("/orders")
    @Idempotent(type = IdempotentType.PARAM, expireTime = 300)
    public R<Order> createOrder(@RequestBody CreateOrderRequest request) {
        // 创建订单逻辑
        Order order = orderService.createOrder(request);
        return R.ok(order);
    }
    
    @PostMapping("/payment")
    @Idempotent(type = IdempotentType.TOKEN, key = "#request.paymentToken")
    public R<Payment> processPayment(@RequestBody PaymentRequest request) {
        // 支付处理逻辑
        Payment payment = paymentService.processPayment(request);
        return R.ok(payment);
    }
}
```

### 3. 不同幂等性类型示例

#### 基于参数的幂等性
```java
@PostMapping("/users")
@Idempotent(type = IdempotentType.PARAM, key = "#user.email")
public R<User> createUser(@RequestBody User user) {
    // 基于用户邮箱进行幂等性控制
    return userService.createUser(user);
}
```

#### 基于Token的幂等性
```java
@PostMapping("/submit")
@Idempotent(type = IdempotentType.TOKEN, key = "#request.idempotentToken")
public R<Void> submitForm(@RequestBody FormRequest request) {
    // 基于幂等性Token进行控制
    return formService.submitForm(request);
}
```

#### 基于用户的幂等性
```java
@PostMapping("/vote")
@Idempotent(type = IdempotentType.USER, key = "#voteRequest.topicId")
public R<Void> vote(@RequestBody VoteRequest voteRequest) {
    // 基于当前用户进行幂等性控制（同一用户对同一主题只能投票一次）
    return voteService.vote(voteRequest);
}
```

#### 基于IP的幂等性
```java
@PostMapping("/feedback")
@Idempotent(type = IdempotentType.IP, expireTime = 3600)
public R<Void> submitFeedback(@RequestBody FeedbackRequest request) {
    // 基于IP地址进行幂等性控制（同一IP一小时内只能提交一次反馈）
    return feedbackService.submitFeedback(request);
}
```

### 4. 自定义幂等性Key

```java
@PostMapping("/transfer")
@Idempotent(
    type = IdempotentType.CUSTOM,
    key = "'transfer:' + #request.fromAccount + ':' + #request.toAccount + ':' + #request.amount",
    expireTime = 600
)
public R<Transfer> transfer(@RequestBody TransferRequest request) {
    // 自定义幂等性Key：转账操作基于源账户、目标账户和金额
    return transferService.transfer(request);
}
```

## 配置属性

```yaml
rui:
  idempotent:
    # 是否启用幂等性功能
    enabled: true
    # 默认过期时间（秒）
    default-expire-time: 300
    # Redis key前缀
    key-prefix: "idempotent:"
    # 是否记录幂等性日志
    log-enabled: true
    # 幂等性冲突时的处理策略
    conflict-strategy: EXCEPTION # EXCEPTION, RETURN_CACHED, IGNORE
    # 缓存配置
    cache:
      # 缓存类型：REDIS, MEMORY
      type: REDIS
      # 内存缓存最大条目数（仅当type=MEMORY时有效）
      max-entries: 10000
```

## 幂等性类型说明

### 1. PARAM（参数幂等）
- 基于方法参数生成幂等性Key
- 适用于基于业务参数的幂等性控制

### 2. TOKEN（令牌幂等）
- 基于客户端提供的幂等性Token
- 适用于客户端主动控制的幂等性场景

### 3. USER（用户幂等）
- 基于当前登录用户
- 适用于用户级别的幂等性控制

### 4. IP（IP幂等）
- 基于客户端IP地址
- 适用于防止恶意请求的场景

### 5. CUSTOM（自定义幂等）
- 基于自定义SpEL表达式
- 适用于复杂的幂等性规则

## 高级配置

### 1. 自定义幂等性服务

```java
@Component
public class CustomIdempotentService implements IdempotentService {
    
    @Override
    public boolean tryLock(String key, long expireTime) {
        // 自定义幂等性锁定逻辑
        return true;
    }
    
    @Override
    public void unlock(String key) {
        // 自定义幂等性解锁逻辑
    }
    
    @Override
    public boolean isLocked(String key) {
        // 自定义幂等性检查逻辑
        return false;
    }
}
```

### 2. 自定义Key生成器

```java
@Component
public class CustomKeyGenerator {
    
    public String generateKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        // 自定义Key生成逻辑
        return "custom:" + System.currentTimeMillis();
    }
}
```

### 3. 幂等性事件监听

```java
@Component
public class IdempotentEventListener {
    
    @EventListener
    public void handleIdempotentConflict(IdempotentConflictEvent event) {
        // 处理幂等性冲突事件
        log.warn("幂等性冲突: key={}, method={}", event.getKey(), event.getMethod());
    }
    
    @EventListener
    public void handleIdempotentSuccess(IdempotentSuccessEvent event) {
        // 处理幂等性成功事件
        log.info("幂等性检查通过: key={}", event.getKey());
    }
}
```

## 最佳实践

### 1. 合理设置过期时间
```java
// 短期操作（如表单提交）
@Idempotent(expireTime = 60)

// 中期操作（如订单创建）
@Idempotent(expireTime = 300)

// 长期操作（如文件上传）
@Idempotent(expireTime = 3600)
```

### 2. 选择合适的幂等性类型
```java
// 业务幂等：基于业务参数
@Idempotent(type = IdempotentType.PARAM, key = "#order.orderNo")

// 用户幂等：基于用户行为
@Idempotent(type = IdempotentType.USER, key = "#action")

// 防刷幂等：基于IP限制
@Idempotent(type = IdempotentType.IP)
```

### 3. 异常处理
```java
@PostMapping("/submit")
@Idempotent
public R<Void> submit(@RequestBody Request request) {
    try {
        // 业务逻辑
        return R.ok();
    } catch (IdempotentException e) {
        return R.fail("请勿重复提交");
    }
}
```

## 注意事项

1. **性能考虑**: 幂等性检查会增加Redis访问，注意性能影响
2. **过期时间**: 合理设置过期时间，避免内存泄漏
3. **Key设计**: 幂等性Key应具有唯一性和业务意义
4. **异常处理**: 妥善处理幂等性冲突异常
5. **监控告警**: 监控幂等性冲突频率，及时发现异常

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x
- Redis版本: 7.x