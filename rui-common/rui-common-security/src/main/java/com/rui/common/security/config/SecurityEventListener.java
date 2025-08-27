package com.rui.common.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全事件监听器
 * 监听和记录各种安全相关事件，用于审计和安全分析
 *
 * @author rui
 */
@Slf4j
@Component
public class SecurityEventListener {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 监听认证成功事件
     */
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        Map<String, Object> auditData = createBaseAuditData("AUTHENTICATION_SUCCESS", auth);
        
        auditData.put("username", auth.getName());
        auditData.put("authorities", auth.getAuthorities().toString());
        
        logSecurityEvent("用户认证成功", auditData);
    }

    /**
     * 监听认证失败事件
     */
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication auth = event.getAuthentication();
        Exception exception = event.getException();
        
        Map<String, Object> auditData = createBaseAuditData("AUTHENTICATION_FAILURE", auth);
        auditData.put("username", auth != null ? auth.getName() : "unknown");
        auditData.put("failureReason", exception.getClass().getSimpleName());
        auditData.put("errorMessage", exception.getMessage());
        
        logSecurityEvent("用户认证失败", auditData);
    }

    /**
     * 监听坏凭据事件
     */
    @EventListener
    public void handleBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        Authentication auth = event.getAuthentication();
        Map<String, Object> auditData = createBaseAuditData("BAD_CREDENTIALS", auth);
        
        auditData.put("username", auth.getName());
        auditData.put("attemptedCredentials", "[PROTECTED]");
        
        logSecurityEvent("用户凭据错误", auditData);
    }

    /**
     * 监听账户锁定事件
     */
    @EventListener
    public void handleAccountLocked(AuthenticationFailureLockedEvent event) {
        Authentication auth = event.getAuthentication();
        Map<String, Object> auditData = createBaseAuditData("ACCOUNT_LOCKED", auth);
        
        auditData.put("username", auth.getName());
        
        logSecurityEvent("账户被锁定", auditData);
    }

    /**
     * 监听账户禁用事件
     */
    @EventListener
    public void handleAccountDisabled(AuthenticationFailureDisabledEvent event) {
        Authentication auth = event.getAuthentication();
        Map<String, Object> auditData = createBaseAuditData("ACCOUNT_DISABLED", auth);
        
        auditData.put("username", auth.getName());
        
        logSecurityEvent("账户被禁用", auditData);
    }

    /**
     * 监听账户过期事件
     */
    @EventListener
    public void handleAccountExpired(AuthenticationFailureExpiredEvent event) {
        Authentication auth = event.getAuthentication();
        Map<String, Object> auditData = createBaseAuditData("ACCOUNT_EXPIRED", auth);
        
        auditData.put("username", auth.getName());
        
        logSecurityEvent("账户已过期", auditData);
    }

    /**
     * 监听凭据过期事件
     */
    @EventListener
    public void handleCredentialsExpired(AuthenticationFailureCredentialsExpiredEvent event) {
        Authentication auth = event.getAuthentication();
        Map<String, Object> auditData = createBaseAuditData("CREDENTIALS_EXPIRED", auth);
        
        auditData.put("username", auth.getName());
        
        logSecurityEvent("用户凭据已过期", auditData);
    }

    /**
     * 监听授权拒绝事件
     */
    @EventListener
    public void handleAuthorizationDenied(AuthorizationDeniedEvent event) {
        Authentication auth = event.getAuthentication().get();
        Map<String, Object> auditData = createBaseAuditData("AUTHORIZATION_DENIED", auth);
        
        auditData.put("username", auth.getName());
        auditData.put("authorities", auth.getAuthorities().toString());
        auditData.put("deniedObject", event.getAuthorizationDecision().toString());
        
        logSecurityEvent("访问权限被拒绝", auditData);
    }

    /**
     * 监听会话创建事件
     */
    public void handleSessionCreated(String sessionId, String username) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", "SESSION_CREATED");
        auditData.put("sessionId", sessionId);
        auditData.put("username", username);
        auditData.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        logSecurityEvent("会话创建", auditData);
    }

    /**
     * 监听会话销毁事件
     */
    public void handleSessionDestroyed(String sessionId, String username, String reason) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", "SESSION_DESTROYED");
        auditData.put("sessionId", sessionId);
        auditData.put("username", username);
        auditData.put("reason", reason);
        auditData.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        logSecurityEvent("会话销毁", auditData);
    }

    /**
     * 监听JWT令牌创建事件
     */
    public void handleJwtTokenCreated(String username, String tokenId, String clientIp) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", "JWT_TOKEN_CREATED");
        auditData.put("username", username);
        auditData.put("tokenId", tokenId);
        auditData.put("clientIp", clientIp);
        auditData.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        logSecurityEvent("JWT令牌创建", auditData);
    }

    /**
     * 监听JWT令牌刷新事件
     */
    public void handleJwtTokenRefreshed(String username, String oldTokenId, String newTokenId, String clientIp) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", "JWT_TOKEN_REFRESHED");
        auditData.put("username", username);
        auditData.put("oldTokenId", oldTokenId);
        auditData.put("newTokenId", newTokenId);
        auditData.put("clientIp", clientIp);
        auditData.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        logSecurityEvent("JWT令牌刷新", auditData);
    }

    /**
     * 监听JWT令牌撤销事件
     */
    public void handleJwtTokenRevoked(String username, String tokenId, String reason, String clientIp) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", "JWT_TOKEN_REVOKED");
        auditData.put("username", username);
        auditData.put("tokenId", tokenId);
        auditData.put("reason", reason);
        auditData.put("clientIp", clientIp);
        auditData.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        logSecurityEvent("JWT令牌撤销", auditData);
    }

    /**
     * 监听密钥轮换事件
     */
    public void handleKeyRotation(String oldKeyId, String newKeyId, String reason) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", "KEY_ROTATION");
        auditData.put("oldKeyId", oldKeyId);
        auditData.put("newKeyId", newKeyId);
        auditData.put("reason", reason);
        auditData.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        logSecurityEvent("密钥轮换", auditData);
    }

    /**
     * 监听IP封禁事件
     */
    public void handleIpBlocked(String ip, String reason, long durationSeconds) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", "IP_BLOCKED");
        auditData.put("ip", ip);
        auditData.put("reason", reason);
        auditData.put("durationSeconds", durationSeconds);
        auditData.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        logSecurityEvent("IP地址封禁", auditData);
    }

    /**
     * 监听频率限制触发事件
     */
    public void handleRateLimitExceeded(String identifier, String limitType, int requests, int windowSeconds) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", "RATE_LIMIT_EXCEEDED");
        auditData.put("identifier", identifier);
        auditData.put("limitType", limitType);
        auditData.put("requests", requests);
        auditData.put("windowSeconds", windowSeconds);
        auditData.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        logSecurityEvent("频率限制触发", auditData);
    }

    /**
     * 监听可疑活动事件
     */
    public void handleSuspiciousActivity(String activityType, String description, Map<String, Object> details) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", "SUSPICIOUS_ACTIVITY");
        auditData.put("activityType", activityType);
        auditData.put("description", description);
        auditData.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        if (details != null) {
            auditData.putAll(details);
        }
        
        logSecurityEvent("可疑活动检测", auditData);
    }

    /**
     * 创建基础审计数据
     */
    private Map<String, Object> createBaseAuditData(String eventType, Authentication auth) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("eventType", eventType);
        auditData.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        // 获取客户端IP和会话信息
        if (auth != null && auth.getDetails() instanceof WebAuthenticationDetails) {
            WebAuthenticationDetails details = (WebAuthenticationDetails) auth.getDetails();
            auditData.put("clientIp", details.getRemoteAddress());
            auditData.put("sessionId", details.getSessionId());
        }
        
        return auditData;
    }

    /**
     * 记录安全事件
     */
    private void logSecurityEvent(String message, Map<String, Object> auditData) {
        // 结构化日志记录
        log.info("[SECURITY_AUDIT] {} - {}", message, auditData);
        
        // 这里可以扩展为发送到外部审计系统
        // 例如：发送到Elasticsearch、数据库、消息队列等
        sendToAuditSystem(auditData);
    }

    /**
     * 发送到外部审计系统
     * 可以根据需要实现具体的审计系统集成
     */
    private void sendToAuditSystem(Map<String, Object> auditData) {
        // TODO: 实现外部审计系统集成
        // 例如：
        // - 发送到Elasticsearch进行日志分析
        // - 存储到数据库进行长期保存
        // - 发送到消息队列进行异步处理
        // - 发送到SIEM系统进行安全分析
        
        // 示例：异步发送到消息队列
        // rabbitTemplate.convertAndSend("security.audit.exchange", "audit.event", auditData);
        
        // 示例：存储到数据库
        // auditLogService.saveAuditLog(auditData);
        
        // 示例：发送到Elasticsearch
        // elasticsearchTemplate.index(IndexQuery.builder()
        //     .withIndexName("security-audit")
        //     .withObject(auditData)
        //     .build());
    }

    /**
     * 获取当前用户信息
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    /**
     * 获取客户端IP（从当前请求中）
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
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}