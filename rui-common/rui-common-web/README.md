# RUI Common Web

## 模块简介

RUI框架的Web通用模块，提供基础控制器、全局异常处理、安全异常处理等功能，简化Web应用开发中的通用操作。

## 主要功能

### 1. 基础控制器
- **BaseController**: 提供通用的控制器基础功能

### 2. 异常处理
- **GlobalExceptionHandler**: 全局异常处理器
- **SecureExceptionHandler**: 安全异常处理器

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-web</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Spring Boot Web Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- 参数验证 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 2. 继承基础控制器

```java
@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 分页查询用户
     */
    @GetMapping
    public R<TableDataInfo<User>> getUsers(UserQuery query) {
        // 使用基础控制器的分页方法
        startPage();
        List<User> users = userService.selectUserList(query);
        return success(getDataTable(users));
    }
    
    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public R<User> getUser(@PathVariable Long id) {
        User user = userService.selectUserById(id);
        return success(user);
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    public R<Void> createUser(@Valid @RequestBody User user) {
        userService.insertUser(user);
        return success();
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public R<Void> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        user.setId(id);
        userService.updateUser(user);
        return success();
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{ids}")
    public R<Void> deleteUsers(@PathVariable Long[] ids) {
        userService.deleteUserByIds(ids);
        return success();
    }
    
    /**
     * 导出用户数据
     */
    @PostMapping("/export")
    public void exportUsers(HttpServletResponse response, UserQuery query) {
        List<User> users = userService.selectUserList(query);
        ExcelUtil<User> util = new ExcelUtil<>(User.class);
        util.exportExcel(response, users, "用户数据");
    }
    
    /**
     * 批量操作示例
     */
    @PostMapping("/batch")
    public R<Void> batchOperation(@RequestBody BatchRequest request) {
        // 获取当前登录用户
        LoginUser loginUser = getLoginUser();
        
        // 记录操作日志
        logOperation("批量操作", request.toString());
        
        // 执行批量操作
        userService.batchOperation(request.getIds(), request.getOperation());
        
        return success();
    }
}
```

### 3. 自定义异常处理

```java
@RestControllerAdvice
public class CustomExceptionHandler extends GlobalExceptionHandler {
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return R.fail(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理数据访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    public R<Void> handleDataAccessException(DataAccessException e) {
        log.error("数据访问异常: {}", e.getMessage(), e);
        return R.fail("数据操作失败");
    }
    
    /**
     * 处理文件上传异常
     */
    @ExceptionHandler(MultipartException.class)
    public R<Void> handleMultipartException(MultipartException e) {
        log.error("文件上传异常: {}", e.getMessage(), e);
        return R.fail("文件上传失败: " + e.getMessage());
    }
    
    /**
     * 处理外部服务调用异常
     */
    @ExceptionHandler(FeignException.class)
    public R<Void> handleFeignException(FeignException e) {
        log.error("外部服务调用异常: {}", e.getMessage(), e);
        return R.fail("外部服务调用失败");
    }
}
```

### 4. 文件上传控制器

