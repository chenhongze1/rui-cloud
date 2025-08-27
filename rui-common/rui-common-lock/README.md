# RUI Common Lock

## 模块简介

RUI框架的分布式锁模块，基于Redisson实现，提供简单易用的分布式锁功能，支持注解式和编程式两种使用方式，确保在分布式环境下的数据一致性和并发安全。

## 主要功能

### 1. 分布式锁注解
- **@DistributedLock**: 分布式锁注解，支持方法级别的锁控制

### 2. 锁类型支持
- **REENTRANT**: 可重入锁（默认）
- **FAIR**: 公平锁
- **READ**: 读锁
- **WRITE**: 写锁
- **SEMAPHORE**: 信号量
- **COUNT_DOWN_LATCH**: 倒计时锁

### 3. 核心组件
- **DistributedLockAspect**: 分布式锁切面处理
- **DistributedLockService**: 分布式锁服务接口
- **RedissonDistributedLockServiceImpl**: 基于Redisson的实现
- **DistributedLockAutoConfiguration**: 自动配置类

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-lock</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Redisson依赖 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.24.3</version>
</dependency>

<!-- Spring Boot AOP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 2. 注解式使用

```java
@Service
public class OrderService {
    
    /**
     * 基本分布式锁使用
     */
    @DistributedLock(key = "order:create:#{#userId}")
    public void createOrder(Long userId, OrderRequest request) {
        // 业务逻辑，同一用户同时只能创建一个订单
        Order order = new Order();
        order.setUserId(userId);
        order.setAmount(request.getAmount());
        orderMapper.insert(order);
    }
    
    /**
     * 自定义锁等待时间和持有时间
     */
    @DistributedLock(
        key = "inventory:deduct:#{#productId}",
        waitTime = 5000,  // 等待锁5秒
        leaseTime = 10000 // 持有锁10秒
    )
    public boolean deductInventory(Long productId, Integer quantity) {
        // 扣减库存逻辑
        Inventory inventory = inventoryMapper.selectById(productId);
        if (inventory.getStock() >= quantity) {
            inventory.setStock(inventory.getStock() - quantity);
            inventoryMapper.updateById(inventory);
            return true;
        }
        return false;
    }
    
    /**
     * 公平锁使用
     */
    @DistributedLock(
        key = "payment:process:#{#orderId}",
        lockType = LockType.FAIR,
        waitTime = 3000
    )
    public void processPayment(Long orderId, PaymentRequest request) {
        // 支付处理逻辑，使用公平锁确保先到先得
        Order order = orderMapper.selectById(orderId);
        if (order.getStatus() == OrderStatus.PENDING) {
            // 处理支付
            paymentService.process(order, request);
            order.setStatus(OrderStatus.PAID);
            orderMapper.updateById(order);
        }
    }
    
    /**
     * 读写锁 - 读锁
     */
    @DistributedLock(
        key = "product:info:#{#productId}",
        lockType = LockType.READ,
        waitTime = 1000
    )
    public Product getProduct(Long productId) {
        // 读取商品信息，多个线程可以同时读取
        return productMapper.selectById(productId);
    }
    
    /**
     * 读写锁 - 写锁
     */
    @DistributedLock(
        key = "product:info:#{#product.id}",
        lockType = LockType.WRITE,
        waitTime = 2000,
        leaseTime = 5000
    )
    public void updateProduct(Product product) {
        // 更新商品信息，写操作需要独占锁
        productMapper.updateById(product);
        
        // 清除缓存
        redisTemplate.delete("product:cache:" + product.getId());
    }
    
    /**
     * 信号量使用
     */
    @DistributedLock(
        key = "concurrent:limit",
        lockType = LockType.SEMAPHORE,
        permits = 10,  // 最多允许10个并发
        waitTime = 5000
    )
    public void limitedConcurrentOperation() {
        // 限制并发数量的操作
        try {
            Thread.sleep(2000); // 模拟耗时操作
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 自定义锁Key生成
     */
    @DistributedLock(
        key = "complex:#{#request.userId}:#{#request.type}:#{T(java.time.LocalDate).now()}",
        waitTime = 3000
    )
    public void complexKeyExample(ComplexRequest request) {
        // 复杂的锁Key生成，包含用户ID、类型和当前日期
        processComplexRequest(request);
    }
    
    /**
     * 条件锁 - 只有满足条件才加锁
     */
    @DistributedLock(
        key = "conditional:#{#orderId}",
        condition = "#{#order.amount > 1000}", // 只有订单金额大于1000才加锁
        waitTime = 2000
    )
    public void conditionalLock(Long orderId, Order order) {
        // 只有高价值订单才需要加锁处理
        processHighValueOrder(order);
    }
}
```

