# rui-common-mongo

MongoDB数据访问模块，提供MongoDB的统一数据访问层，包含基础实体、仓储、服务层以及常用工具类。

## 功能特性

### 🚀 核心功能
- **基础实体类**: 提供统一的实体基类，包含审计字段、多租户、软删除等功能
- **仓储层**: 基于Spring Data MongoDB的Repository模式，提供常用查询方法
- **服务层**: 统一的Service接口和实现，封装常用业务操作
- **工具类**: MongoDB操作工具类，提供便捷的数据库操作方法
- **自动配置**: Spring Boot自动配置，开箱即用

### 📊 数据特性
- **审计功能**: 自动记录创建时间、更新时间、创建人、更新人
- **乐观锁**: 基于版本号的乐观锁机制
- **软删除**: 逻辑删除功能，支持数据恢复
- **多租户**: 内置租户隔离支持
- **分页查询**: 统一的分页查询接口
- **索引管理**: 自动索引创建和管理

### 🔧 技术特性
- **事务支持**: MongoDB事务管理
- **连接池**: 高性能连接池配置
- **类型转换**: 自定义类型转换器
- **验证支持**: Bean Validation集成
- **监控集成**: 与监控模块集成

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-mongo</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 启用MongoDB

在`application.yml`中配置：

```yaml
rui:
  mongo:
    enabled: true

spring:
  data:
    mongodb:
      database: your_database
      host: localhost
      port: 27017
```

### 3. 创建实体类

```java
@Document(collection = "users")
public class User extends BaseEntity {
    private String username;
    private String email;
    private Integer age;
    
    // getters and setters
}
```

### 4. 创建Repository

```java
@Repository
public interface UserRepository extends BaseRepository<User> {
    List<User> findByUsername(String username);
    Page<User> findByAgeGreaterThan(Integer age, Pageable pageable);
}
```

### 5. 创建Service

```java
@Service
public class UserService extends BaseServiceImpl<User, UserRepository> {
    
    public List<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }
    
    public PageResult<User> findAdults(Pageable pageable) {
        Page<User> page = repository.findByAgeGreaterThan(18, pageable);
        return PageResult.of(page.getContent(), page.getTotalElements(), 
                           page.getNumber() + 1, page.getSize());
    }
}
```

## 使用方式

### 基础CRUD操作

```java
@Autowired
private UserService userService;

// 保存
User user = new User();
user.setUsername("john");
user.setEmail("john@example.com");
User saved = userService.save(user);

// 查询
Optional<User> found = userService.findById(saved.getId());
List<User> all = userService.findAllNotDeleted();

// 分页查询
Pageable pageable = PageRequest.of(0, 10);
PageResult<User> page = userService.findPageNotDeleted(pageable);

// 软删除
userService.logicalDelete(saved.getId());

// 恢复
userService.restore(saved.getId());
```

### 使用工具类

```java
@Autowired
private MongoUtils mongoUtils;

// 动态查询
Query query = MongoUtils.buildEqualQuery("username", "john");
List<User> users = mongoUtils.find(query, User.class);

// 范围查询
Query rangeQuery = MongoUtils.buildRangeQuery("age", 18, 65);
List<User> adults = mongoUtils.find(rangeQuery, User.class);

// 模糊查询
Query likeQuery = MongoUtils.buildLikeQuery("email", "@gmail.com");
List<User> gmailUsers = mongoUtils.find(likeQuery, User.class);

// 批量更新
Query updateQuery = MongoUtils.buildEqualQuery("status", "inactive");
Update update = MongoUtils.buildUpdate(Map.of("status", "active"));
mongoUtils.updateMulti(updateQuery, update, User.class);
```

### 聚合查询

```java
// 使用Aggregation进行复杂查询
Aggregation aggregation = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("age").gte(18)),
    Aggregation.group("department").count().as("count"),
    Aggregation.sort(Sort.Direction.DESC, "count")
);

AggregationResults<Document> results = mongoUtils.aggregate(
    aggregation, "users", Document.class
);
```

## 配置说明

### 基础配置

```yaml
rui:
  mongo:
    enabled: true                    # 是否启用MongoDB模块
    audit:
      enabled: true                  # 是否启用审计功能
      optimistic-locking: true       # 是否启用乐观锁
    page:
      default-page-size: 20          # 默认页大小
      max-page-size: 1000           # 最大页大小
    index:
      auto-create-index: true        # 是否自动创建索引
      check-on-startup: true         # 启动时检查索引
```

### 连接配置

```yaml
spring:
  data:
    mongodb:
      database: your_database
      host: localhost
      port: 27017
      username: your_username
      password: your_password
      authentication-database: admin
      
      # 或使用URI连接
      uri: mongodb://username:password@localhost:27017/database?authSource=admin
```

### 连接池配置

```yaml
spring:
  data:
    mongodb:
      options:
        min-connections-per-host: 5
        connections-per-host: 100
        connect-timeout: 10000
        socket-timeout: 30000
        max-wait-time: 120000
        max-connection-idle-time: 60000
        max-connection-life-time: 600000
```

## 高级用法

### 自定义Repository方法

