# RUI Framework - SpringCloud微服务架构基础框架

基于Spring Cloud的企业级微服务框架，提供完整的公共模块和基础设施支持，助力快速构建高质量的微服务应用。

## 🚀 技术栈

- **Java**: 21+
- **Spring Boot**: 3.2+
- **Spring Cloud**: 2023.0+
- **Spring Security**: 6.x
- **MyBatis Plus**: 3.5+
- **Redis**: 7.x
- **MongoDB**: 4.4+
- **RocketMQ**: 5.x
- **Nacos**: 3.0+
- **OpenTelemetry**: 1.32+

## 📦 模块架构

### 🏗️ 核心基础模块

#### rui-common-core
**核心工具模块** - 提供框架的基础工具类和通用功能
- 🔧 **通用工具类**: 字符串、日期、集合、反射等工具
- 📋 **常量定义**: 系统常量、错误码、状态码等
- 🎯 **异常处理**: 统一异常定义和处理机制
- 🔄 **类型转换**: 通用的数据类型转换器
- ✅ **验证工具**: 参数校验和数据验证工具

#### rui-common-web
**Web层公共模块** - 统一Web层配置和处理逻辑
- 🌐 **全局异常处理**: 统一的异常拦截和响应格式
- 📤 **响应封装**: 标准化的API响应格式
- 🔍 **参数校验**: 请求参数的自动校验
- 📄 **分页支持**: 统一的分页查询封装
- 🎨 **跨域配置**: CORS跨域请求处理
- 📝 **接口文档**: Swagger/OpenAPI集成

#### rui-common-security
**安全认证模块** - 提供完整的安全认证和授权功能
- 🔐 **JWT认证**: 基于JWT的无状态认证
- 👤 **用户认证**: 多种认证方式支持
- 🛡️ **权限控制**: 基于角色和权限的访问控制
- 🔒 **密码加密**: 安全的密码加密和验证
- 🚫 **安全防护**: XSS、CSRF等安全防护
- 📊 **审计日志**: 用户操作审计和日志记录

### 💾 数据访问模块

#### rui-common-mybatis
**MyBatis集成模块** - 关系型数据库访问层统一配置
- 🗄️ **数据源配置**: 多数据源支持和动态切换
- 📝 **SQL审计**: SQL执行监控和性能分析
- 🔄 **事务管理**: 分布式事务支持
- 📄 **分页插件**: 高性能分页查询
- 🎯 **代码生成**: 自动生成Mapper和实体类
- 🔍 **慢查询监控**: SQL性能监控和优化建议

#### rui-common-redis
**Redis操作模块** - 缓存和分布式功能支持
- 💾 **缓存操作**: 统一的缓存操作接口
- 🔄 **序列化**: 多种序列化方式支持
- ⏰ **过期策略**: 灵活的缓存过期配置
- 📊 **缓存监控**: 缓存命中率和性能监控
- 🔧 **工具类**: 常用的Redis操作工具
- 🎯 **分布式锁**: 基于Redis的分布式锁实现

#### rui-common-mongo
**MongoDB数据访问模块** - NoSQL数据库访问支持
- 📄 **文档操作**: 统一的MongoDB文档操作
- 🏗️ **基础实体**: 包含审计字段的实体基类
- 🔍 **查询构建**: 动态查询条件构建
- 📊 **聚合查询**: 复杂聚合查询支持
- 🗑️ **软删除**: 逻辑删除和数据恢复
- 👥 **多租户**: 租户数据隔离支持

### 🔧 功能增强模块

#### rui-common-lock
**分布式锁模块** - 基于Redisson的分布式锁实现
- 🔒 **可重入锁**: 支持可重入的分布式锁
- ⏱️ **锁超时**: 自动释放和超时处理
- 🎯 **注解支持**: 基于注解的声明式锁
- 🔄 **锁续期**: 自动续期机制
- 📊 **锁监控**: 锁使用情况监控
- 🚀 **高性能**: 基于Lua脚本的原子操作

#### rui-common-tenant
**多租户模块** - 企业级多租户数据隔离
- 🏢 **租户隔离**: 数据库级别的租户隔离
- 🔄 **自动切换**: 基于请求上下文的自动切换
- 🎯 **注解支持**: 声明式租户配置
- 📊 **租户管理**: 租户信息管理和配置
- 🔍 **数据过滤**: 自动的租户数据过滤
- 🛡️ **安全保障**: 防止跨租户数据访问

