# RUI Common MyBatis

## 模块简介

RUI框架的MyBatis增强模块，基于MyBatis-Plus提供数据库操作的统一配置、基础实体类、自动填充、分页工具等功能，简化数据库开发。

## 主要功能

### 1. MyBatis-Plus配置
- **MybatisPlusConfig**: MyBatis-Plus统一配置类

### 2. 基础实体
- **BaseEntity**: 基础实体类，包含通用字段

### 3. 自动填充
- **MyMetaObjectHandler**: 元数据自动填充处理器

### 4. 分页工具
- **PageUtils**: 分页工具类

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-mybatis</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- MyBatis-Plus依赖 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.5</version>
</dependency>

<!-- 数据库驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. 实体类定义

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    @TableField("username")
    private String username;
    
    @TableField("password")
    private String password;
    
    @TableField("email")
    private String email;
    
    @TableField("phone")
    private String phone;
    
    @TableField("status")
    private Integer status;
    
    @TableField("avatar")
    private String avatar;
    
    // 逻辑删除字段（BaseEntity中已包含）
    // private Integer deleted;
    
    // 创建时间、更新时间等字段（BaseEntity中已包含）
    // private LocalDateTime createTime;
    // private LocalDateTime updateTime;
    // private Long createBy;
    // private Long updateBy;
}
```

### 3. Mapper接口定义

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);
    
    /**
     * 查询活跃用户列表
     */
    @Select("SELECT * FROM sys_user WHERE status = 1 AND deleted = 0")
    List<User> selectActiveUsers();
    
    /**
     * 自定义分页查询
     */
    IPage<User> selectUserPage(Page<User> page, @Param("query") UserQuery query);
}
```

### 4. Service层使用

```java
@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    
    /**
     * 根据用户名查询用户
     */
    public User getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }
    
    /**
     * 分页查询用户
     */
    public IPage<User> getUserPage(UserQuery query) {
        Page<User> page = PageUtils.buildPage(query);
        return baseMapper.selectUserPage(page, query);
    }
    
    /**
     * 批量创建用户
     */
    @Transactional
    public boolean batchCreateUsers(List<User> users) {
        return saveBatch(users);
    }
    
    /**
     * 软删除用户
     */
    public boolean deleteUser(Long id) {
        return removeById(id); // 自动使用逻辑删除
    }
    
    /**
     * 条件查询示例
     */
    public List<User> getUsersByCondition(String username, Integer status) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(username), User::getUsername, username)
               .eq(status != null, User::getStatus, status)
               .orderByDesc(User::getCreateTime);
        return list(wrapper);
    }
}
```

### 5. Controller层使用

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public R<IPage<User>> getUsers(UserQuery query) {
        IPage<User> page = userService.getUserPage(query);
        return R.ok(page);
    }
    
    @GetMapping("/{id}")
    public R<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        return R.ok(user);
    }
    
    @PostMapping
    public R<Void> createUser(@RequestBody User user) {
        userService.save(user);
        return R.ok();
    }
    
    @PutMapping("/{id}")
    public R<Void> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateById(user);
        return R.ok();
    }
    
    @DeleteMapping("/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }
}
```

## 配置属性

```yaml
# MyBatis-Plus配置
mybatis-plus:
  # 配置文件位置
  config-location: classpath:mybatis-config.xml
  # Mapper XML文件位置
  mapper-locations: classpath*:mapper/**/*.xml
  # 实体类包路径
  type-aliases-package: com.rui.**.entity
  # 全局配置
  global-config:
    # 数据库字段驼峰下划线转换
    db-column-underline: true
    # 刷新Mapper
    refresh-mapper: true
    # 逻辑删除配置
    db-config:
      # 逻辑删除字段
      logic-delete-field: deleted
      # 逻辑删除值
      logic-delete-value: 1
      # 逻辑未删除值
      logic-not-delete-value: 0
      # 主键类型
      id-type: ASSIGN_ID
      # 表名前缀
      table-prefix: ""
  # 配置
  configuration:
    # 开启驼峰命名转换
    map-underscore-to-camel-case: true
    # 开启缓存
    cache-enabled: true
    # 延迟加载
    lazy-loading-enabled: true
    # 积极延迟加载
    aggressive-lazy-loading: false
    # 允许多结果集
    multiple-result-sets-enabled: true
    # 使用列标签
    use-column-label: true
    # 使用生成的键
    use-generated-keys: true
    # 自动映射行为
    auto-mapping-behavior: PARTIAL
    # 自动映射未知列行为
    auto-mapping-unknown-column-behavior: WARNING
    # 默认执行器类型
    default-executor-type: SIMPLE
    # 默认语句超时时间
    default-statement-timeout: 25
    # 默认获取大小
    default-fetch-size: 100
    # 安全行边界
    safe-row-bounds-enabled: false
    # 本地缓存范围
    local-cache-scope: SESSION
    # JDBC类型为空时的处理
    jdbc-type-for-null: OTHER
    # 延迟加载触发方法
    lazy-load-trigger-methods: equals,clone,hashCode,toString
    # 日志实现
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# 数据源配置
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/rui_db?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
    username: root
    password: password
    # 连接池配置
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      auto-commit: true
      idle-timeout: 30000
      pool-name: RuiHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
