package com.rui.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 安全头配置
 *
 * @author rui
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.headers")
public class SecurityHeaderConfig {

    /**
     * 是否启用安全头
     */
    private boolean enabled = true;

    /**
     * Content Security Policy 配置
     */
    private CspConfig csp = new CspConfig();

    /**
     * HSTS 配置
     */
    private HstsConfig hsts = new HstsConfig();

    /**
     * X-Frame-Options 配置
     */
    private FrameOptionsConfig frameOptions = new FrameOptionsConfig();

    /**
     * X-Content-Type-Options 配置
     */
    private boolean contentTypeOptions = true;

    /**
     * X-XSS-Protection 配置
     */
    private XssProtectionConfig xssProtection = new XssProtectionConfig();

    /**
     * Referrer Policy 配置
     */
    private String referrerPolicy = "strict-origin-when-cross-origin";

    /**
     * Permissions Policy 配置
     */
    private PermissionsPolicyConfig permissionsPolicy = new PermissionsPolicyConfig();

    /**
     * 自定义安全头
     */
    private Map<String, String> customHeaders;

    @Data
    public static class CspConfig {
        /**
         * 是否启用 CSP
         */
        private boolean enabled = true;

        /**
         * CSP 策略
         */
        private String policy = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none';";

        /**
         * 是否仅报告模式
         */
        private boolean reportOnly = false;

        /**
         * 报告 URI
         */
        private String reportUri;
    }

    @Data
    public static class HstsConfig {
        /**
         * 是否启用 HSTS
         */
        private boolean enabled = true;

        /**
         * 最大年龄（秒）
         */
        private Duration maxAge = Duration.ofDays(365);

        /**
         * 是否包含子域名
         */
        private boolean includeSubdomains = true;

        /**
         * 是否预加载
         */
        private boolean preload = false;
    }

    @Data
    public static class FrameOptionsConfig {
        /**
         * 是否启用 X-Frame-Options
         */
        private boolean enabled = true;

        /**
         * 策略：DENY, SAMEORIGIN, ALLOW-FROM
         */
        private String policy = "DENY";

        /**
         * 允许的来源（当策略为 ALLOW-FROM 时）
         */
        private String allowFrom;
    }

    @Data
    public static class XssProtectionConfig {
        /**
         * 是否启用 X-XSS-Protection
         */
        private boolean enabled = true;

        /**
         * 是否启用保护
         */
        private boolean protection = true;

        /**
         * 是否阻止模式
         */
        private boolean block = true;
    }

    @Data
    public static class PermissionsPolicyConfig {
        /**
         * 是否启用 Permissions Policy
         */
        private boolean enabled = true;

        /**
         * 策略配置
         */
        private Map<String, List<String>> policies;

        /**
         * 获取默认策略
         */
        public Map<String, List<String>> getDefaultPolicies() {
            return Map.of(
                "camera", List.of("'none'"),
                "microphone", List.of("'none'"),
                "geolocation", List.of("'none'"),
                "payment", List.of("'none'"),
                "usb", List.of("'none'")
            );
        }
    }
}