#### rui-common-ratelimit
**限流模块** - 基于Redis+Lua的高性能限流
- 🚦 **多种算法**: 令牌桶、滑动窗口等限流算法
- 🎯 **注解支持**: 基于注解的声明式限流
- 🔧 **灵活配置**: 支持多维度限流配置
- 📊 **实时监控**: 限流状态实时监控
- ⚡ **高性能**: 基于Lua脚本的原子操作
- 🎨 **自定义**: 支持自定义限流策略

#### rui-common-idempotent
**幂等性模块** - 防止重复操作的幂等性保障
- 🔄 **重复检测**: 基于多种策略的重复请求检测
- 🎯 **注解支持**: 声明式幂等性配置
- 💾 **存储支持**: Redis、数据库等存储方式
- ⏰ **过期机制**: 自动清理过期的幂等性记录
- 🔧 **灵活配置**: 支持自定义幂等性策略
- 📊 **监控统计**: 幂等性操作统计和监控

### 🌐 服务通信模块

#### rui-common-feign
**Feign客户端模块** - 微服务间通信支持
- 🔗 **服务调用**: 声明式的HTTP客户端
- 🔄 **负载均衡**: 集成Ribbon负载均衡
- 🛡️ **熔断降级**: Hystrix熔断器支持
- 📊 **调用监控**: 服务调用链路监控
- 🔧 **自动配置**: 开箱即用的配置
- 🎯 **拦截器**: 请求和响应拦截处理

#### rui-common-mq
**RocketMQ消息队列模块** - 异步消息处理支持
- 📨 **消息发送**: 同步、异步、延时消息发送
- 📥 **消息消费**: 集群和广播消费模式
- 🔄 **事务消息**: 分布式事务消息支持
- 📊 **消息监控**: 消息发送和消费监控
- 🎯 **注解支持**: 基于注解的消息处理
- 🔧 **自动配置**: 简化的配置和使用

### 📊 监控运维模块

#### rui-common-log
**日志模块** - 集成ELK的统一日志处理
- 📝 **结构化日志**: JSON格式的结构化日志输出
- 🔍 **链路追踪**: 集成分布式链路追踪
- 📊 **日志聚合**: ELK集成和日志聚合
- 🎯 **注解支持**: 基于注解的操作日志
- 🔧 **灵活配置**: 多环境日志配置
- 📈 **性能监控**: 日志性能和统计监控

#### rui-common-monitoring
**监控模块** - 应用性能监控和健康检查
- 📊 **指标收集**: 应用性能指标收集
- 💓 **健康检查**: 应用和依赖服务健康检查
- 🚨 **告警通知**: 多渠道告警通知支持
- 📈 **性能分析**: 应用性能分析和优化建议
- 🔧 **自定义指标**: 支持自定义业务指标
- 📱 **监控面板**: 集成Grafana监控面板

#### rui-common-tracing
**链路追踪模块** - 基于OpenTelemetry的分布式链路追踪
- 🔍 **链路追踪**: 完整的请求链路追踪
- 📊 **性能分析**: 接口性能分析和优化
- 🎯 **注解支持**: 基于注解的链路追踪
- 🔧 **多种导出**: Jaeger、Zipkin等导出支持
- 📈 **实时监控**: 实时的链路监控和分析
- 🚀 **高性能**: 低开销的追踪实现

### 🔧 工具模块

#### rui-common-config
**配置管理模块** - 统一的配置管理和动态配置
- ⚙️ **配置中心**: Nacos配置中心集成
- 🔄 **动态刷新**: 配置的动态刷新和热更新
- 🔒 **配置加密**: 敏感配置的加密存储
- 📊 **配置监控**: 配置变更监控和审计
- 🎯 **环境隔离**: 多环境配置隔离
- 🔧 **类型安全**: 强类型的配置绑定

#### rui-common-utils
**通用工具模块** - 扩展的工具类和辅助功能
- 🔧 **工具扩展**: 更多专用工具类
- 📄 **文件处理**: 文件上传、下载、处理
- 🎨 **图片处理**: 图片压缩、裁剪、水印
- 📊 **Excel处理**: Excel导入导出工具
- 🔐 **加密解密**: 多种加密算法支持
- 📱 **二维码**: 二维码生成和解析

