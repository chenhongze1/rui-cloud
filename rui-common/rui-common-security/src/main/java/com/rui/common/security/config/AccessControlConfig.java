package com.rui.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 访问控制配置
 *
 * @author rui
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.access-control")
public class AccessControlConfig {

    /**
     * 是否启用访问控制
     */
    private boolean enabled = true;

    /**
     * IP 访问控制配置
     */
    private IpControlConfig ipControl = new IpControlConfig();

    /**
     * 用户代理控制配置
     */
    private UserAgentControlConfig userAgentControl = new UserAgentControlConfig();

    /**
     * 地理位置控制配置
     */
    private GeoLocationControlConfig geoLocationControl = new GeoLocationControlConfig();

    /**
     * 请求频率控制配置
     */
    private RequestRateControlConfig requestRateControl = new RequestRateControlConfig();

    /**
     * 会话控制配置
     */
    private SessionControlConfig sessionControl = new SessionControlConfig();

    @Data
    public static class IpControlConfig {
        /**
         * 是否启用 IP 控制
         */
        private boolean enabled = true;

        /**
         * IP 白名单
         */
        private List<String> whitelist;

        /**
         * IP 黑名单
         */
        private List<String> blacklist;

        /**
         * 是否允许内网 IP
         */
        private boolean allowPrivateIp = true;

        /**
         * 是否允许本地 IP
         */
        private boolean allowLocalhost = true;

        /**
         * 白名单优先级（true: 白名单优先，false: 黑名单优先）
         */
        private boolean whitelistPriority = true;

        /**
         * 动态黑名单配置
         */
        private DynamicBlacklistConfig dynamicBlacklist = new DynamicBlacklistConfig();

        @Data
        public static class DynamicBlacklistConfig {
            /**
             * 是否启用动态黑名单
             */
            private boolean enabled = true;

            /**
             * 失败次数阈值
             */
            private int failureThreshold = 5;

            /**
             * 时间窗口
             */
            private Duration timeWindow = Duration.ofMinutes(10);

            /**
             * 封禁时长
             */
            private Duration banDuration = Duration.ofHours(1);

            /**
             * 最大封禁时长
             */
            private Duration maxBanDuration = Duration.ofDays(1);
        }
    }

    @Data
    public static class UserAgentControlConfig {
        /**
         * 是否启用用户代理控制
         */
        private boolean enabled = false;

        /**
         * 允许的用户代理模式
         */
        private List<String> allowedPatterns;

        /**
         * 禁止的用户代理模式
         */
        private List<String> blockedPatterns;

        /**
         * 是否阻止空用户代理
         */
        private boolean blockEmptyUserAgent = true;

        /**
         * 是否阻止可疑的爬虫
         */
        private boolean blockSuspiciousBots = true;
    }

    @Data
    public static class GeoLocationControlConfig {
        /**
         * 是否启用地理位置控制
         */
        private boolean enabled = false;

        /**
         * 允许的国家代码
         */
        private List<String> allowedCountries;

        /**
         * 禁止的国家代码
         */
        private List<String> blockedCountries;

        /**
         * 是否允许未知地理位置
         */
        private boolean allowUnknownLocation = true;
    }

    @Data
    public static class RequestRateControlConfig {
        /**
         * 是否启用请求频率控制
         */
        private boolean enabled = true;

        /**
         * 全局频率限制
         */
        private RateLimitRule globalRule = new RateLimitRule();

        /**
         * 按 IP 的频率限制
         */
        private RateLimitRule ipRule = new RateLimitRule();

        /**
         * 按用户的频率限制
         */
        private RateLimitRule userRule = new RateLimitRule();

        /**
         * 按路径的频率限制
         */
        private Map<String, RateLimitRule> pathRules;

        @Data
        public static class RateLimitRule {
            /**
             * 是否启用
             */
            private boolean enabled = true;

            /**
             * 请求次数限制
             */
            private int requests = 100;

            /**
             * 时间窗口
             */
            private Duration window = Duration.ofMinutes(1);

            /**
             * 超限后的封禁时长
             */
            private Duration banDuration = Duration.ofMinutes(5);
        }
    }

    @Data
    public static class SessionControlConfig {
        /**
         * 是否启用会话控制
         */
        private boolean enabled = true;

        /**
         * 最大会话数（每个用户）
         */
        private int maxSessionsPerUser = 5;

        /**
         * 会话超时时间
         */
        private Duration sessionTimeout = Duration.ofHours(2);

        /**
         * 是否启用会话固定保护
         */
        private boolean sessionFixationProtection = true;

        /**
         * 是否启用并发会话控制
         */
        private boolean concurrentSessionControl = true;

        /**
         * 会话无效时的处理策略
         */
        private SessionInvalidationStrategy invalidationStrategy = SessionInvalidationStrategy.REDIRECT_TO_LOGIN;

        public enum SessionInvalidationStrategy {
            /**
             * 重定向到登录页
             */
            REDIRECT_TO_LOGIN,
            
            /**
             * 返回错误响应
             */
            ERROR_RESPONSE,
            
            /**
             * 踢出最早的会话
             */
            KICK_OUT_EARLIEST
        }
    }
}