```java
public interface UserRepository extends BaseRepository<User> {
    
    @Query("{'username': ?0, 'deleted': 0}")
    Optional<User> findByUsernameAndNotDeleted(String username);
    
    @Query("{'age': {$gte: ?0, $lte: ?1}, 'deleted': 0}")
    List<User> findByAgeBetweenAndNotDeleted(Integer minAge, Integer maxAge);
    
    @Aggregation(pipeline = {
        "{ '$match': { 'deleted': 0 } }",
        "{ '$group': { '_id': '$department', 'count': { '$sum': 1 } } }",
        "{ '$sort': { 'count': -1 } }"
    })
    List<DepartmentCount> countByDepartment();
}
```

### 事务管理

```java
@Service
@Transactional
public class UserService extends BaseServiceImpl<User, UserRepository> {
    
    @Transactional(rollbackFor = Exception.class)
    public void transferUser(String fromDept, String toDept, String userId) {
        User user = findById(userId).orElseThrow();
        user.setDepartment(toDept);
        save(user);
        
        // 其他相关操作
        updateDepartmentStats(fromDept, toDept);
    }
}
```

### 自定义类型转换

```java
@Component
public class CustomMongoConverters {
    
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new LocalDateTimeToStringConverter());
        converters.add(new StringToLocalDateTimeConverter());
        return new MongoCustomConversions(converters);
    }
    
    @WritingConverter
    public static class LocalDateTimeToStringConverter 
            implements Converter<LocalDateTime, String> {
        @Override
        public String convert(LocalDateTime source) {
            return source.toString();
        }
    }
    
    @ReadingConverter
    public static class StringToLocalDateTimeConverter 
            implements Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            return LocalDateTime.parse(source);
        }
    }
}
```

## 监控和观察

### 健康检查

模块自动注册MongoDB健康检查端点：

```bash
# 检查MongoDB连接状态
curl http://localhost:8080/actuator/health/mongo
```

### 指标监控

集成Micrometer指标：

- `mongodb.connections.active`: 活跃连接数
- `mongodb.connections.total`: 总连接数
- `mongodb.operations.total`: 操作总数
- `mongodb.operations.duration`: 操作耗时

### 日志配置

```yaml
logging:
  level:
    org.springframework.data.mongodb: DEBUG
    com.mongodb: INFO
    com.rui.common.mongo: DEBUG
```

## 性能优化

### 索引优化

```java
@Document(collection = "users")
@CompoundIndex(name = "username_email_idx", def = "{'username': 1, 'email': 1}")
@CompoundIndex(name = "tenant_create_time_idx", def = "{'tenantId': 1, 'createTime': -1}")
public class User extends BaseEntity {
    @Indexed(unique = true)
    private String username;
    
    @Indexed
    private String email;
    
    @TextIndexed
    private String description;
}
```

### 查询优化

```java
// 使用投影减少数据传输
Query query = new Query(Criteria.where("department").is("IT"));
query.fields().include("username").include("email");
List<User> users = mongoTemplate.find(query, User.class);

// 使用批量操作
List<User> users = Arrays.asList(user1, user2, user3);
mongoTemplate.insertAll(users);

// 使用聚合管道优化复杂查询
Aggregation aggregation = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("deleted").is(0)),
    Aggregation.lookup("departments", "departmentId", "_id", "department"),
    Aggregation.unwind("department"),
    Aggregation.project("username", "email", "department.name")
);
```

### 连接池优化

```yaml
spring:
  data:
    mongodb:
      options:
        # 根据应用负载调整连接池大小
        min-connections-per-host: 10
        connections-per-host: 200
        # 优化超时设置
        connect-timeout: 5000
        socket-timeout: 60000
        # 连接生命周期管理
        max-connection-idle-time: 300000
        max-connection-life-time: 1800000
```

## 故障排除

### 常见问题

1. **连接超时**
   ```yaml
   spring:
     data:
       mongodb:
         options:
           connect-timeout: 30000
           socket-timeout: 60000
   ```

2. **索引创建失败**
   ```yaml
   rui:
     mongo:
       index:
         auto-create-index: false  # 手动管理索引
   ```

3. **事务不支持**
   - 确保MongoDB版本 >= 4.0
   - 使用副本集或分片集群
   - 启用事务配置

### 调试技巧

```yaml
# 启用详细日志
logging:
  level:
    org.springframework.data.mongodb.core.MongoTemplate: DEBUG
    org.springframework.data.mongodb.repository: DEBUG
    com.mongodb.diagnostics.logging: DEBUG
```

## 最佳实践

### 1. 实体设计
- 继承BaseEntity获得审计功能
- 合理使用索引注解
- 避免深层嵌套文档
- 使用合适的字段类型

### 2. 查询优化
- 优先使用索引字段查询
- 使用投影减少数据传输
- 合理使用聚合管道
- 避免全表扫描

### 3. 事务使用
- 仅在必要时使用事务
- 保持事务简短
- 避免长时间持有事务
- 合理设置超时时间

### 4. 性能监控
- 监控连接池状态
- 跟踪慢查询
- 定期分析索引使用情况
- 监控内存使用

## 版本兼容性

- **Spring Boot**: 3.2+
- **Spring Data MongoDB**: 4.2+
- **MongoDB**: 4.4+
- **Java**: 21+

## 更新日志

### v1.0.0
- 初始版本发布
- 基础CRUD功能
- 审计和软删除支持
- 多租户功能
- 工具类和自动配置
- 完整的文档和示例