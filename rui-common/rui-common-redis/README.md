# RUI Common Redis

## 模块简介

RUI框架的Redis增强模块，提供Redis的统一配置、序列化优化、性能调优、连接池管理、缓存服务等功能，简化Redis在Spring Boot项目中的使用。

## 主要功能

### 1. 自动配置
- **RedisOptimizationAutoConfiguration**: Redis优化自动配置类

### 2. 配置管理
- **RedisConfig**: Redis核心配置类
- **FastJson2JsonRedisSerializer**: FastJson2序列化器

### 3. 性能优化
- **RedisPerformanceOptimizer**: Redis性能优化器

### 4. 连接池管理
- **RedisPoolConfig**: Redis连接池配置
- **RedisPoolManager**: Redis连接池管理器

### 5. 缓存服务
- **RedisService**: Redis统一服务接口

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-redis</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Spring Boot Redis Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Lettuce连接池 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>

<!-- FastJson2序列化 -->
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.43</version>
</dependency>
```

### 2. 基本使用

```java
@Service
public class UserService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 缓存用户信息
     */
    public void cacheUser(User user) {
        String key = "user:" + user.getId();
        redisService.set(key, user, 3600); // 缓存1小时
    }
    
    /**
     * 获取缓存的用户信息
     */
    public User getCachedUser(Long userId) {
        String key = "user:" + userId;
        return redisService.get(key, User.class);
    }
    
    /**
     * 删除用户缓存
     */
    public void removeCachedUser(Long userId) {
        String key = "user:" + userId;
        redisService.delete(key);
    }
    
    /**
     * 批量缓存用户
     */
    public void batchCacheUsers(List<User> users) {
        Map<String, Object> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put("user:" + user.getId(), user);
        }
        redisService.multiSet(userMap);
    }
}
```

### 3. 字符串操作

```java
@Component
public class StringCacheService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 设置字符串值
     */
    public void setValue(String key, String value) {
        redisService.set(key, value);
    }
    
    /**
     * 设置带过期时间的字符串值
     */
    public void setValueWithExpire(String key, String value, long seconds) {
        redisService.set(key, value, seconds);
    }
    
    /**
     * 获取字符串值
     */
    public String getValue(String key) {
        return redisService.get(key, String.class);
    }
    
    /**
     * 原子递增
     */
    public Long increment(String key) {
        return redisService.increment(key);
    }
    
    /**
     * 原子递增指定步长
     */
    public Long incrementBy(String key, long delta) {
        return redisService.incrementBy(key, delta);
    }
    
    /**
     * 原子递减
     */
    public Long decrement(String key) {
        return redisService.decrement(key);
    }
}
```

### 4. 哈希操作

```java
@Component
public class HashCacheService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 设置哈希字段
     */
    public void setHashField(String key, String field, Object value) {
        redisService.hSet(key, field, value);
    }
    
    /**
     * 获取哈希字段
     */
    public <T> T getHashField(String key, String field, Class<T> clazz) {
        return redisService.hGet(key, field, clazz);
    }
    
    /**
     * 批量设置哈希字段
     */
    public void setHashFields(String key, Map<String, Object> hash) {
        redisService.hMultiSet(key, hash);
    }
    
    /**
     * 获取所有哈希字段
     */
    public Map<String, Object> getAllHashFields(String key) {
        return redisService.hGetAll(key);
    }
    
    /**
     * 删除哈希字段
     */
    public void deleteHashField(String key, String... fields) {
        redisService.hDelete(key, fields);
    }
    
    /**
     * 判断哈希字段是否存在
     */
    public boolean hasHashField(String key, String field) {
        return redisService.hExists(key, field);
    }
}
```

### 5. 列表操作

```java
@Component
public class ListCacheService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 左侧推入元素
     */
    public void leftPush(String key, Object value) {
        redisService.lLeftPush(key, value);
    }
    
    /**
     * 右侧推入元素
     */
    public void rightPush(String key, Object value) {
        redisService.lRightPush(key, value);
    }
    
    /**
     * 左侧弹出元素
     */
    public <T> T leftPop(String key, Class<T> clazz) {
        return redisService.lLeftPop(key, clazz);
    }
    
    /**
     * 右侧弹出元素
     */
    public <T> T rightPop(String key, Class<T> clazz) {
        return redisService.lRightPop(key, clazz);
    }
    
    /**
     * 获取列表范围元素
     */
    public <T> List<T> getRange(String key, long start, long end, Class<T> clazz) {
        return redisService.lRange(key, start, end, clazz);
    }
    
    /**
     * 获取列表长度
     */
    public Long getListSize(String key) {
        return redisService.lSize(key);
    }
}
```

### 6. 集合操作

```java
@Component
public class SetCacheService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 添加集合元素
     */
    public void addToSet(String key, Object... values) {
        redisService.sAdd(key, values);
    }
    
    /**
     * 移除集合元素
     */
    public void removeFromSet(String key, Object... values) {
        redisService.sRemove(key, values);
    }
    
    /**
     * 判断元素是否在集合中
     */
    public boolean isMemberOfSet(String key, Object value) {
        return redisService.sIsMember(key, value);
    }
    
    /**
     * 获取集合所有元素
     */
    public <T> Set<T> getSetMembers(String key, Class<T> clazz) {
        return redisService.sMembers(key, clazz);
    }
    
    /**
     * 获取集合大小
     */
    public Long getSetSize(String key) {
        return redisService.sSize(key);
    }
    
    /**
     * 随机获取集合元素
     */
    public <T> T randomMember(String key, Class<T> clazz) {
        return redisService.sRandomMember(key, clazz);
    }
}
```

### 7. 有序集合操作

```java
@Component
public class ZSetCacheService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 添加有序集合元素
     */
    public void addToZSet(String key, Object value, double score) {
        redisService.zAdd(key, value, score);
    }
    
    /**
     * 移除有序集合元素
     */
    public void removeFromZSet(String key, Object... values) {
        redisService.zRemove(key, values);
    }
    
    /**
     * 获取元素分数
     */
    public Double getScore(String key, Object value) {
        return redisService.zScore(key, value);
    }
    
    /**
     * 增加元素分数
     */
    public Double incrementScore(String key, Object value, double delta) {
        return redisService.zIncrementScore(key, value, delta);
    }
    
    /**
     * 获取排名范围元素
     */
    public <T> Set<T> getRangeByRank(String key, long start, long end, Class<T> clazz) {
        return redisService.zRange(key, start, end, clazz);
    }
    
    /**
     * 获取分数范围元素
     */
    public <T> Set<T> getRangeByScore(String key, double min, double max, Class<T> clazz) {
        return redisService.zRangeByScore(key, min, max, clazz);
    }
    
    /**
     * 获取有序集合大小
     */
    public Long getZSetSize(String key) {
        return redisService.zSize(key);
    }
}
```

## 配置属性

```yaml
# Redis配置
spring:
  redis:
    # 服务器地址
    host: localhost
    # 服务器端口
    port: 6379
    # 数据库索引
    database: 0
    # 密码
    password: 
    # 连接超时时间
    timeout: 10s
    # SSL配置
    ssl: false
    
    # Lettuce连接池配置
    lettuce:
      pool:
        # 连接池最大连接数
        max-active: 20
        # 连接池最大阻塞等待时间
        max-wait: -1ms
        # 连接池中的最大空闲连接
        max-idle: 10
        # 连接池中的最小空闲连接
        min-idle: 5
        # 空闲连接检测周期
        time-between-eviction-runs: 60s
      # 关闭超时时间
      shutdown-timeout: 100ms
    
    # 集群配置（可选）
    cluster:
      nodes:
        - 192.168.1.100:7000
        - 192.168.1.100:7001
        - 192.168.1.100:7002
        - 192.168.1.101:7000
        - 192.168.1.101:7001
        - 192.168.1.101:7002
      max-redirects: 3
    
    # 哨兵配置（可选）
    sentinel:
      master: mymaster
      nodes:
        - 192.168.1.100:26379
        - 192.168.1.101:26379
        - 192.168.1.102:26379
      password: sentinel_password

