# RUI Common MQ

## 模块简介

RUI框架的消息队列模块，提供统一的消息发送和接收功能，支持多种消息队列中间件，简化消息队列的使用和管理。

## 主要功能

### 1. 消息生产者
- **MessageProducerService**: 消息生产者服务接口
- **MessageProducerServiceImpl**: 消息生产者服务实现

### 2. 消息消费者
- **BaseMessageConsumer**: 基础消息消费者抽象类

### 3. 工具类
- **MessageUtils**: 消息工具类

### 4. 自动配置
- **MqAutoConfiguration**: 消息队列自动配置
- **MqProperties**: 消息队列配置属性

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-mq</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- RabbitMQ依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- 或者RocketMQ依赖 -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
```

### 2. 消息发送示例

```java
@Service
public class OrderService {
    
    @Autowired
    private MessageProducerService messageProducerService;
    
    public void createOrder(Order order) {
        // 保存订单
        orderMapper.insert(order);
        
        // 发送订单创建消息
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setAmount(order.getAmount());
        
        messageProducerService.sendMessage("order.created", event);
    }
    
    public void cancelOrder(Long orderId) {
        // 取消订单
        orderMapper.updateStatus(orderId, OrderStatus.CANCELLED);
        
        // 发送延迟消息（30分钟后检查订单状态）
        OrderCheckEvent checkEvent = new OrderCheckEvent();
        checkEvent.setOrderId(orderId);
        
        messageProducerService.sendDelayMessage(
            "order.check", 
            checkEvent, 
            Duration.ofMinutes(30)
        );
    }
}
```

### 3. 消息消费示例

```java
@Component
public class OrderMessageConsumer extends BaseMessageConsumer<OrderCreatedEvent> {
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    protected String getTopic() {
        return "order.created";
    }
    
    @Override
    protected void handleMessage(OrderCreatedEvent event) {
        try {
            // 扣减库存
            inventoryService.decreaseStock(event.getOrderId());
            
            // 发送通知
            notificationService.sendOrderNotification(event.getUserId(), event.getOrderId());
            
            log.info("订单创建事件处理成功: orderId={}", event.getOrderId());
            
        } catch (Exception e) {
            log.error("订单创建事件处理失败: orderId={}", event.getOrderId(), e);
            throw e; // 重新抛出异常，触发重试机制
        }
    }
    
    @Override
    protected int getMaxRetryTimes() {
        return 3; // 最大重试3次
    }
    
    @Override
    protected long getRetryInterval() {
        return 5000; // 重试间隔5秒
    }
}
```

### 4. 批量消息处理

```java
@Component
public class BatchOrderConsumer extends BaseMessageConsumer<List<OrderCreatedEvent>> {
    
    @Override
    protected String getTopic() {
        return "order.batch";
    }
    
    @Override
    protected void handleMessage(List<OrderCreatedEvent> events) {
        // 批量处理订单事件
        List<Long> orderIds = events.stream()
            .map(OrderCreatedEvent::getOrderId)
            .collect(Collectors.toList());
            
        inventoryService.batchDecreaseStock(orderIds);
        notificationService.batchSendNotification(events);
    }
    
    @Override
    protected boolean isBatchConsumer() {
        return true;
    }
    
    @Override
    protected int getBatchSize() {
        return 100; // 批量大小
    }
}
```

### 5. 事务消息

```java
@Service
public class PaymentService {
    
    @Autowired
    private MessageProducerService messageProducerService;
    
    @Transactional
    public void processPayment(Payment payment) {
        // 处理支付
        paymentMapper.insert(payment);
        
        // 发送事务消息
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setPaymentId(payment.getId());
        event.setOrderId(payment.getOrderId());
        event.setAmount(payment.getAmount());
        
        messageProducerService.sendTransactionalMessage(
            "payment.completed", 
            event,
            () -> {
                // 本地事务执行成功后的回调
                log.info("支付事务消息发送成功: paymentId={}", payment.getId());
            }
        );
    }
}
```

## 配置属性

```yaml
rui:
  mq:
    # 消息队列类型：RABBITMQ, ROCKETMQ, KAFKA
    type: RABBITMQ
    # 是否启用消息队列
    enabled: true
    # 默认序列化方式：JSON, PROTOBUF, AVRO
    serialization: JSON
    # 生产者配置
    producer:
      # 是否启用事务消息
      transaction-enabled: true
      # 发送超时时间（毫秒）
      send-timeout: 3000
      # 重试次数
      retry-times: 3
      # 是否启用异步发送
      async-enabled: true
    # 消费者配置
    consumer:
      # 并发消费线程数
      concurrency: 4
      # 最大并发消费线程数
      max-concurrency: 8
      # 消息预取数量
      prefetch-count: 10
      # 是否启用死信队列
      dead-letter-enabled: true
      # 最大重试次数
      max-retry-times: 3
      # 重试间隔（毫秒）
      retry-interval: 5000
    # 监控配置
    monitoring:
      # 是否启用监控
      enabled: true
      # 监控指标收集间隔（秒）
      collect-interval: 30
      # 是否启用慢消息监控
      slow-message-enabled: true
      # 慢消息阈值（毫秒）
      slow-threshold: 5000