## 🚀 快速开始

### 1. 环境要求

- JDK 21+
- Maven 3.8+
- Redis 7.x
- MySQL 8.x (可选)
- MongoDB 4.4+ (可选)

### 2. 添加依赖

在你的Spring Boot项目中添加需要的模块依赖：

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-web</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-security</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-redis</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. 配置文件

在`application.yml`中添加相应配置：

```yaml
rui:
  web:
    enabled: true
  security:
    enabled: true
    jwt:
      secret: your-jwt-secret
  redis:
    enabled: true
```

### 4. 启用自动配置

在启动类上添加注解：

```java
@SpringBootApplication
@EnableRuiFramework
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 📖 使用文档

每个模块都提供了详细的使用文档和示例代码，请查看各模块目录下的README.md文件：

- [rui-common-core 使用文档](./rui-common/rui-common-core/README.md)
- [rui-common-web 使用文档](./rui-common/rui-common-web/README.md)
- [rui-common-security 使用文档](./rui-common/rui-common-security/README.md)
- [rui-common-redis 使用文档](./rui-common/rui-common-redis/README.md)
- [rui-common-mybatis 使用文档](./rui-common/rui-common-mybatis/README.md)
- [rui-common-mongo 使用文档](./rui-common/rui-common-mongo/README.md)
- [rui-common-lock 使用文档](./rui-common/rui-common-lock/README.md)
- [rui-common-tenant 使用文档](./rui-common/rui-common-tenant/README.md)
- [rui-common-ratelimit 使用文档](./rui-common/rui-common-ratelimit/README.md)
- [rui-common-idempotent 使用文档](./rui-common/rui-common-idempotent/README.md)
- [rui-common-feign 使用文档](./rui-common/rui-common-feign/README.md)
- [rui-common-mq 使用文档](./rui-common/rui-common-mq/README.md)
- [rui-common-log 使用文档](./rui-common/rui-common-log/README.md)
- [rui-common-monitoring 使用文档](./rui-common/rui-common-monitoring/README.md)
- [rui-common-tracing 使用文档](./rui-common/rui-common-tracing/README.md)

## 🏗️ 架构设计

### 分层架构

```
┌─────────────────────────────────────────┐
│              应用层 (Application)        │
├─────────────────────────────────────────┤
│              业务层 (Business)          │
├─────────────────────────────────────────┤
│              服务层 (Service)           │
├─────────────────────────────────────────┤
│              数据层 (Data Access)       │
├─────────────────────────────────────────┤
│              基础设施层 (Infrastructure) │
└─────────────────────────────────────────┘
```

### 模块依赖关系

```
rui-common-core (核心基础)
    ↑
    ├── rui-common-web
    ├── rui-common-security
    ├── rui-common-redis
    ├── rui-common-mybatis
    ├── rui-common-mongo
    ├── rui-common-lock
    ├── rui-common-tenant
    ├── rui-common-ratelimit
    ├── rui-common-idempotent
    ├── rui-common-feign
    ├── rui-common-mq
    ├── rui-common-log
    ├── rui-common-monitoring
    └── rui-common-tracing
```

## 🔧 最佳实践

### 1. 模块选择
- 根据项目需求选择合适的模块
- 避免引入不必要的依赖
- 优先使用核心模块

### 2. 配置管理
- 使用统一的配置前缀
- 合理设置默认值
- 支持多环境配置

### 3. 性能优化
- 合理使用缓存
- 优化数据库查询
- 监控关键指标

### 4. 安全规范
- 启用安全模块
- 定期更新依赖
- 遵循安全最佳实践

## 🤝 贡献指南

欢迎贡献代码和提出建议！请遵循以下步骤：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 联系我们

- 项目地址: [https://github.com/chenhongze1/rui-cloud](https://github.com/chenhongze1/rui-cloud)
- 问题反馈: [Issues](https://github.com/chenhongze1/rui-cloud/issues)
- 邮箱: support@rui-framework.com

## 🙏 致谢

感谢所有为这个项目做出贡献的开发者！