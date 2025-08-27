# RUI Common Security

## 模块简介

RUI框架的安全模块，基于Spring Security提供JWT认证、访问控制、安全配置、用户管理等功能，为应用提供完整的安全解决方案。

## 主要功能

### 1. 安全配置
- **SecurityAutoConfiguration**: 安全自动配置类
- **JwtSecurityConfig**: JWT安全配置
- **AccessControlConfig**: 访问控制配置
- **SecurityConfigValidator**: 安全配置验证器
- **SecurityEventListener**: 安全事件监听器
- **SecurityHeaderConfig**: 安全头配置

### 2. 领域对象
- **LoginUser**: 登录用户信息
- **SysUser**: 系统用户实体
- **SysRole**: 系统角色实体
- **SysDept**: 系统部门实体

### 3. 过滤器
- **AccessControlFilter**: 访问控制过滤器
- **SecurityHeaderFilter**: 安全头过滤器

### 4. 服务
- **JwtBlacklistService**: JWT黑名单服务
- **JwtKeyManager**: JWT密钥管理器

### 5. 工具类
- **JwtUtils**: JWT工具类

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-security</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

### 2. 用户认证

```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            // 认证用户
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(), 
                    request.getPassword()
                )
            );
            
            // 获取用户信息
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            
            // 生成JWT令牌
            String token = jwtUtils.generateToken(loginUser);
            
            // 返回登录结果
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUser(loginUser.getSysUser());
            response.setExpireTime(jwtUtils.getExpirationDateFromToken(token));
            
            return R.ok(response);
            
        } catch (BadCredentialsException e) {
            return R.fail("用户名或密码错误");
        } catch (DisabledException e) {
            return R.fail("账户已被禁用");
        } catch (AccountExpiredException e) {
            return R.fail("账户已过期");
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String token = jwtUtils.getTokenFromRequest(request);
        if (StringUtils.hasText(token)) {
            // 将令牌加入黑名单
            jwtBlacklistService.addToBlacklist(token);
        }
        return R.ok();
    }
    
    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public R<String> refreshToken(HttpServletRequest request) {
        String token = jwtUtils.getTokenFromRequest(request);
        if (StringUtils.hasText(token) && jwtUtils.canTokenBeRefreshed(token)) {
            String refreshedToken = jwtUtils.refreshToken(token);
            return R.ok(refreshedToken);
        }
        return R.fail("令牌无法刷新");
    }
}
```

### 3. 权限控制

```java
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取用户列表 - 需要USER_QUERY权限
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public R<List<SysUser>> getUsers() {
        List<SysUser> users = userService.list();
        return R.ok(users);
    }
    
    /**
     * 创建用户 - 需要USER_CREATE权限
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public R<Void> createUser(@RequestBody SysUser user) {
        userService.save(user);
        return R.ok();
    }
    
    /**
     * 更新用户 - 需要USER_UPDATE权限或者是用户本人
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or #id == authentication.principal.userId")
    public R<Void> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userService.updateById(user);
        return R.ok();
    }
    
    /**
     * 删除用户 - 需要USER_DELETE权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return R.ok();
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public R<SysUser> getCurrentUser(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return R.ok(loginUser.getSysUser());
    }
}
```

### 4. 自定义用户详情服务

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private SysUserService userService;
    
    @Autowired
    private SysRoleService roleService;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户信息
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new DisabledException("用户已被禁用: " + username);
        }
        
        // 查询用户角色和权限
        List<SysRole> roles = roleService.getRolesByUserId(user.getId());
        Set<String> authorities = new HashSet<>();
        
        for (SysRole role : roles) {
            // 添加角色
            authorities.add("ROLE_" + role.getRoleKey());
            
            // 添加权限
            List<String> permissions = roleService.getPermissionsByRoleId(role.getId());
            authorities.addAll(permissions);
        }
        
        // 创建登录用户对象
        return new LoginUser(user, authorities);
    }
}
```

### 5. JWT工具类使用

```java
@Component
public class JwtService {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    /**
     * 生成访问令牌
     */
    public String generateAccessToken(LoginUser loginUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", loginUser.getUserId());
        claims.put("username", loginUser.getUsername());
        claims.put("authorities", loginUser.getAuthorities());
        
        return jwtUtils.generateToken(claims, loginUser.getUsername());
    }
    
    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(LoginUser loginUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", loginUser.getUserId());
        claims.put("type", "refresh");
        
        return jwtUtils.generateRefreshToken(claims, loginUser.getUsername());
    }
    
