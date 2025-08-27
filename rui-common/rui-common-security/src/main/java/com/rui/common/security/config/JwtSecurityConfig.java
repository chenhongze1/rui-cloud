package com.rui.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT安全配置
 *
 * @author rui
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt.security")
public class JwtSecurityConfig {

    /**
     * 令牌自定义标识
     */
    private String header = "Authorization";

    /**
     * 令牌前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * 令牌秘钥（生产环境应从配置中心获取）
     */
    private String secret;

    /**
     * 令牌有效期（默认30分钟）
     */
    private Duration expireTime = Duration.ofMinutes(30);

    /**
     * 刷新令牌有效期（默认7天）
     */
    private Duration refreshExpireTime = Duration.ofDays(7);

    /**
     * 是否启用密钥轮换
     */
    private boolean enableKeyRotation = false;

    /**
     * 密钥轮换间隔（默认24小时）
     */
    private Duration keyRotationInterval = Duration.ofHours(24);

    /**
     * 密钥最小长度
     */
    private int minKeyLength = 32;

    /**
     * 是否启用令牌黑名单
     */
    private boolean enableBlacklist = true;

    /**
     * 黑名单缓存前缀
     */
    private String blacklistPrefix = "jwt:blacklist:";

    /**
     * 是否启用单点登录（同一用户只能有一个有效令牌）
     */
    private boolean enableSingleSignOn = false;

    /**
     * 用户令牌缓存前缀
     */
    private String userTokenPrefix = "jwt:user:token:";

    /**
     * 是否记录JWT操作日志
     */
    private boolean enableAuditLog = true;

    /**
     * 审计日志前缀
     */
    private String auditLogPrefix = "jwt:audit:";

    /**
     * 黑名单配置
     */
    private BlacklistConfig blacklist = new BlacklistConfig();

    /**
     * 密钥轮换配置
     */
    private KeyRotationConfig keyRotation = new KeyRotationConfig();

    /**
     * 获取JWT过期时间
     */
    public Duration getExpiration() {
        return expireTime;
    }

    /**
     * 获取刷新令牌过期时间
     */
    public Duration getRefreshExpiration() {
        return refreshExpireTime;
    }

    /**
     * 黑名单配置类
     */
    @Data
    public static class BlacklistConfig {
        /**
         * 是否启用黑名单
         */
        private boolean enabled = true;

        /**
         * 黑名单清理间隔（默认1小时）
         */
        private Duration cleanupInterval = Duration.ofHours(1);

        /**
         * 黑名单缓存前缀
         */
        private String cachePrefix = "jwt:blacklist:";
    }

    /**
     * 密钥轮换配置类
     */
    @Data
    public static class KeyRotationConfig {
        /**
         * 是否启用密钥轮换
         */
        private boolean enabled = false;

        /**
         * 密钥轮换间隔（默认24小时）
         */
        private Duration interval = Duration.ofHours(24);

        /**
         * 密钥宽限期（默认1小时）
         */
        private Duration gracePeriod = Duration.ofHours(1);
    }
}