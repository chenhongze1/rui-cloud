package com.rui.common.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 安全配置验证器
 * 在应用启动时验证安全配置的有效性
 *
 * @author rui
 */
@Slf4j
@Component
public class SecurityConfigValidator {

    private JwtSecurityConfig jwtSecurityConfig;
    private SecurityHeaderConfig securityHeaderConfig;
    private AccessControlConfig accessControlConfig;

    public SecurityConfigValidator() {
    }

    public SecurityConfigValidator(JwtSecurityConfig jwtSecurityConfig,
                                 SecurityHeaderConfig securityHeaderConfig,
                                 AccessControlConfig accessControlConfig) {
        this.jwtSecurityConfig = jwtSecurityConfig;
        this.securityHeaderConfig = securityHeaderConfig;
        this.accessControlConfig = accessControlConfig;
    }

    /**
     * 应用启动后验证配置
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfigurations() {
        log.info("开始验证安全配置...");

        try {
            if (jwtSecurityConfig != null) {
                validateJwtConfig();
            }
            if (securityHeaderConfig != null) {
                validateSecurityHeaderConfig();
            }
            if (accessControlConfig != null) {
                validateAccessControlConfig();
            }

            log.info("安全配置验证完成");
        } catch (Exception e) {
            log.error("安全配置验证失败", e);
            throw new IllegalStateException("安全配置验证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证JWT配置
     */
    private void validateJwtConfig() {
        log.debug("验证JWT配置...");

        // 验证密钥长度
        if (StringUtils.hasText(jwtSecurityConfig.getSecret())) {
            if (jwtSecurityConfig.getSecret().length() < 32) {
                log.warn("JWT密钥长度过短，建议至少32个字符");
            }
        }

        // 验证过期时间
        if (jwtSecurityConfig.getExpiration().isNegative() || jwtSecurityConfig.getExpiration().isZero()) {
            throw new IllegalArgumentException("JWT过期时间必须为正数");
        }

        if (jwtSecurityConfig.getExpiration().toDays() > 30) {
            log.warn("JWT过期时间过长（{}天），存在安全风险", jwtSecurityConfig.getExpiration().toDays());
        }

        // 验证刷新令牌过期时间
        if (jwtSecurityConfig.getRefreshExpiration().compareTo(jwtSecurityConfig.getExpiration()) <= 0) {
            throw new IllegalArgumentException("刷新令牌过期时间必须大于访问令牌过期时间");
        }

        // 验证密钥轮换配置
        JwtSecurityConfig.KeyRotationConfig keyRotation = jwtSecurityConfig.getKeyRotation();
        if (keyRotation.isEnabled()) {
            if (keyRotation.getInterval().isNegative() || keyRotation.getInterval().isZero()) {
                throw new IllegalArgumentException("密钥轮换间隔必须为正数");
            }

            if (keyRotation.getInterval().toDays() < 1) {
                log.warn("密钥轮换间隔过短（{}小时），可能影响性能", keyRotation.getInterval().toHours());
            }

            if (keyRotation.getGracePeriod().compareTo(keyRotation.getInterval()) >= 0) {
                throw new IllegalArgumentException("密钥宽限期必须小于轮换间隔");
            }
        }

        // 验证黑名单配置
        JwtSecurityConfig.BlacklistConfig blacklist = jwtSecurityConfig.getBlacklist();
        if (blacklist.isEnabled()) {
            if (blacklist.getCleanupInterval().isNegative() || blacklist.getCleanupInterval().isZero()) {
                throw new IllegalArgumentException("黑名单清理间隔必须为正数");
            }
        }

        log.debug("JWT配置验证通过");
    }

    /**
     * 验证安全头配置
     */
    private void validateSecurityHeaderConfig() {
        log.debug("验证安全头配置...");

        if (!securityHeaderConfig.isEnabled()) {
            return;
        }

        // 验证CSP配置
        SecurityHeaderConfig.CspConfig csp = securityHeaderConfig.getCsp();
        if (csp.isEnabled() && !StringUtils.hasText(csp.getPolicy())) {
            log.warn("CSP已启用但未配置策略");
        }

        // 验证HSTS配置
        SecurityHeaderConfig.HstsConfig hsts = securityHeaderConfig.getHsts();
        if (hsts.isEnabled()) {
            if (hsts.getMaxAge().isNegative()) {
                throw new IllegalArgumentException("HSTS max-age必须为非负数");
            }
            if (hsts.getMaxAge().toDays() < 30) {
                log.warn("HSTS max-age过短（{}天），建议至少30天", hsts.getMaxAge().toDays());
            }
        }

        // 验证自定义头
        if (securityHeaderConfig.getCustomHeaders() != null) {
            securityHeaderConfig.getCustomHeaders().forEach((name, value) -> {
                if (!StringUtils.hasText(name) || !StringUtils.hasText(value)) {
                    throw new IllegalArgumentException("自定义头名称和值不能为空");
                }
            });
        }

        log.debug("安全头配置验证通过");
    }