    /**
     * 验证令牌
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = jwtUtils.getUsernameFromToken(token);
            return username.equals(userDetails.getUsername()) && 
                   !jwtUtils.isTokenExpired(token) &&
                   !jwtBlacklistService.isBlacklisted(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从令牌获取用户信息
     */
    public LoginUser getUserFromToken(String token) {
        Claims claims = jwtUtils.getClaimsFromToken(token);
        
        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();
        
        @SuppressWarnings("unchecked")
        List<String> authoritiesList = claims.get("authorities", List.class);
        
        Set<SimpleGrantedAuthority> authorities = authoritiesList.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());
        
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(username);
        
        return new LoginUser(user, authorities);
    }
}
```

### 6. 安全配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;
    
    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    
    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * 认证提供者
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
    
    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF
            .csrf(csrf -> csrf.disable())
            
            // 会话管理
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 异常处理
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler))
            
            // 请求授权
            .authorizeHttpRequests(authz -> authz
                // 公开接口
                .requestMatchers("/auth/**", "/public/**").permitAll()
                // 静态资源
                .requestMatchers("/static/**", "/favicon.ico").permitAll()
                // API文档
                .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**").permitAll()
                // 健康检查
                .requestMatchers("/actuator/health").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated())
            
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationTokenFilter, 
                UsernamePasswordAuthenticationFilter.class)
            
            // 添加安全头过滤器
            .addFilterAfter(new SecurityHeaderFilter(), 
                JwtAuthenticationTokenFilter.class);
        
        return http.build();
    }
    
    /**
     * Web安全配置
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
            .requestMatchers("/error", "/favicon.ico")
            .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }
}
```

## 配置属性

```yaml
# 安全配置
rui:
  security:
    # JWT配置
    jwt:
      # 密钥
      secret: "rui-framework-jwt-secret-key-2024"
      # 访问令牌过期时间（秒）
      access-token-expire: 7200
      # 刷新令牌过期时间（秒）
      refresh-token-expire: 604800
      # 令牌前缀
      token-prefix: "Bearer "
      # 请求头名称
      header-name: "Authorization"
      # 是否启用刷新令牌
      enable-refresh: true
      # 密钥轮换间隔（小时）
      key-rotation-interval: 24
    
    # 访问控制配置
    access-control:
      # 是否启用访问控制
      enabled: true
      # 默认拒绝访问
      default-deny: false
      # 白名单路径
      whitelist-paths:
        - "/auth/**"
        - "/public/**"
        - "/actuator/health"
      # 黑名单IP
      blacklist-ips: []
      # 白名单IP
      whitelist-ips: []
    
    # 安全头配置
    headers:
      # 是否启用安全头
      enabled: true
      # 内容安全策略
      content-security-policy: "default-src 'self'"
      # X-Frame-Options
      frame-options: "DENY"
      # X-Content-Type-Options
      content-type-options: "nosniff"
      # X-XSS-Protection
      xss-protection: "1; mode=block"
      # Strict-Transport-Security
      hsts: "max-age=31536000; includeSubDomains"
    
    # 密码策略
    password:
      # 最小长度
      min-length: 8
      # 最大长度
      max-length: 32
      # 是否需要数字
      require-digit: true
      # 是否需要小写字母
      require-lowercase: true
      # 是否需要大写字母
      require-uppercase: true
      # 是否需要特殊字符
      require-special-char: true
      # 密码历史记录数量
      history-count: 5
      # 密码过期天数
      expire-days: 90
    
    # 登录策略
    login:
      # 最大失败次数
      max-failure-count: 5
      # 锁定时间（分钟）
      lockout-duration: 30
      # 是否启用验证码
      enable-captcha: true
      # 验证码触发失败次数
      captcha-threshold: 3
      # 会话超时时间（分钟）
      session-timeout: 30
    
    # 审计配置
    audit:
      # 是否启用审计
      enabled: true
      # 审计事件类型
      event-types:
        - "LOGIN"
        - "LOGOUT"
        - "ACCESS_DENIED"
        - "AUTHENTICATION_FAILURE"
      # 审计日志保留天数
      retention-days: 90

# Spring Security配置
spring:
  security:
    # 用户配置（开发环境）
    user:
      name: admin
      password: admin123
      roles: ADMIN
```

## 安全特性

### 1. JWT令牌管理
- 访问令牌和刷新令牌分离
- 令牌黑名单机制
- 密钥轮换支持
- 令牌自动刷新

