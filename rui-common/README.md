# RUI Common Modules

## 模块简介

RUI框架的公共模块集合，提供企业级应用开发所需的基础功能和通用组件。采用模块化设计，支持按需引入，降低项目复杂度，提高开发效率。

## 架构设计

### 设计原则
- **模块化**: 功能独立，职责单一，支持按需引入
- **标准化**: 统一的编码规范和接口设计
- **可扩展**: 支持自定义扩展和配置
- **高性能**: 优化关键路径，提供高性能实现
- **易集成**: 提供自动配置，简化集成过程

### 技术栈
- **JDK**: 21+
- **Spring Boot**: 3.x
- **Spring Cloud**: 2023.x
- **MyBatis-Plus**: 3.5.x
- **Redis**: 7.x
- **MongoDB**: 6.x
- **Nacos**: 3.0

## 模块列表

### 核心模块

#### [rui-common-core](./rui-common-core/README.md)
**核心基础模块**
- 基础实体类和枚举
- 统一响应结果封装
- 分页工具和数据转换
- 常用工具类集合
- 异常定义和处理

#### [rui-common-config](./rui-common-config/README.md)
**配置管理模块**
- 统一配置管理
- 配置加密解密
- 动态配置刷新
- 配置验证和校验
- 多环境配置支持

#### [rui-common-web](./rui-common-web/README.md)
**Web基础模块**
- 基础控制器
- 全局异常处理
- 参数验证
- 跨域配置
- 文件上传下载

### 数据访问模块

#### [rui-common-mybatis](./rui-common-mybatis/README.md)
**MyBatis-Plus增强模块**
- MyBatis-Plus配置
- 基础实体和审计
- 自动填充功能
- 分页工具
- 多租户支持

#### [rui-common-redis](./rui-common-redis/README.md)
**Redis缓存模块**
- Redis配置和优化
- 缓存服务封装
- 分布式锁
- 限流器
- 性能监控

#### [rui-common-mongo](./rui-common-mongo/README.md)
**MongoDB数据库模块**
- MongoDB配置
- 基础Repository
- 查询构建器
- 索引管理
- 数据迁移

### 安全模块

#### [rui-common-security](./rui-common-security/README.md)
**安全认证模块**
- JWT令牌管理
- 用户认证授权
- 权限控制
- 安全过滤器
- 密码加密

#### [rui-common-idempotent](./rui-common-idempotent/README.md)
**幂等性控制模块**
- 接口幂等性保证
- 重复提交防护
- 分布式幂等
- 自定义幂等策略
- 幂等性监控

### 分布式模块

#### [rui-common-lock](./rui-common-lock/README.md)
**分布式锁模块**
- 基于Redisson的分布式锁
- 多种锁类型支持
- 注解式使用
- 锁续期和降级
- 锁监控告警

#### [rui-common-ratelimit](./rui-common-ratelimit/README.md)
**限流控制模块**
- 多种限流算法
- 分布式限流
- 注解式限流
- 动态配置
- 限流监控

### 服务治理模块

#### [rui-common-feign](./rui-common-feign/README.md)
**Feign客户端模块**
- Feign配置增强
- 负载均衡
- 熔断降级
- 请求重试
- 调用监控

#### [rui-common-mq](./rui-common-mq/README.md)
**消息队列模块**
- 多MQ支持
- 消息发送接收
- 事务消息
- 延时消息
- 死信处理

### 监控模块

#### [rui-common-log](./rui-common-log/README.md)
**日志管理模块**
- 统一日志配置
- 结构化日志
- 日志脱敏
- 操作日志记录
- 日志分析

#### [rui-common-monitoring](./rui-common-monitoring/README.md)
**监控指标模块**
- 应用性能监控
- 自定义指标
- 健康检查
- 告警通知
- 监控大盘

#### [rui-common-tracing](./rui-common-tracing/README.md)
**链路追踪模块**
- 分布式链路追踪
- 调用链分析
- 性能分析
- 异常追踪
- 链路可视化

## 快速开始

### 1. 添加父依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common</artifactId>
    <version>1.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

### 2. 引入所需模块

```xml
<!-- 核心模块（必需） -->
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-core</artifactId>
</dependency>

<!-- Web模块 -->
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-web</artifactId>
</dependency>

<!-- 数据库模块 -->
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-mybatis</artifactId>
</dependency>

<!-- 缓存模块 -->
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-redis</artifactId>
</dependency>

<!-- 安全模块 -->
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-security</artifactId>
</dependency>
```

### 3. 配置应用

```yaml
# application.yml
spring:
  application:
    name: rui-demo
  
  # 数据库配置
  datasource:
    url: jdbc:mysql://localhost:3306/rui_demo
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # Redis配置
  redis:
    host: localhost
    port: 6379
    database: 0

# RUI框架配置
rui:
  # 核心配置
  core:
    enabled: true
  
  # 安全配置
  security:
    enabled: true
    jwt:
      secret: your-secret-key
      expire-time: 7200
  
  # 限流配置
  rate-limit:
    enabled: true
    default:
      count: 100
      time: 60
```

### 4. 启用自动配置