```java
@RestController
@RequestMapping("/api/upload")
public class UploadController extends BaseController {
    
    @Autowired
    private FileService fileService;
    
    /**
     * 单文件上传
     */
    @PostMapping("/single")
    public R<FileInfo> uploadSingle(@RequestParam("file") MultipartFile file) {
        // 验证文件
        validateFile(file);
        
        // 上传文件
        FileInfo fileInfo = fileService.uploadFile(file);
        
        return success(fileInfo);
    }
    
    /**
     * 多文件上传
     */
    @PostMapping("/multiple")
    public R<List<FileInfo>> uploadMultiple(@RequestParam("files") MultipartFile[] files) {
        List<FileInfo> fileInfos = new ArrayList<>();
        
        for (MultipartFile file : files) {
            validateFile(file);
            FileInfo fileInfo = fileService.uploadFile(file);
            fileInfos.add(fileInfo);
        }
        
        return success(fileInfos);
    }
    
    /**
     * 图片上传（带压缩）
     */
    @PostMapping("/image")
    public R<FileInfo> uploadImage(@RequestParam("file") MultipartFile file) {
        // 验证图片文件
        validateImageFile(file);
        
        // 上传并压缩图片
        FileInfo fileInfo = fileService.uploadImage(file, true);
        
        return success(fileInfo);
    }
    
    /**
     * 文件下载
     */
    @GetMapping("/download/{fileId}")
    public void downloadFile(@PathVariable String fileId, HttpServletResponse response) {
        FileInfo fileInfo = fileService.getFileInfo(fileId);
        if (fileInfo == null) {
            throw new ServiceException("文件不存在");
        }
        
        // 设置下载响应头
        setDownloadResponse(response, fileInfo.getOriginalName());
        
        // 下载文件
        fileService.downloadFile(fileId, response.getOutputStream());
    }
    
    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ServiceException("文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new ServiceException("文件大小不能超过10MB");
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (!isAllowedContentType(contentType)) {
            throw new ServiceException("不支持的文件类型: " + contentType);
        }
    }
    
    /**
     * 验证图片文件
     */
    private void validateImageFile(MultipartFile file) {
        validateFile(file);
        
        String contentType = file.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new ServiceException("只支持图片文件");
        }
    }
}
```

### 5. API版本控制

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller extends BaseController {
    
    @GetMapping
    public R<List<UserV1DTO>> getUsers() {
        // V1版本的用户查询
        return success(userService.getUsersV1());
    }
}

@RestController
@RequestMapping("/api/v2/users")
public class UserV2Controller extends BaseController {
    
    @GetMapping
    public R<PageResult<UserV2DTO>> getUsers(PageQuery query) {
        // V2版本的用户查询（支持分页）
        return success(userService.getUsersV2(query));
    }
}
```

### 6. 跨域配置

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的域名
        config.addAllowedOriginPattern("*");
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 允许的请求方法
        config.addAllowedMethod("*");
        
        // 允许携带凭证
        config.setAllowCredentials(true);
        
        // 预检请求的缓存时间
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
```

## 配置属性

```yaml
# Web配置
spring:
  servlet:
    multipart:
      # 单个文件最大大小
      max-file-size: 10MB
      # 总文件最大大小
      max-request-size: 100MB
      # 文件写入磁盘的阈值
      file-size-threshold: 2KB
      # 临时文件位置
      location: /tmp
  
  # Jackson配置
  jackson:
    # 日期格式
    date-format: yyyy-MM-dd HH:mm:ss
    # 时区
    time-zone: GMT+8
    # 序列化配置
    serialization:
      # 格式化输出
      indent-output: false
      # 忽略空值
      write-null-map-values: false
    # 反序列化配置
    deserialization:
      # 忽略未知属性
      fail-on-unknown-properties: false
  
  # Web MVC配置
  mvc:
    # 路径匹配策略
    pathmatch:
      matching-strategy: ant_path_matcher
    # 静态资源配置
    static-path-pattern: /static/**
    # 视图配置
    view:
      prefix: /WEB-INF/views/
      suffix: .jsp

# RUI Web扩展配置
rui:
  web:
    # 全局异常处理配置
    exception:
      # 是否启用全局异常处理
      enabled: true
      # 是否打印异常堆栈
      print-stack-trace: true
      # 是否返回详细错误信息（生产环境建议关闭）
      include-error-details: false
      # 异常日志级别
      log-level: ERROR
    
    # 跨域配置
    cors:
      # 是否启用跨域
      enabled: true
      # 允许的域名
      allowed-origins: "*"
      # 允许的请求头
      allowed-headers: "*"
      # 允许的请求方法
      allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
      # 是否允许携带凭证
      allow-credentials: true
      # 预检请求缓存时间
      max-age: 3600
    
    # 文件上传配置
    upload:
      # 上传路径
      path: /uploads
      # 允许的文件类型
      allowed-types:
        - image/jpeg
        - image/png
        - image/gif
        - application/pdf
        - application/msword
        - application/vnd.openxmlformats-officedocument.wordprocessingml.document
      # 单文件最大大小（字节）
      max-file-size: 10485760  # 10MB
      # 总文件最大大小（字节）
      max-total-size: 104857600  # 100MB
    
    # 分页配置
    page:
      # 默认页码
      default-page-num: 1
      # 默认页大小
      default-page-size: 10
      # 最大页大小
      max-page-size: 500
      # 是否启用合理化
      reasonable: true
    
    # 响应配置
    response:
      # 统一响应格式
      unified-format: true
      # 成功状态码
      success-code: 200
      # 失败状态码
      error-code: 500
      # 默认成功消息
      success-message: "操作成功"
      # 默认失败消息
      error-message: "操作失败"
```