### 2. 访问控制
- 基于角色的访问控制（RBAC）
- 方法级权限控制
- IP白名单/黑名单
- 路径级访问控制

### 3. 安全防护
- CSRF防护
- XSS防护
- 点击劫持防护
- 安全头设置

### 4. 密码安全
- 密码强度验证
- 密码加密存储
- 密码历史记录
- 密码过期策略

### 5. 登录安全
- 登录失败锁定
- 验证码保护
- 会话管理
- 异地登录检测

## 高级功能

### 1. 多因素认证

```java
@Service
public class MfaService {
    
    /**
     * 生成TOTP密钥
     */
    public String generateTotpSecret() {
        return Base32.random();
    }
    
    /**
     * 验证TOTP代码
     */
    public boolean verifyTotpCode(String secret, String code) {
        TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
        return totp.verify(secret, code);
    }
    
    /**
     * 发送短信验证码
     */
    public void sendSmsCode(String phone) {
        String code = RandomStringUtils.randomNumeric(6);
        // 发送短信逻辑
        redisService.set("sms:" + phone, code, 300); // 5分钟过期
    }
    
    /**
     * 验证短信验证码
     */
    public boolean verifySmsCode(String phone, String code) {
        String cachedCode = redisService.get("sms:" + phone, String.class);
        return code.equals(cachedCode);
    }
}
```

### 2. 单点登录（SSO）

```java
@Service
public class SsoService {
    
    /**
     * 生成SSO票据
     */
    public String generateSsoTicket(LoginUser loginUser) {
        String ticket = UUID.randomUUID().toString();
        redisService.set("sso:ticket:" + ticket, loginUser, 300); // 5分钟过期
        return ticket;
    }
    
    /**
     * 验证SSO票据
     */
    public LoginUser validateSsoTicket(String ticket) {
        LoginUser loginUser = redisService.get("sso:ticket:" + ticket, LoginUser.class);
        if (loginUser != null) {
            redisService.delete("sso:ticket:" + ticket); // 一次性使用
        }
        return loginUser;
    }
    
    /**
     * 全局登出
     */
    public void globalLogout(String userId) {
        // 清除用户所有会话
        Set<String> sessions = redisService.sMembers("user:sessions:" + userId, String.class);
        for (String sessionId : sessions) {
            redisService.delete("session:" + sessionId);
        }
        redisService.delete("user:sessions:" + userId);
    }
}
```

### 3. 权限缓存

```java
@Service
public class PermissionCacheService {
    
    @Cacheable(value = "user:permissions", key = "#userId")
    public Set<String> getUserPermissions(Long userId) {
        return roleService.getPermissionsByUserId(userId);
    }
    
    @CacheEvict(value = "user:permissions", key = "#userId")
    public void evictUserPermissions(Long userId) {
        // 清除用户权限缓存
    }
    
    @CacheEvict(value = "user:permissions", allEntries = true)
    public void evictAllPermissions() {
        // 清除所有权限缓存
    }
}
```

## 安全最佳实践

### 1. 令牌安全
- 使用强随机密钥
- 定期轮换密钥
- 设置合理的过期时间
- 实现令牌黑名单

### 2. 密码安全
- 使用强密码策略
- 密码加盐哈希存储
- 防止密码重用
- 定期强制更换密码

### 3. 会话安全
- 使用安全的会话ID
- 设置会话超时
- 防止会话固定攻击
- 安全的会话销毁

### 4. 传输安全
- 强制使用HTTPS
- 设置安全头
- 防止中间人攻击
- 证书验证

## 监控和审计

### 1. 安全事件监控
- 登录失败监控
- 异常访问监控
- 权限变更监控
- 系统入侵检测

### 2. 审计日志
- 用户操作审计
- 权限变更审计
- 系统配置审计
- 数据访问审计

### 3. 安全指标
- 认证成功率
- 权限拒绝率
- 安全事件频率
- 系统安全评分

## 注意事项

1. **密钥管理**: 妥善保管JWT密钥，定期轮换
2. **权限设计**: 遵循最小权限原则
3. **会话管理**: 合理设置会话超时时间
4. **安全更新**: 及时更新安全组件版本
5. **日志安全**: 避免在日志中记录敏感信息
6. **错误处理**: 避免泄露系统内部信息

## 版本信息

- 当前版本: 1.0.0
- JDK版本: 21+
- Spring Boot版本: 3.x
- Spring Security版本: 6.x
- JWT版本: 0.12.x