# RUI Cloud - SpringCloud微服务架构基础框架

## 项目简介

RUI Cloud是一个基于SpringCloud的微服务架构基础框架，提供了完整的公共模块和基础设施，帮助快速构建企业级微服务应用。

## 技术栈

- **JDK**: 21
- **Spring Boot**: 3.2.1
- **Spring Cloud**: 2023.0.0
- **Spring Cloud Alibaba**: 2022.0.0.0
- **MyBatis Plus**: 3.5.5
- **MySQL**: 8.x
- **Redis**: 7.x
- **Nacos**: 3.0

## 项目结构

```
rui-cloud/
├── pom.xml                    # 父项目POM，统一版本管理
└── rui-common/                # 公共模块
    ├── rui-common-core/       # 核心工具模块
    ├── rui-common-web/        # Web相关公共模块
    ├── rui-common-security/   # 安全相关公共模块
    ├── rui-common-redis/      # Redis相关公共模块
    └── rui-common-mybatis/    # MyBatis相关公共模块
```

## 核心功能

### 🔧 rui-common-core (核心工具模块)
- 统一响应结果封装 (R)
- 分页查询基础类 (PageQuery, TableDataInfo)
- 系统常量定义 (Constants, HttpStatus)
- 业务异常处理 (ServiceException)

### 🌐 rui-common-web (Web公共模块)
- 全局异常处理器 (GlobalExceptionHandler)
- 基础控制器 (BaseController)
- 统一响应格式

### 🔐 rui-common-security (安全模块)
- JWT工具类 (JwtUtils)
- 登录用户信息 (LoginUser)
- 用户、部门、角色实体类
- Spring Security集成

### 📦 rui-common-redis (Redis模块)
- Redis服务封装 (RedisService)
- 缓存配置 (RedisConfig)
- FastJson2序列化器
- Redisson分布式锁支持

### 🗄️ rui-common-mybatis (数据访问模块)
- MyBatis Plus配置
- 基础实体类 (BaseEntity)
- 自动填充处理器 (MyMetaObjectHandler)
- 分页工具类 (PageUtils)
- 乐观锁、防全表操作等插件

## 快速开始

### 1. 环境要求
- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 7.0+

### 2. 克隆项目
```bash
git clone https://github.com/your-username/rui-cloud.git
cd rui-cloud
```

### 3. 编译项目
```bash
mvn clean compile
```

### 4. 安装到本地仓库
```bash
mvn clean install
```

## 使用指南

### 依赖引入
在你的微服务项目中引入需要的公共模块：

```xml
<!-- 核心模块 -->
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Web模块 -->
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-web</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 安全模块 -->
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-security</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Redis模块 -->
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-redis</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- MyBatis模块 -->
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-mybatis</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 统一响应格式
```java
@RestController
public class UserController extends BaseController {
    
    @GetMapping("/users")
    public R<List<User>> getUsers() {
        List<User> users = userService.list();
        return R.ok(users);
    }
    
    @PostMapping("/users")
    public R<Void> createUser(@RequestBody User user) {
        userService.save(user);
        return R.ok();
    }
}
```

### 分页查询
```java
@GetMapping("/users/page")
public TableDataInfo<User> getUserPage(PageQuery pageQuery) {
    startPage();
    List<User> list = userService.selectUserList();
    return getDataTable(list);
}
```

## 开发规范

### 代码规范
- 使用Lombok简化代码
- 统一异常处理
- RESTful API设计
- 统一响应格式

### 安全规范
- JWT令牌认证
- 接口权限控制
- 数据权限过滤
- 密码加密存储

### 数据库规范
- 统一字段命名
- 自动填充创建/更新信息
- 乐观锁版本控制
- 逻辑删除

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

- 作者: RUI
- 邮箱: your-email@example.com
- 项目地址: https://github.com/your-username/rui-cloud

## 更新日志

### v1.0.0 (2024-01-XX)
- 初始版本发布
- 完成核心公共模块开发
- 集成Spring Boot 3.x + Spring Cloud 2023.x
- 支持JWT认证、Redis缓存、MyBatis Plus等