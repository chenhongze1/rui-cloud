package com.rui.common.security.filter;

import com.rui.common.core.enums.ErrorCode;
import com.rui.common.core.exception.ServiceException;
import com.rui.common.security.config.AccessControlConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 访问控制过滤器
 * 实现IP控制、用户代理检查、请求频率限制等安全功能
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class AccessControlFilter implements Filter {

    private final AccessControlConfig accessControlConfig;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Redis 键前缀
    private static final String IP_BLACKLIST_KEY = "security:ip:blacklist:";
    private static final String IP_FAILURE_KEY = "security:ip:failure:";
    private static final String RATE_LIMIT_KEY = "security:rate:";

    // 内网IP模式
    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
        "^(10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.)");
    private static final Pattern LOCALHOST_PATTERN = Pattern.compile(
        "^(127\\.|::1|localhost)");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!accessControlConfig.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // IP 访问控制
            checkIpAccess(httpRequest);

            // 用户代理控制
            checkUserAgent(httpRequest);

            // 请求频率控制
            checkRequestRate(httpRequest);

            chain.doFilter(request, response);

        } catch (ServiceException e) {
            handleAccessDenied(httpResponse, e);
        }
    }

    /**
     * 检查IP访问权限
     */
    private void checkIpAccess(HttpServletRequest request) {
        AccessControlConfig.IpControlConfig ipConfig = accessControlConfig.getIpControl();
        if (!ipConfig.isEnabled()) {
            return;
        }

        String clientIp = getClientIp(request);
        log.debug("检查IP访问权限: {}", clientIp);

        // 检查动态黑名单
        if (isInDynamicBlacklist(clientIp)) {
            log.warn("IP {} 在动态黑名单中，拒绝访问", clientIp);
            throw ServiceException.of(ErrorCode.ACCESS_DENIED, "IP已被临时封禁");
        }

        // 检查静态黑名单
        if (isInBlacklist(clientIp, ipConfig.getBlacklist())) {
            log.warn("IP {} 在黑名单中，拒绝访问", clientIp);
            throw ServiceException.of(ErrorCode.ACCESS_DENIED, "IP访问被拒绝");
        }

        // 检查白名单（如果配置了白名单）
        if (ipConfig.getWhitelist() != null && !ipConfig.getWhitelist().isEmpty()) {
            if (!isInWhitelist(clientIp, ipConfig)) {
                log.warn("IP {} 不在白名单中，拒绝访问", clientIp);
                throw ServiceException.of(ErrorCode.ACCESS_DENIED, "IP访问被拒绝");
            }
        }
    }

    /**
     * 检查用户代理
     */
    private void checkUserAgent(HttpServletRequest request) {
        AccessControlConfig.UserAgentControlConfig uaConfig = accessControlConfig.getUserAgentControl();
        if (!uaConfig.isEnabled()) {
            return;
        }

        String userAgent = request.getHeader("User-Agent");
        log.debug("检查用户代理: {}", userAgent);

        // 检查空用户代理
        if (!StringUtils.hasText(userAgent) && uaConfig.isBlockEmptyUserAgent()) {
            log.warn("空用户代理被拒绝访问");
            throw ServiceException.of(ErrorCode.ACCESS_DENIED, "无效的用户代理");
        }

        if (StringUtils.hasText(userAgent)) {
            // 检查禁止的用户代理模式
            if (isBlockedUserAgent(userAgent, uaConfig.getBlockedPatterns())) {
                log.warn("用户代理 {} 被拒绝访问", userAgent);
                throw ServiceException.of(ErrorCode.ACCESS_DENIED, "用户代理被拒绝");
            }

            // 检查允许的用户代理模式（如果配置了）
            if (uaConfig.getAllowedPatterns() != null && !uaConfig.getAllowedPatterns().isEmpty()) {
                if (!isAllowedUserAgent(userAgent, uaConfig.getAllowedPatterns())) {
                    log.warn("用户代理 {} 不在允许列表中", userAgent);
                    throw ServiceException.of(ErrorCode.ACCESS_DENIED, "用户代理不被允许");
                }
            }

            // 检查可疑爬虫
            if (uaConfig.isBlockSuspiciousBots() && isSuspiciousBot(userAgent)) {
                log.warn("可疑爬虫用户代理 {} 被拒绝访问", userAgent);
                throw ServiceException.of(ErrorCode.ACCESS_DENIED, "可疑爬虫被拒绝");
            }
        }
    }

    /**
     * 检查请求频率
     */
    private void checkRequestRate(HttpServletRequest request) {
        AccessControlConfig.RequestRateControlConfig rateConfig = accessControlConfig.getRequestRateControl();
        if (!rateConfig.isEnabled()) {
            return;
        }

        String clientIp = getClientIp(request);
        String requestPath = request.getRequestURI();

        // 检查IP频率限制
        if (rateConfig.getIpRule().isEnabled()) {
            checkRateLimit("ip:" + clientIp, rateConfig.getIpRule());
        }

        // 检查路径频率限制
        if (rateConfig.getPathRules() != null) {
            for (var entry : rateConfig.getPathRules().entrySet()) {
                if (pathMatcher.match(entry.getKey(), requestPath)) {
                    checkRateLimit("path:" + entry.getKey() + ":" + clientIp, entry.getValue());
                    break;
                }
            }
        }

        // 检查全局频率限制
        if (rateConfig.getGlobalRule().isEnabled()) {
            checkRateLimit("global", rateConfig.getGlobalRule());
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 取第一个IP（可能有多个IP用逗号分隔）
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 检查是否在动态黑名单中
     */
    private boolean isInDynamicBlacklist(String ip) {
        String key = IP_BLACKLIST_KEY + ip;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 检查是否在静态黑名单中
     */
    private boolean isInBlacklist(String ip, List<String> blacklist) {
        if (blacklist == null || blacklist.isEmpty()) {
            return false;
        }
        return blacklist.stream().anyMatch(pattern -> matchIpPattern(ip, pattern));
    }

    /**
     * 检查是否在白名单中
     */
    private boolean isInWhitelist(String ip, AccessControlConfig.IpControlConfig ipConfig) {
        List<String> whitelist = ipConfig.getWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }

        // 检查内网IP
        if (ipConfig.isAllowPrivateIp() && PRIVATE_IP_PATTERN.matcher(ip).find()) {
            return true;
        }

        // 检查本地IP
        if (ipConfig.isAllowLocalhost() && LOCALHOST_PATTERN.matcher(ip).find()) {
            return true;
        }

        return whitelist.stream().anyMatch(pattern -> matchIpPattern(ip, pattern));
    }

    /**
     * IP模式匹配
     */
    private boolean matchIpPattern(String ip, String pattern) {
        if (pattern.contains("*")) {
            return pathMatcher.match(pattern, ip);
        }
        if (pattern.contains("/")) {
            // CIDR 格式
            return matchCidr(ip, pattern);
        }
        return ip.equals(pattern);
    }

    /**
     * CIDR匹配
     */
    private boolean matchCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress targetAddr = InetAddress.getByName(ip);
            InetAddress cidrAddr = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);

            byte[] targetBytes = targetAddr.getAddress();
            byte[] cidrBytes = cidrAddr.getAddress();

            int bytesToCheck = prefixLength / 8;
            int bitsToCheck = prefixLength % 8;

            for (int i = 0; i < bytesToCheck; i++) {
                if (targetBytes[i] != cidrBytes[i]) {
                    return false;
                }
            }

            if (bitsToCheck > 0) {
                int mask = 0xFF << (8 - bitsToCheck);
                return (targetBytes[bytesToCheck] & mask) == (cidrBytes[bytesToCheck] & mask);
            }

            return true;
        } catch (Exception e) {
            log.warn("CIDR匹配失败: {} vs {}", ip, cidr, e);
            return false;
        }
    }

    /**
     * 检查是否为被禁止的用户代理
     */
    private boolean isBlockedUserAgent(String userAgent, List<String> blockedPatterns) {
        if (blockedPatterns == null || blockedPatterns.isEmpty()) {
            return false;
        }
        return blockedPatterns.stream().anyMatch(pattern -> 
            Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(userAgent).find());
    }

    /**
     * 检查是否为允许的用户代理
     */
    private boolean isAllowedUserAgent(String userAgent, List<String> allowedPatterns) {
        return allowedPatterns.stream().anyMatch(pattern -> 
            Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(userAgent).find());
    }

    /**
     * 检查是否为可疑爬虫
     */
    private boolean isSuspiciousBot(String userAgent) {
        String[] suspiciousPatterns = {
            "bot", "crawler", "spider", "scraper", "wget", "curl",
            "python", "java", "go-http", "okhttp", "apache-httpclient"
        };
        
        String lowerUserAgent = userAgent.toLowerCase();
        for (String pattern : suspiciousPatterns) {
            if (lowerUserAgent.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查频率限制
     */
    private void checkRateLimit(String key, AccessControlConfig.RequestRateControlConfig.RateLimitRule rule) {
        String rateLimitKey = RATE_LIMIT_KEY + key;
        
        try {
            Long currentCount = redisTemplate.opsForValue().increment(rateLimitKey);
            if (currentCount == 1) {
                redisTemplate.expire(rateLimitKey, rule.getWindow().getSeconds(), TimeUnit.SECONDS);
            }

            if (currentCount > rule.getRequests()) {
                log.warn("频率限制触发: {} 超过 {} 次/{}秒", key, rule.getRequests(), rule.getWindow().getSeconds());
                
                // 添加到临时黑名单
                if (key.startsWith("ip:")) {
                    String ip = key.substring(3);
                    addToDynamicBlacklist(ip, rule.getBanDuration());
                }
                
                throw ServiceException.of(ErrorCode.RATE_LIMIT_EXCEEDED);
            }
        } catch (Exception e) {
            if (e instanceof ServiceException) {
                throw e;
            }
            log.error("频率限制检查失败: {}", key, e);
        }
    }

    /**
     * 添加到动态黑名单
     */
    private void addToDynamicBlacklist(String ip, Duration duration) {
        String key = IP_BLACKLIST_KEY + ip;
        redisTemplate.opsForValue().set(key, true, duration.getSeconds(), TimeUnit.SECONDS);
        log.info("IP {} 已添加到动态黑名单，封禁时长: {}秒", ip, duration.getSeconds());
    }

    /**
     * 处理访问被拒绝
     */
    private void handleAccessDenied(HttpServletResponse response, ServiceException e) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"code\":" + e.getCode() + ",\"message\":\"" + e.getMessage() + "\"}");
    }
}