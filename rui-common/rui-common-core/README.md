# RUI Common Core

## 模块简介

RUI框架的核心工具模块，提供基础的工具类、常量定义、异常处理、分页查询、统一响应等核心功能。

## 主要功能

### 1. 常量定义
- **Constants**: 系统通用常量
- **HttpStatus**: HTTP状态码常量
- **SecurityConstants**: 安全相关常量

### 2. 安全上下文
- **SecurityContext**: 安全上下文信息
- **SecurityContextHolder**: 安全上下文持有者

### 3. 领域对象
- **PageResult**: 分页结果封装
- **R**: 统一响应结果封装

### 4. 枚举定义
- **ErrorCode**: 错误码枚举

### 5. 异常处理
- **ServiceException**: 业务异常类

### 6. 分页查询
- **PageQuery**: 分页查询参数
- **TableDataInfo**: 表格数据信息

### 7. 工具类
- **IpUtils**: IP地址工具类
- **ServletUtils**: Servlet工具类
- **SpringUtils**: Spring工具类
- **StringUtils**: 字符串工具类

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 统一响应示例

```java
@RestController
public class UserController {
    
    @GetMapping("/user/{id}")
    public R<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        return R.ok(user);
    }
    
    @PostMapping("/user")
    public R<Void> createUser(@RequestBody User user) {
        userService.save(user);
        return R.ok();
    }
}
```

### 3. 分页查询示例

```java
@GetMapping("/users")
public R<PageResult<User>> getUsers(PageQuery pageQuery) {
    PageResult<User> result = userService.getUsers(pageQuery);
    return R.ok(result);
}
```

### 4. 异常处理示例

```java
@Service
public class UserService {
    
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
```

### 5. 工具类使用示例

```java
// IP工具类
String clientIp = IpUtils.getIpAddr(request);

// Servlet工具类
HttpServletRequest request = ServletUtils.getRequest();
HttpServletResponse response = ServletUtils.getResponse();

// Spring工具类
UserService userService = SpringUtils.getBean(UserService.class);

// 字符串工具类
boolean isEmpty = StringUtils.isEmpty(str);
boolean isNotEmpty = StringUtils.isNotEmpty(str);
```

## 主要依赖

- Spring Boot Starter
- Spring Boot Web
- Spring Boot Validation
- Jakarta Servlet API
- Jakarta Annotation API
- Jakarta Validation API
- Jackson (JSON处理)
- Apache Commons Lang3
- Apache Commons Codec
- Apache Commons IO
- Hutool
- Google Guava

## 注意事项

1. **统一响应**: 建议所有API接口都使用R类进行统一响应封装
2. **异常处理**: 业务异常统一使用ServiceException，配合全局异常处理器使用
3. **分页查询**: 使用PageQuery和PageResult进行标准化分页处理
4. **安全上下文**: 在需要获取当前用户信息时使用SecurityContextHolder
5. **工具类**: 优先使用本模块提供的工具类，保持代码风格一致性

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x