# RUI Redis扩展配置
rui:
  redis:
    # 性能优化配置
    optimization:
      # 启用性能优化
      enabled: true
      # 批量操作大小
      batch-size: 1000
      # 管道操作启用
      pipeline-enabled: true
      # 压缩启用
      compression-enabled: false
      # 压缩阈值（字节）
      compression-threshold: 1024
    
    # 序列化配置
    serialization:
      # 序列化类型：fastjson2, jackson, jdk
      type: fastjson2
      # 启用类型信息
      enable-type-info: true
    
    # 缓存配置
    cache:
      # 默认过期时间（秒）
      default-expire: 3600
      # 空值缓存时间（秒）
      null-value-expire: 300
      # 缓存前缀
      key-prefix: "rui:"
      # 启用缓存统计
      enable-statistics: true
    
    # 监控配置
    monitoring:
      # 启用监控
      enabled: true
      # 慢查询阈值（毫秒）
      slow-query-threshold: 100
      # 监控采样率
      sample-rate: 0.1
```

## 高级功能

### 1. 分布式锁

```java
@Component
public class DistributedLockService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 获取分布式锁
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime) {
        String script = "if redis.call('get', KEYS[1]) == false then " +
                       "return redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2]) " +
                       "else return false end";
        
        Object result = redisService.execute(script, 
            Collections.singletonList(lockKey), 
            requestId, String.valueOf(expireTime));
        
        return "OK".equals(result);
    }
    
    /**
     * 释放分布式锁
     */
    public boolean releaseLock(String lockKey, String requestId) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                       "return redis.call('del', KEYS[1]) " +
                       "else return 0 end";
        
        Object result = redisService.execute(script, 
            Collections.singletonList(lockKey), requestId);
        
        return Long.valueOf(1).equals(result);
    }
}
```

### 2. 限流器

```java
@Component
public class RateLimiterService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 滑动窗口限流
     */
    public boolean isAllowed(String key, int limit, int windowSize) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSize * 1000L;
        
        String script = 
            "redis.call('zremrangebyscore', KEYS[1], 0, ARGV[1]) " +
            "local count = redis.call('zcard', KEYS[1]) " +
            "if count < tonumber(ARGV[2]) then " +
            "  redis.call('zadd', KEYS[1], ARGV[3], ARGV[3]) " +
            "  redis.call('expire', KEYS[1], ARGV[4]) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";
        
        Object result = redisService.execute(script,
            Collections.singletonList(key),
            String.valueOf(windowStart),
            String.valueOf(limit),
            String.valueOf(now),
            String.valueOf(windowSize));
        
        return Long.valueOf(1).equals(result);
    }
}
```

### 3. 缓存预热

```java
@Component
public class CacheWarmupService {
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 预热用户缓存
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupUserCache() {
        log.info("开始预热用户缓存...");
        
        // 获取热点用户列表
        List<User> hotUsers = userService.getHotUsers();
        
        // 批量缓存
        Map<String, Object> cacheMap = new HashMap<>();
        for (User user : hotUsers) {
            cacheMap.put("user:" + user.getId(), user);
        }
        
        redisService.multiSet(cacheMap);
        
        // 设置过期时间
        for (String key : cacheMap.keySet()) {
            redisService.expire(key, 3600);
        }
        
        log.info("用户缓存预热完成，共预热{}个用户", hotUsers.size());
    }
}
```

### 4. 缓存穿透防护

```java
@Component
public class CacheService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 防缓存穿透的查询
     */
    public <T> T getWithBloomFilter(String key, Class<T> clazz, Supplier<T> dataLoader) {
        // 先检查布隆过滤器
        if (!bloomFilterExists(key)) {
            return null;
        }
        
        // 从缓存获取
        T result = redisService.get(key, clazz);
        if (result != null) {
            return result;
        }
        
        // 从数据源加载
        result = dataLoader.get();
        if (result != null) {
            redisService.set(key, result, 3600);
        } else {
            // 缓存空值，防止缓存穿透
            redisService.set(key, "", 300);
        }
        
        return result;
    }
    
    private boolean bloomFilterExists(String key) {
        // 布隆过滤器检查逻辑
        return redisService.execute(
            "return redis.call('bf.exists', KEYS[1], ARGV[1])",
            Collections.singletonList("bloom_filter"),
            key
        ).equals(1L);
    }
}
```

## 性能优化

### 1. 连接池优化
- 合理设置连接池大小
- 启用连接空闲检测
- 配置合适的超时时间

### 2. 序列化优化
- 使用FastJson2提高序列化性能
- 启用压缩减少网络传输
- 合理选择序列化方式

### 3. 批量操作
- 使用Pipeline减少网络往返
- 批量设置和获取操作
- 合理控制批量大小

### 4. 内存优化
- 设置合理的过期时间
- 使用压缩减少内存占用
- 定期清理无用数据

## 监控指标

### 1. 连接指标
- 活跃连接数
- 空闲连接数
- 连接创建/销毁速率

### 2. 操作指标
- 命令执行次数
- 命令执行耗时
- 慢查询统计

### 3. 内存指标
- 内存使用量
- 键数量统计
- 过期键清理

### 4. 网络指标
- 网络IO统计
- 连接错误率
- 超时统计

## 最佳实践

### 1. 键命名规范
- 使用有意义的前缀
- 避免键名冲突
- 合理设置过期时间

### 2. 数据结构选择
- 根据场景选择合适的数据结构
- 避免大键值对
- 合理使用哈希结构

### 3. 缓存策略
- 实现缓存更新策略
- 处理缓存雪崩和穿透
- 合理设置缓存层级

### 4. 安全考虑
- 启用密码认证
- 配置网络安全
- 限制危险命令

## 注意事项

1. **连接管理**: 合理配置连接池，避免连接泄漏
2. **内存管理**: 设置合理的过期时间，避免内存溢出
3. **序列化**: 选择合适的序列化方式，注意兼容性
4. **集群模式**: 注意键的分布和槽位迁移
5. **事务操作**: Redis事务的原子性限制
6. **大键处理**: 避免存储过大的键值对

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x
- Redis版本: 7.x
- Lettuce版本: 6.x