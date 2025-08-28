package com.rui.common.security.config;

import com.rui.common.security.filter.AccessControlFilter;
import com.rui.common.security.filter.SecurityHeaderFilter;
import com.rui.common.security.service.JwtBlacklistService;
import com.rui.common.security.service.JwtKeyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 安全模块自动配置类
 * 整合JWT密钥管理、黑名单服务、安全过滤器等组件
 *
 * @author rui
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({
    JwtSecurityConfig.class,
    SecurityHeaderConfig.class,
    AccessControlConfig.class
})
public class SecurityAutoConfiguration {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * JWT密钥管理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.security.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JwtKeyManager jwtKeyManager(JwtSecurityConfig jwtSecurityConfig) {
        return new JwtKeyManager(jwtSecurityConfig, stringRedisTemplate);
    }

    /**
     * JWT黑名单服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.security.jwt.blacklist", name = "enabled", havingValue = "true")
    public JwtBlacklistService jwtBlacklistService(JwtSecurityConfig jwtSecurityConfig) {
        return new JwtBlacklistService(jwtSecurityConfig, stringRedisTemplate);
    }

    /**
     * 安全头过滤器注册
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.security.headers", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<SecurityHeaderFilter> securityHeaderFilterRegistration(
            SecurityHeaderConfig securityHeaderConfig) {
        FilterRegistrationBean<SecurityHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeaderFilter(securityHeaderConfig));
        registration.addUrlPatterns("/*");
        registration.setName("securityHeaderFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    /**
     * 访问控制过滤器注册
     */
    @Bean
    @ConditionalOnProperty(prefix = "rui.security.access-control", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<AccessControlFilter> accessControlFilterRegistration(
            AccessControlConfig accessControlConfig,
            RedisTemplate<String, Object> redisTemplate) {
        FilterRegistrationBean<AccessControlFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AccessControlFilter(accessControlConfig, redisTemplate));
        registration.addUrlPatterns("/*");
        registration.setName("accessControlFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }

    /**
     * 安全配置验证器
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityConfigValidator securityConfigValidator() {
        return new SecurityConfigValidator();
    }

    /**
     * 安全事件监听器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.security.audit", name = "enabled", havingValue = "true")
    public SecurityEventListener securityEventListener() {
        return new SecurityEventListener();
    }
}