## BaseController功能

### 1. 分页功能
```java
public abstract class BaseController {
    
    /**
     * 设置请求分页数据
     */
    protected void startPage() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
        Boolean reasonable = pageDomain.getReasonable();
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(reasonable);
    }
    
    /**
     * 响应请求分页数据
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected TableDataInfo getDataTable(List<?> list) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }
}
```

### 2. 响应封装
```java
/**
 * 返回成功消息
 */
protected R<Void> success() {
    return R.ok();
}

/**
 * 返回成功数据
 */
protected <T> R<T> success(T data) {
    return R.ok(data);
}

/**
 * 返回成功消息
 */
protected R<Void> success(String message) {
    return R.ok(message);
}

/**
 * 返回失败消息
 */
protected R<Void> error() {
    return R.fail();
}

/**
 * 返回失败消息
 */
protected R<Void> error(String message) {
    return R.fail(message);
}
```

### 3. 用户信息获取
```java
/**
 * 获取当前登录用户
 */
protected LoginUser getLoginUser() {
    return SecurityUtils.getLoginUser();
}

/**
 * 获取当前用户ID
 */
protected Long getUserId() {
    return getLoginUser().getUserId();
}

/**
 * 获取当前用户名
 */
protected String getUsername() {
    return getLoginUser().getUsername();
}
```

## 全局异常处理

### 1. 系统异常
- **Exception**: 系统异常
- **RuntimeException**: 运行时异常
- **ServiceException**: 业务异常
- **SecurityException**: 安全异常

### 2. Web异常
- **MethodArgumentNotValidException**: 参数验证异常
- **BindException**: 参数绑定异常
- **HttpRequestMethodNotSupportedException**: 请求方法不支持
- **HttpMediaTypeNotSupportedException**: 媒体类型不支持

### 3. 数据库异常
- **DataIntegrityViolationException**: 数据完整性异常
- **DuplicateKeyException**: 主键重复异常
- **DataAccessException**: 数据访问异常

### 4. 安全异常
- **AccessDeniedException**: 访问拒绝异常
- **AuthenticationException**: 认证异常
- **InsufficientAuthenticationException**: 认证不足异常

## 最佳实践

### 1. 控制器设计
- 继承BaseController获得通用功能
- 使用RESTful API设计风格
- 合理使用HTTP状态码
- 统一响应格式

### 2. 异常处理
- 使用全局异常处理器
- 区分业务异常和系统异常
- 记录详细的错误日志
- 返回用户友好的错误信息

### 3. 参数验证
- 使用Bean Validation注解
- 自定义验证器处理复杂验证
- 统一验证错误响应格式

### 4. 安全考虑
- 输入参数验证和过滤
- 防止SQL注入和XSS攻击
- 合理设置CORS策略
- 敏感信息脱敏处理

## 注意事项

1. **异常处理**: 避免在异常信息中暴露敏感信息
2. **参数验证**: 对所有外部输入进行验证
3. **响应格式**: 保持API响应格式的一致性
4. **日志记录**: 记录关键操作和异常信息
5. **性能考虑**: 避免在控制器中进行复杂的业务逻辑处理
6. **版本管理**: 合理设计API版本控制策略

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x
- Spring Web版本: 6.x