# RabbitMQ配置
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    connection-timeout: 15000
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: manual
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000

# RocketMQ配置
rocketmq:
  name-server: localhost:9876
  producer:
    group: rui-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 3
  consumer:
    group: rui-consumer-group
    consume-thread-min: 4
    consume-thread-max: 8
```

## 消息类型

### 1. 普通消息
- 最基本的消息类型
- 支持同步和异步发送
- 适用于一般的业务通知

### 2. 延迟消息
- 指定延迟时间后才被消费
- 适用于定时任务、超时处理等场景

### 3. 事务消息
- 与本地事务绑定
- 保证消息发送与本地事务的一致性

### 4. 顺序消息
- 保证消息的顺序消费
- 适用于有序性要求的业务场景

### 5. 批量消息
- 批量发送和消费消息
- 提高处理效率

## 高级功能

### 1. 消息过滤

```java
@Component
public class OrderMessageConsumer extends BaseMessageConsumer<OrderEvent> {
    
    @Override
    protected boolean shouldConsume(OrderEvent event) {
        // 只处理金额大于100的订单
        return event.getAmount().compareTo(BigDecimal.valueOf(100)) > 0;
    }
    
    @Override
    protected void handleMessage(OrderEvent event) {
        // 处理高金额订单
    }
}
```

### 2. 消息转换

```java
@Component
public class MessageConverter {
    
    public <T> T convert(Object message, Class<T> targetType) {
        // 自定义消息转换逻辑
        return objectMapper.convertValue(message, targetType);
    }
}
```

### 3. 消息监控

```java
@Component
public class MessageMonitor {
    
    @EventListener
    public void handleMessageSent(MessageSentEvent event) {
        // 记录消息发送指标
        meterRegistry.counter("message.sent", "topic", event.getTopic()).increment();
    }
    
    @EventListener
    public void handleMessageConsumed(MessageConsumedEvent event) {
        // 记录消息消费指标
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("message.consume.duration")
            .tag("topic", event.getTopic())
            .register(meterRegistry));
    }
}
```

### 4. 死信队列处理

```java
@Component
public class DeadLetterConsumer extends BaseMessageConsumer<Object> {
    
    @Override
    protected String getTopic() {
        return "dead.letter.queue";
    }
    
    @Override
    protected void handleMessage(Object message) {
        // 处理死信消息
        log.error("收到死信消息: {}", message);
        
        // 可以选择重新投递、记录日志、发送告警等
        alertService.sendDeadLetterAlert(message);
    }
}
```

## 最佳实践

### 1. 消息设计
- 消息应包含足够的上下文信息
- 避免消息过大，考虑分拆或引用
- 设计向后兼容的消息格式

### 2. 幂等性
- 消费者应设计为幂等的
- 使用唯一标识符避免重复处理

### 3. 错误处理
- 合理设置重试次数和间隔
- 对于不可恢复的错误，及时进入死信队列

### 4. 性能优化
- 合理设置并发消费线程数
- 使用批量处理提高效率
- 监控消息积压情况

## 监控指标

### 1. 生产者指标
- 消息发送速率
- 发送成功率
- 发送延迟

### 2. 消费者指标
- 消息消费速率
- 消费成功率
- 消费延迟
- 消息积压数量

### 3. 系统指标
- 连接数
- 内存使用
- 网络IO

## 注意事项

1. **消息丢失**: 确保消息的可靠性投递
2. **重复消费**: 设计幂等的消费逻辑
3. **消息积压**: 监控队列长度，及时处理积压
4. **资源管理**: 合理配置连接池和线程池
5. **安全考虑**: 配置适当的认证和授权机制

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x
- RabbitMQ版本: 3.12.x
- RocketMQ版本: 5.x