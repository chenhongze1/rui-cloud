package com.rui.common.security.service;

import com.rui.common.security.config.JwtSecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JWT黑名单服务
 *
 * @author rui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final JwtSecurityConfig jwtConfig;
    private final StringRedisTemplate redisTemplate;

    /**
     * 将令牌加入黑名单
     *
     * @param token 令牌
     * @param expireTime 过期时间（秒）
     * @param reason 加入黑名单的原因
     */
    public void addToBlacklist(String token, long expireTime, String reason) {
        if (!jwtConfig.isEnableBlacklist()) {
            return;
        }

        try {
            String key = jwtConfig.getBlacklistPrefix() + token;
            String value = String.format("{\"reason\":\"%s\",\"time\":\"%s\"}", 
                reason, LocalDateTime.now());
            
            redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
            
            if (jwtConfig.isEnableAuditLog()) {
                logAuditEvent("TOKEN_BLACKLISTED", token, reason);
            }
            
            log.debug("令牌已加入黑名单: {}, 原因: {}", token.substring(0, Math.min(token.length(), 20)) + "...", reason);
        } catch (Exception e) {
            log.error("添加令牌到黑名单失败", e);
        }
    }

    /**
     * 检查令牌是否在黑名单中
     *
     * @param token 令牌
     * @return true表示在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (!jwtConfig.isEnableBlacklist()) {
            return false;
        }

        try {
            String key = jwtConfig.getBlacklistPrefix() + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查令牌黑名单状态失败", e);
            // 异常情况下认为令牌有效，避免影响正常业务
            return false;
        }
    }

    /**
     * 从黑名单中移除令牌
     *
     * @param token 令牌
     */
    public void removeFromBlacklist(String token) {
        if (!jwtConfig.isEnableBlacklist()) {
            return;
        }

        try {
            String key = jwtConfig.getBlacklistPrefix() + token;
            redisTemplate.delete(key);
            
            if (jwtConfig.isEnableAuditLog()) {
                logAuditEvent("TOKEN_REMOVED_FROM_BLACKLIST", token, "手动移除");
            }
            
            log.debug("令牌已从黑名单移除: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
        } catch (Exception e) {
            log.error("从黑名单移除令牌失败", e);
        }
    }

    /**
     * 用户注销时，将该用户的所有令牌加入黑名单
     *
     * @param userId 用户ID
     */
    public void blacklistUserTokens(String userId) {
        if (!jwtConfig.isEnableBlacklist()) {
            return;
        }

        try {
            String pattern = jwtConfig.getUserTokenPrefix() + userId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    String token = redisTemplate.opsForValue().get(key);
                    if (token != null) {
                        addToBlacklist(token, jwtConfig.getExpireTime().toSeconds(), "用户注销");
                    }
                }
                
                // 删除用户令牌记录
                redisTemplate.delete(keys);
            }
            
            if (jwtConfig.isEnableAuditLog()) {
                logAuditEvent("USER_TOKENS_BLACKLISTED", userId, "用户注销");
            }
            
            log.debug("用户{}的所有令牌已加入黑名单", userId);
        } catch (Exception e) {
            log.error("将用户令牌加入黑名单失败", e);
        }
    }

    /**
     * 单点登录：为用户设置新令牌，并将旧令牌加入黑名单
     *
     * @param userId 用户ID
     * @param newToken 新令牌
     */
    public void setSingleSignOnToken(String userId, String newToken) {
        if (!jwtConfig.isEnableSingleSignOn()) {
            return;
        }

        try {
            // 先将用户的旧令牌加入黑名单
            blacklistUserTokens(userId);
            
            // 设置新的用户令牌
            String key = jwtConfig.getUserTokenPrefix() + userId;
            redisTemplate.opsForValue().set(key, newToken, 
                jwtConfig.getExpireTime().toSeconds(), TimeUnit.SECONDS);
            
            if (jwtConfig.isEnableAuditLog()) {
                logAuditEvent("SSO_TOKEN_SET", userId, "单点登录令牌设置");
            }
            
            log.debug("用户{}的单点登录令牌已设置", userId);
        } catch (Exception e) {
            log.error("设置单点登录令牌失败", e);
        }
    }

    /**
     * 检查是否为用户的有效令牌（单点登录场景）
     *
     * @param userId 用户ID
     * @param token 令牌
     * @return true表示是有效令牌
     */
    public boolean isValidUserToken(String userId, String token) {
        if (!jwtConfig.isEnableSingleSignOn()) {
            return true; // 未启用单点登录，不进行检查
        }

        try {
            String key = jwtConfig.getUserTokenPrefix() + userId;
            String storedToken = redisTemplate.opsForValue().get(key);
            return token.equals(storedToken);
        } catch (Exception e) {
            log.error("检查用户令牌有效性失败", e);
            return true; // 异常情况下认为令牌有效
        }
    }

    /**
     * 清理过期的黑名单记录
     */
    public void cleanupExpiredBlacklist() {
        // Redis会自动清理过期的key，这里主要用于统计和日志
        if (jwtConfig.isEnableAuditLog()) {
            logAuditEvent("BLACKLIST_CLEANUP", "system", "定时清理过期黑名单");
        }
    }

    /**
     * 记录审计日志
     */
    private void logAuditEvent(String event, String target, String details) {
        try {
            String logKey = jwtConfig.getAuditLogPrefix() + LocalDateTime.now().toLocalDate();
            String logEntry = String.format("{\"time\":\"%s\",\"event\":\"%s\",\"target\":\"%s\",\"details\":\"%s\"}",
                LocalDateTime.now(), event, target, details);
            
            redisTemplate.opsForList().leftPush(logKey, logEntry);
            redisTemplate.opsForList().trim(logKey, 0, 9999); // 保留最近10000条记录
            redisTemplate.expire(logKey, 30, TimeUnit.DAYS); // 日志保留30天
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    /**
     * 获取黑名单统计信息
     */
    public long getBlacklistCount() {
        try {
            String pattern = jwtConfig.getBlacklistPrefix() + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("获取黑名单统计失败", e);
            return 0;
        }
    }
}