```

## BaseEntity说明

```java
@Data
@MappedSuperclass
public abstract class BaseEntity implements Serializable {
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /**
     * 逻辑删除标识
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
    
    /**
     * 版本号（乐观锁）
     */
    @Version
    @TableField("version")
    private Integer version;
}
```

## 自动填充功能

自动填充处理器会在插入和更新时自动填充以下字段：
- **createTime**: 创建时间（仅插入时填充）
- **updateTime**: 更新时间（插入和更新时填充）
- **createBy**: 创建人（仅插入时填充）
- **updateBy**: 更新人（插入和更新时填充）

## 分页工具使用

```java
// 基础分页
Page<User> page = PageUtils.buildPage(pageNum, pageSize);
IPage<User> result = userService.page(page);

// 带排序的分页
Page<User> page = PageUtils.buildPage(pageNum, pageSize, "create_time", false);
IPage<User> result = userService.page(page);

// 从查询对象构建分页
UserQuery query = new UserQuery();
query.setPageNum(1);
query.setPageSize(10);
query.setOrderBy("create_time");
query.setIsAsc(false);
Page<User> page = PageUtils.buildPage(query);
```

## 高级功能

### 1. 多租户支持

```java
@Component
public class TenantLineHandler implements TenantLineHandler {
    
    @Override
    public Expression getTenantId() {
        // 从上下文获取租户ID
        Long tenantId = TenantContextHolder.getTenantId();
        return new LongValue(tenantId);
    }
    
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }
    
    @Override
    public boolean ignoreTable(String tableName) {
        // 忽略系统表
        return "sys_config".equals(tableName) || "sys_dict".equals(tableName);
    }
}
```

### 2. 数据权限

```java
@Component
public class DataScopeInterceptor implements InnerInterceptor {
    
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        // 根据用户权限动态添加数据过滤条件
        String sql = boundSql.getSql();
        String dataScopeSql = buildDataScopeSql();
        // 修改SQL添加数据权限条件
    }
    
    private String buildDataScopeSql() {
        // 构建数据权限SQL
        return "AND dept_id IN (SELECT id FROM sys_dept WHERE ...)";
    }
}
```

### 3. SQL性能监控

```java
@Configuration
public class MybatisPlusConfig {
    
    @Bean
    @Profile({"dev", "test"})
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        // SQL性能监控
        interceptor.addInnerInterceptor(new IllegalSQLInnerInterceptor());
        
        return interceptor;
    }
}
```

### 4. 动态表名

```java
@Component
public class DynamicTableNameHandler implements TableNameHandler {
    
    @Override
    public String dynamicTableName(String sql, String tableName) {
        // 根据业务逻辑动态确定表名
        if ("sys_log".equals(tableName)) {
            String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            return tableName + "_" + month;
        }
        return tableName;
    }
}
```

## 最佳实践

### 1. 实体类设计
- 继承BaseEntity获得通用字段
- 使用@TableName指定表名
- 合理使用@TableField配置字段映射
- 使用@TableLogic实现逻辑删除

### 2. Mapper设计
- 继承BaseMapper获得基础CRUD方法
- 复杂查询使用XML配置
- 合理使用条件构造器

### 3. 性能优化
- 合理使用分页查询
- 避免N+1查询问题
- 使用批量操作提高效率
- 开启二级缓存

### 4. 安全考虑
- 使用参数化查询防止SQL注入
- 实现数据权限控制
- 敏感操作记录审计日志

## 注意事项

1. **逻辑删除**: 使用逻辑删除时注意唯一索引问题
2. **乐观锁**: 更新时需要传入version字段
3. **分页查询**: 大数据量时注意性能问题
4. **事务管理**: 合理使用@Transactional注解
5. **连接池**: 根据业务量合理配置连接池参数

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x
- MyBatis-Plus版本: 3.5.x
- MySQL版本: 8.x