### 3. 编程式使用

```java
@Service
public class InventoryService {
    
    @Autowired
    private DistributedLockService lockService;
    
    /**
     * 编程式分布式锁使用
     */
    public boolean deductInventory(Long productId, Integer quantity) {
        String lockKey = "inventory:" + productId;
        
        return lockService.executeWithLock(lockKey, 5000, 10000, () -> {
            // 在锁保护下执行业务逻辑
            Inventory inventory = inventoryMapper.selectById(productId);
            if (inventory.getStock() >= quantity) {
                inventory.setStock(inventory.getStock() - quantity);
                inventoryMapper.updateById(inventory);
                return true;
            }
            return false;
        });
    }
    
    /**
     * 手动获取和释放锁
     */
    public void manualLockExample(Long productId) {
        String lockKey = "manual:lock:" + productId;
        RLock lock = null;
        
        try {
            // 获取锁
            lock = lockService.getLock(lockKey);
            
            // 尝试获取锁，等待5秒，持有10秒
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                // 执行业务逻辑
                doBusinessLogic(productId);
            } else {
                throw new ServiceException("获取锁失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("获取锁被中断");
        } finally {
            // 释放锁
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * 公平锁使用
     */
    public void fairLockExample(String resource) {
        String lockKey = "fair:lock:" + resource;
        
        lockService.executeWithFairLock(lockKey, 3000, 5000, () -> {
            // 公平锁保证先到先得
            processFairResource(resource);
            return null;
        });
    }
    
    /**
     * 读写锁使用
     */
    public String readData(String dataKey) {
        String lockKey = "rw:lock:" + dataKey;
        
        return lockService.executeWithReadLock(lockKey, 2000, () -> {
            // 读操作，多个线程可以同时执行
            return dataService.getData(dataKey);
        });
    }
    
    public void writeData(String dataKey, String data) {
        String lockKey = "rw:lock:" + dataKey;
        
        lockService.executeWithWriteLock(lockKey, 3000, 5000, () -> {
            // 写操作，需要独占锁
            dataService.saveData(dataKey, data);
            return null;
        });
    }
    
    /**
     * 信号量使用
     */
    public void semaphoreExample() {
        String semaphoreKey = "semaphore:limit";
        int permits = 5; // 最多5个并发
        
        lockService.executeWithSemaphore(semaphoreKey, permits, 3000, () -> {
            // 限制并发数量的操作
            performLimitedOperation();
            return null;
        });
    }
    
    /**
     * 倒计时锁使用
     */
    public void countDownLatchExample() {
        String latchKey = "countdown:task";
        int count = 3; // 等待3个任务完成
        
        // 等待任务完成
        lockService.executeWithCountDownLatch(latchKey, count, 10000, () -> {
            // 等待所有任务完成后执行
            processAfterAllTasksComplete();
            return null;
        });
    }
    
    /**
     * 批量锁操作
     */
    public void batchLockExample(List<Long> productIds) {
        List<String> lockKeys = productIds.stream()
            .map(id -> "batch:lock:" + id)
            .collect(Collectors.toList());
        
        lockService.executeWithMultiLock(lockKeys, 5000, 10000, () -> {
            // 同时锁定多个资源
            for (Long productId : productIds) {
                updateProduct(productId);
            }
            return null;
        });
    }
}
```

### 4. 自定义锁Key生成器