    /**
     * 验证访问控制配置
     */
    private void validateAccessControlConfig() {
        log.debug("验证访问控制配置...");

        if (!accessControlConfig.isEnabled()) {
            return;
        }

        // 验证IP控制配置
        validateIpControlConfig();

        // 验证用户代理控制配置
        validateUserAgentControlConfig();

        // 验证请求频率控制配置
        validateRequestRateControlConfig();

        log.debug("访问控制配置验证通过");
    }

    /**
     * 验证IP控制配置
     */
    private void validateIpControlConfig() {
        AccessControlConfig.IpControlConfig ipControl = accessControlConfig.getIpControl();
        if (!ipControl.isEnabled()) {
            return;
        }

        // 验证IP格式
        validateIpList(ipControl.getWhitelist(), "白名单");
        validateIpList(ipControl.getBlacklist(), "黑名单");

        // 检查配置冲突
        if (ipControl.getWhitelist() != null && !ipControl.getWhitelist().isEmpty() &&
            ipControl.getBlacklist() != null && !ipControl.getBlacklist().isEmpty()) {
            log.warn("同时配置了IP白名单和黑名单，白名单优先级更高");
        }
    }

    /**
     * 验证IP列表格式
     */
    private void validateIpList(List<String> ipList, String listType) {
        if (ipList == null) {
            return;
        }

        for (String ip : ipList) {
            if (!StringUtils.hasText(ip)) {
                throw new IllegalArgumentException(listType + "中包含空IP地址");
            }

            // 简单的IP格式验证
            if (!isValidIpPattern(ip)) {
                throw new IllegalArgumentException(listType + "中包含无效IP格式: " + ip);
            }
        }
    }

    /**
     * 验证IP模式格式
     */
    private boolean isValidIpPattern(String ip) {
        // 支持通配符、CIDR、普通IP
        if (ip.contains("*")) {
            return true; // 通配符格式
        }
        if (ip.contains("/")) {
            // CIDR格式验证
            String[] parts = ip.split("/");
            if (parts.length != 2) {
                return false;
            }
            try {
                int prefix = Integer.parseInt(parts[1]);
                return prefix >= 0 && prefix <= 32;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        // 普通IP格式验证（简化版）
        return ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    }

    /**
     * 验证用户代理控制配置
     */
    private void validateUserAgentControlConfig() {
        AccessControlConfig.UserAgentControlConfig uaControl = accessControlConfig.getUserAgentControl();
        if (!uaControl.isEnabled()) {
            return;
        }

        // 验证正则表达式
        validateRegexPatterns(uaControl.getBlockedPatterns(), "禁止的用户代理模式");
        validateRegexPatterns(uaControl.getAllowedPatterns(), "允许的用户代理模式");
    }

    /**
     * 验证正则表达式模式
     */
    private void validateRegexPatterns(List<String> patterns, String patternType) {
        if (patterns == null) {
            return;
        }

        for (String pattern : patterns) {
            if (!StringUtils.hasText(pattern)) {
                throw new IllegalArgumentException(patternType + "中包含空模式");
            }

            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException(patternType + "中包含无效正则表达式: " + pattern, e);
            }
        }
    }

    /**
     * 验证请求频率控制配置
     */
    private void validateRequestRateControlConfig() {
        AccessControlConfig.RequestRateControlConfig rateControl = accessControlConfig.getRequestRateControl();
        if (!rateControl.isEnabled()) {
            return;
        }

        // 验证全局规则
        validateRateLimitRule(rateControl.getGlobalRule(), "全局频率限制");

        // 验证IP规则
        validateRateLimitRule(rateControl.getIpRule(), "IP频率限制");

        // 验证路径规则
        if (rateControl.getPathRules() != null) {
            rateControl.getPathRules().forEach((path, rule) -> {
                if (!StringUtils.hasText(path)) {
                    throw new IllegalArgumentException("路径频率限制中包含空路径");
                }
                validateRateLimitRule(rule, "路径频率限制[" + path + "]");
            });
        }
    }

    /**
     * 验证频率限制规则
     */
    private void validateRateLimitRule(AccessControlConfig.RequestRateControlConfig.RateLimitRule rule, String ruleType) {
        if (!rule.isEnabled()) {
            return;
        }

        if (rule.getRequests() <= 0) {
            throw new IllegalArgumentException(ruleType + "的请求次数必须为正数");
        }

        if (rule.getWindow().isNegative() || rule.getWindow().isZero()) {
            throw new IllegalArgumentException(ruleType + "的时间窗口必须为正数");
        }

        if (rule.getBanDuration().isNegative()) {
            throw new IllegalArgumentException(ruleType + "的封禁时长不能为负数");
        }

        // 合理性检查
        if (rule.getWindow().toMinutes() > 60) {
            log.warn("{}的时间窗口过长（{}分钟），可能影响用户体验", ruleType, rule.getWindow().toMinutes());
        }

        if (rule.getBanDuration().toHours() > 24) {
            log.warn("{}的封禁时长过长（{}小时），可能影响用户体验", ruleType, rule.getBanDuration().toHours());
        }
    }
}