```java
@SpringBootApplication
@EnableRuiCommon  // 启用RUI公共模块
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

### 5. 使用示例

```java
@RestController
@RequestMapping("/api")
public class DemoController extends BaseController {
    
    @Autowired
    private RedisService redisService;
    
    @RateLimit(count = 10, time = 60)
    @PostMapping("/demo")
    public R<String> demo(@RequestBody @Valid DemoRequest request) {
        // 使用Redis缓存
        String cacheKey = "demo:" + request.getId();
        String result = redisService.get(cacheKey);
        
        if (result == null) {
            // 业务逻辑处理
            result = processDemo(request);
            
            // 缓存结果
            redisService.set(cacheKey, result, 300);
        }
        
        return R.ok(result);
    }
    
    @DistributedLock(key = "demo:lock:#{#id}")
    @GetMapping("/lock/{id}")
    public R<String> lockDemo(@PathVariable String id) {
        // 分布式锁保护的业务逻辑
        return R.ok("success");
    }
    
    @Idempotent(key = "demo:idempotent:#{#request.orderId}")
    @PostMapping("/order")
    public R<Order> createOrder(@RequestBody OrderRequest request) {
        // 幂等性保护的订单创建
        Order order = orderService.create(request);
        return R.ok(order);
    }
}
```

## 模块依赖关系

```
rui-common-core (核心模块)
├── rui-common-config
├── rui-common-web
├── rui-common-mybatis
├── rui-common-redis
├── rui-common-mongo
├── rui-common-security
├── rui-common-idempotent
├── rui-common-lock
├── rui-common-ratelimit
├── rui-common-feign
├── rui-common-mq
├── rui-common-log
├── rui-common-monitoring
└── rui-common-tracing
```

## 最佳实践

### 1. 模块选择
- **最小化原则**: 只引入必需的模块，避免不必要的依赖
- **功能完整性**: 确保引入的模块能够满足业务需求
- **版本一致性**: 使用统一的版本管理，避免版本冲突

### 2. 配置管理
- **环境隔离**: 不同环境使用不同的配置文件
- **敏感信息**: 使用配置加密保护敏感信息
- **动态配置**: 利用配置中心实现动态配置更新

### 3. 性能优化
- **缓存策略**: 合理使用缓存提高性能
- **连接池**: 配置合适的连接池参数
- **监控告警**: 建立完善的监控和告警机制

### 4. 安全考虑
- **权限控制**: 实现细粒度的权限控制
- **数据加密**: 对敏感数据进行加密存储
- **安全审计**: 记录重要操作的审计日志

### 5. 开发规范
- **代码规范**: 遵循统一的代码规范和命名约定
- **异常处理**: 统一的异常处理和错误码定义
- **日志规范**: 规范的日志记录和格式

## 升级指南

### 版本兼容性
- **向后兼容**: 新版本保持向后兼容性
- **废弃通知**: 提前通知废弃的API和功能
- **迁移指南**: 提供详细的升级迁移指南

### 升级步骤
1. **备份数据**: 升级前备份重要数据
2. **测试验证**: 在测试环境验证新版本
3. **灰度发布**: 采用灰度发布降低风险
4. **监控观察**: 升级后密切监控系统状态

## 故障排查

### 常见问题
1. **依赖冲突**: 检查依赖版本是否兼容
2. **配置错误**: 验证配置文件的正确性
3. **网络问题**: 检查网络连接和防火墙设置
4. **资源不足**: 检查内存、CPU等资源使用情况

### 排查工具
- **日志分析**: 查看应用和框架日志
- **监控指标**: 分析性能监控数据
- **链路追踪**: 使用链路追踪定位问题
- **健康检查**: 检查各组件的健康状态

## 社区支持

### 文档资源
- **官方文档**: [https://docs.rui-framework.com](https://docs.rui-framework.com)
- **API文档**: [https://api.rui-framework.com](https://api.rui-framework.com)
- **示例项目**: [https://github.com/rui-framework/examples](https://github.com/rui-framework/examples)

### 技术支持
- **GitHub Issues**: [https://github.com/rui-framework/rui-common/issues](https://github.com/rui-framework/rui-common/issues)
- **技术论坛**: [https://forum.rui-framework.com](https://forum.rui-framework.com)
- **微信群**: 扫码加入技术交流群

### 贡献指南
- **代码贡献**: 欢迎提交Pull Request
- **问题反馈**: 及时反馈使用中遇到的问题
- **文档完善**: 帮助完善和翻译文档
- **测试用例**: 贡献测试用例和示例代码

## 版本信息

- **当前版本**: 1.0.0
- **发布日期**: 2024-01-01
- **JDK要求**: 21+
- **Spring Boot**: 3.2.x
- **Spring Cloud**: 2023.0.x

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 更新日志

### v1.0.0 (2024-01-01)
- 🎉 首次发布
- ✨ 完整的模块化架构
- 🚀 支持Spring Boot 3.x
- 📝 完善的文档和示例
- 🔧 自动配置和starter支持
- 🛡️ 企业级安全特性
- 📊 完整的监控和追踪
- 🔄 分布式组件支持