```java
@Component
public class CustomLockKeyGenerator implements LockKeyGenerator {
    
    @Override
    public String generateKey(String keyExpression, Object[] args, Method method) {
        // 自定义锁Key生成逻辑
        if (keyExpression.startsWith("user:")) {
            // 用户相关的锁Key
            return "lock:user:" + extractUserId(args) + ":" + method.getName();
        }
        
        // 默认处理
        return "lock:" + keyExpression;
    }
    
    private String extractUserId(Object[] args) {
        // 从参数中提取用户ID
        for (Object arg : args) {
            if (arg instanceof Long) {
                return arg.toString();
            }
            if (arg instanceof UserRequest) {
                return ((UserRequest) arg).getUserId().toString();
            }
        }
        return "unknown";
    }
}
```

### 5. 锁事件监听

```java
@Component
@EventListener
public class LockEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(LockEventListener.class);
    
    /**
     * 锁获取成功事件
     */
    @EventListener
    public void onLockAcquired(LockAcquiredEvent event) {
        log.info("锁获取成功: key={}, thread={}, waitTime={}ms", 
            event.getLockKey(), 
            event.getThreadName(), 
            event.getWaitTime());
    }
    
    /**
     * 锁释放事件
     */
    @EventListener
    public void onLockReleased(LockReleasedEvent event) {
        log.info("锁释放: key={}, thread={}, holdTime={}ms", 
            event.getLockKey(), 
            event.getThreadName(), 
            event.getHoldTime());
    }
    
    /**
     * 锁获取失败事件
     */
    @EventListener
    public void onLockFailed(LockFailedEvent event) {
        log.warn("锁获取失败: key={}, thread={}, reason={}", 
            event.getLockKey(), 
            event.getThreadName(), 
            event.getReason());
    }
    
    /**
     * 锁超时事件
     */
    @EventListener
    public void onLockTimeout(LockTimeoutEvent event) {
        log.error("锁超时: key={}, thread={}, timeout={}ms", 
            event.getLockKey(), 
            event.getThreadName(), 
            event.getTimeout());
    }
}
```

## 配置属性

```yaml
# Redisson配置
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

# RUI分布式锁配置
rui:
  lock:
    # 是否启用分布式锁
    enabled: true
    
    # 默认配置
    default:
      # 默认等待时间（毫秒）
      wait-time: 3000
      # 默认持有时间（毫秒）
      lease-time: 10000
      # 默认锁类型
      lock-type: REENTRANT
      # 默认信号量许可数
      permits: 1
    
    # 锁Key配置
    key:
      # 锁Key前缀
      prefix: "rui:lock:"
      # 是否启用Key过期
      enable-expire: true
      # Key过期时间（秒）
      expire-time: 300
    
    # 监控配置
    monitor:
      # 是否启用监控
      enabled: true
      # 是否记录锁事件
      log-events: true
      # 是否启用指标收集
      metrics-enabled: true
      # 慢锁阈值（毫秒）
      slow-lock-threshold: 5000
    
    # 重试配置
    retry:
      # 是否启用重试
      enabled: true
      # 最大重试次数
      max-attempts: 3
      # 重试间隔（毫秒）
      retry-interval: 100
      # 重试退避策略
      backoff-strategy: EXPONENTIAL
    
    # 异常处理配置
    exception:
      # 锁获取失败时是否抛出异常
      throw-on-failure: true
      # 自定义异常类
      custom-exception-class: com.rui.common.exception.LockException
    
    # 性能优化配置
    performance:
      # 是否启用锁池化
      enable-pooling: true
      # 锁池大小
      pool-size: 100
      # 是否启用锁预热
      enable-warmup: false
      # 预热锁数量
      warmup-count: 10
```

## 锁类型说明

### 1. 可重入锁 (REENTRANT)
- **特点**: 同一线程可以多次获取同一把锁
- **适用场景**: 一般的互斥操作
- **性能**: 高

### 2. 公平锁 (FAIR)
- **特点**: 按照请求锁的顺序获取锁
- **适用场景**: 需要保证公平性的场景
- **性能**: 相对较低

### 3. 读锁 (READ)
- **特点**: 多个线程可以同时获取读锁
- **适用场景**: 读多写少的场景
- **性能**: 读操作性能高

### 4. 写锁 (WRITE)
- **特点**: 独占锁，与读锁和写锁互斥
- **适用场景**: 写操作保护
- **性能**: 写操作安全性高

