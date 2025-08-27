# RUI Common Feign

## 模块简介

RUI框架的Feign客户端增强模块，提供Feign客户端的统一配置、错误处理、请求拦截、重试机制等功能，简化微服务间的HTTP调用。

## 主要功能

### 1. 自动配置
- **FeignAutoConfiguration**: Feign自动配置类
- **FeignLoggerConfig**: Feign日志配置
- **FeignProperties**: Feign配置属性

### 2. 错误处理
- **FeignErrorDecoder**: 自定义错误解码器，统一处理HTTP错误响应

### 3. 请求拦截
- **FeignRequestInterceptor**: 请求拦截器，添加通用请求头、认证信息等

### 4. 重试机制
- **FeignRetryer**: 自定义重试器，提供智能重试策略

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-feign</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Spring Cloud OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### 2. 启用Feign客户端

```java
@SpringBootApplication
@EnableFeignClients
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 定义Feign客户端

```java
@FeignClient(name = "user-service", path = "/api/users")
public interface UserFeignClient {
    
    @GetMapping("/{id}")
    R<User> getUser(@PathVariable("id") Long id);
    
    @PostMapping
    R<Void> createUser(@RequestBody User user);
    
    @PutMapping("/{id}")
    R<Void> updateUser(@PathVariable("id") Long id, @RequestBody User user);
    
    @DeleteMapping("/{id}")
    R<Void> deleteUser(@PathVariable("id") Long id);
}
```

### 4. 使用Feign客户端

```java
@Service
public class UserService {
    
    @Autowired
    private UserFeignClient userFeignClient;
    
    public User getUserById(Long id) {
        R<User> result = userFeignClient.getUser(id);
        if (result.isSuccess()) {
            return result.getData();
        }
        throw new ServiceException("获取用户信息失败: " + result.getMsg());
    }
    
    public void createUser(User user) {
        R<Void> result = userFeignClient.createUser(user);
        if (!result.isSuccess()) {
            throw new ServiceException("创建用户失败: " + result.getMsg());
        }
    }
}
```

## 配置属性

```yaml
rui:
  feign:
    # 是否启用请求拦截器
    request-interceptor-enabled: true
    # 是否启用错误解码器
    error-decoder-enabled: true
    # 是否启用自定义重试器
    retryer-enabled: true
    # 重试配置
    retry:
      # 重试间隔（毫秒）
      period: 1000
      # 最大重试间隔（毫秒）
      max-period: 5000
      # 最大重试次数
      max-attempts: 3
    # 日志配置
    logging:
      # 日志级别: NONE, BASIC, HEADERS, FULL
      level: BASIC
      # 是否记录请求体
      log-request-body: false
      # 是否记录响应体
      log-response-body: false

# Feign客户端配置
feign:
  client:
    config:
      default:
        # 连接超时时间（毫秒）
        connect-timeout: 5000
        # 读取超时时间（毫秒）
        read-timeout: 10000
        # 日志级别
        logger-level: basic
      # 特定服务配置
      user-service:
        connect-timeout: 3000
        read-timeout: 8000
        logger-level: full
```

## 主要特性

### 1. 统一错误处理

```java
// 自动处理HTTP错误状态码
// 4xx错误抛出客户端异常
// 5xx错误抛出服务端异常
// 支持自定义错误码映射
```

### 2. 自动请求头添加

```java
// 自动添加以下请求头：
// - X-Request-Id: 请求追踪ID
// - X-User-Id: 当前用户ID
// - Authorization: 认证令牌
// - X-Tenant-Id: 租户ID
```

### 3. 智能重试机制

```java
// 支持以下场景的重试：
// - 网络连接异常
// - 读取超时
// - 5xx服务器错误
// - 自定义重试条件
```

### 4. 详细日志记录

```java
// 记录以下信息：
// - 请求URL和方法
// - 请求头和请求体
// - 响应状态码和响应体
// - 请求耗时
```

## 高级配置

### 1. 自定义错误解码器

```java
@Component
public class CustomFeignErrorDecoder implements ErrorDecoder {
    
    @Override
    public Exception decode(String methodKey, Response response) {
        // 自定义错误处理逻辑
        return new ServiceException("服务调用失败");
    }
}
```

### 2. 自定义请求拦截器

```java
@Component
public class CustomFeignRequestInterceptor implements RequestInterceptor {
    
    @Override
    public void apply(RequestTemplate template) {
        // 添加自定义请求头
        template.header("X-Custom-Header", "custom-value");
    }
}
```

### 3. 熔断器集成

```java
@FeignClient(name = "user-service", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {
    // Feign接口定义
}

@Component
public class UserFeignClientFallback implements UserFeignClient {
    
    @Override
    public R<User> getUser(Long id) {
        return R.fail("用户服务暂时不可用");
    }
}
```

## 监控和指标

### 1. 请求指标
- 请求总数
- 请求成功率
- 请求平均耗时
- 请求错误分布

### 2. 服务健康检查
- 服务可用性监控
- 响应时间监控
- 错误率告警

## 注意事项

1. **超时配置**: 合理设置连接超时和读取超时时间
2. **重试策略**: 避免对非幂等操作进行重试
3. **熔断保护**: 配置熔断器防止雪崩效应
4. **日志级别**: 生产环境建议使用BASIC级别日志
5. **安全考虑**: 敏感信息不要记录在日志中

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x
- Spring Cloud版本: 2023.x
- OpenFeign版本: 4.x