### 5. 信号量 (SEMAPHORE)
- **特点**: 控制同时访问资源的线程数量
- **适用场景**: 限流、资源池管理
- **性能**: 适中

### 6. 倒计时锁 (COUNT_DOWN_LATCH)
- **特点**: 等待多个任务完成
- **适用场景**: 任务协调、批处理
- **性能**: 适中

## 高级功能

### 1. 锁续期
```java
@DistributedLock(
    key = "long:task:#{#taskId}",
    leaseTime = 30000,  // 30秒
    autoRenew = true,   // 自动续期
    renewInterval = 10000 // 每10秒续期一次
)
public void longRunningTask(String taskId) {
    // 长时间运行的任务，锁会自动续期
    performLongTask(taskId);
}
```

### 2. 锁降级
```java
@DistributedLock(
    key = "degradable:#{#resourceId}",
    degradeOnFailure = true, // 获取锁失败时降级
    degradeStrategy = DegradeStrategy.SKIP // 跳过执行
)
public void degradableOperation(String resourceId) {
    // 可降级的操作
    performOperation(resourceId);
}
```

### 3. 分布式锁集群
```java
@DistributedLock(
    key = "cluster:#{#shardKey}",
    cluster = true, // 启用集群模式
    shardCount = 16 // 分片数量
)
public void clusterOperation(String shardKey) {
    // 集群环境下的分布式锁
    performClusterOperation(shardKey);
}
```

### 4. 锁监控和告警
```java
@Component
public class LockMonitor {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void onLockEvent(LockEvent event) {
        // 记录锁指标
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("distributed.lock.duration")
            .tag("key", event.getLockKey())
            .tag("type", event.getLockType().name())
            .register(meterRegistry));
        
        // 慢锁告警
        if (event.getDuration() > 5000) {
            alertService.sendSlowLockAlert(event);
        }
    }
}
```

## 性能优化

### 1. 锁粒度优化
- 使用细粒度锁减少竞争
- 避免大范围锁定
- 合理设计锁Key

### 2. 锁时间优化
- 合理设置等待时间和持有时间
- 避免长时间持有锁
- 使用锁续期机制

### 3. 锁类型选择
- 根据场景选择合适的锁类型
- 读多写少场景使用读写锁
- 限流场景使用信号量

### 4. 连接池优化
- 合理配置Redis连接池
- 使用连接复用
- 监控连接使用情况

## 监控指标

### 1. 锁性能指标
- 锁获取成功率
- 锁等待时间分布
- 锁持有时间分布
- 锁竞争次数

### 2. 系统指标
- Redis连接数
- 内存使用情况
- 网络延迟
- 错误率

### 3. 业务指标
- 业务操作成功率
- 业务操作耗时
- 并发处理能力
- 系统吞吐量

## 最佳实践

### 1. 锁设计原则
- 锁粒度要细，避免大锁
- 锁时间要短，避免长时间持有
- 锁Key要唯一，避免冲突
- 异常处理要完善，确保锁释放

### 2. 性能考虑
- 合理设置超时时间
- 避免锁嵌套和死锁
- 使用异步处理减少锁等待
- 监控锁性能指标

### 3. 安全考虑
- 确保锁的原子性
- 防止锁被误释放
- 处理网络分区问题
- 实现锁的容错机制

### 4. 运维考虑
- 监控锁使用情况
- 设置告警阈值
- 定期清理过期锁
- 备份锁配置

## 注意事项

1. **死锁预防**: 避免循环等待，统一锁获取顺序
2. **异常处理**: 确保在异常情况下锁能正确释放
3. **网络分区**: 考虑Redis网络分区对锁的影响
4. **时钟同步**: 确保各节点时钟同步，避免锁时间计算错误
5. **资源清理**: 定期清理过期的锁资源
6. **版本兼容**: 注意Redisson版本与Redis版本的兼容性
7. **性能测试**: 在生产环境前进行充分的性能测试
8. **监控告警**: 建立完善的监控和告警机制

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x
- Redisson版本: 3.24.3+
- Redis版本: 7.x+