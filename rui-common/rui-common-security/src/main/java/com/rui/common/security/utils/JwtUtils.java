package com.rui.common.security.utils;

import com.rui.common.core.constant.Constants;
import com.rui.common.security.config.JwtSecurityConfig;
import com.rui.common.security.service.JwtKeyManager;
import com.rui.common.security.service.JwtBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtSecurityConfig jwtConfig;
    private final JwtKeyManager keyManager;
    private final JwtBlacklistService blacklistService;

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public Claims getClaimsFromToken(String token) {
        Claims claims = null;
        try {
            // 检查令牌是否在黑名单中
            if (blacklistService.isBlacklisted(token)) {
                log.warn("令牌已在黑名单中: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
                return null;
            }
            
            // 先尝试使用当前密钥解析
            try {
                claims = Jwts.parser()
                        .setSigningKey(getSignKey(keyManager.getCurrentSecretKey()))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (Exception e) {
                // 如果当前密钥失败，尝试使用上一个密钥（密钥轮换场景）
                String previousKey = keyManager.getPreviousSecretKey();
                if (previousKey != null) {
                    claims = Jwts.parser()
                            .setSigningKey(getSignKey(previousKey))
                            .build()
                            .parseClaimsJws(token)
                            .getBody();
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            log.error("JWT格式验证失败:{}", token.substring(0, Math.min(token.length(), 20)) + "...");
        }
        return claims;
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    public String createToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpireTime().toMillis());
        
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSignKey(keyManager.getCurrentSecretKey()), SignatureAlgorithm.HS512)
                .compact();
                
        // 如果启用单点登录，记录用户令牌
        if (jwtConfig.isEnableSingleSignOn() && claims.containsKey("userId")) {
            String userId = claims.get("userId").toString();
            blacklistService.setSingleSignOnToken(userId, token);
        }
        
        return token;
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    public Claims parseToken(String token) {
        return getClaimsFromToken(token);
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 验证令牌
     *
     * @param token 令牌
     * @return 是否有效
     */
    public Boolean validateToken(String token) {
        try {
            // 检查黑名单
            if (blacklistService.isBlacklisted(token)) {
                return false;
            }
            
            Claims claims = parseToken(token);
            if (claims == null) {
                return false;
            }
            
            // 检查是否过期
            if (isTokenExpired(token)) {
                return false;
            }
            
            // 如果启用单点登录，检查是否为用户的有效令牌
            if (jwtConfig.isEnableSingleSignOn() && claims.containsKey("userId")) {
                String userId = claims.get("userId").toString();
                return blacklistService.isValidUserToken(userId, token);
            }
            
            return true;
        } catch (Exception e) {
            log.error("token验证失败", e);
            return false;
        }
    }

    /**
     * 判断令牌是否过期
     *
     * @param token 令牌
     * @return 是否过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新令牌
     *
     * @param token 原令牌
     * @return 新令牌
     */
    public String refreshToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        
        // 将旧令牌加入黑名单
        long remainingTime = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
        if (remainingTime > 0) {
            blacklistService.addToBlacklist(token, remainingTime, "令牌刷新");
        }
        
        // 更新创建时间
        claims.put(Constants.JWT_CREATED, new Date());
        return createToken(claims);
    }

    /**
     * 获取请求token
     *
     * @param request
     * @return token
     */
    public String getToken(jakarta.servlet.http.HttpServletRequest request) {
        String token = request.getHeader(jwtConfig.getHeader());
        if (token != null && token.startsWith(jwtConfig.getTokenPrefix())) {
            token = token.replace(jwtConfig.getTokenPrefix(), "");
        }
        return token;
    }

    /**
     * 生成签名密钥
     *
     * @param secret 密钥字符串
     * @return 密钥
     */
    private SecretKey getSignKey(String secret) {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 注销令牌（加入黑名单）
     *
     * @param token 令牌
     */
    public void logout(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims != null) {
                long remainingTime = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
                if (remainingTime > 0) {
                    blacklistService.addToBlacklist(token, remainingTime, "用户注销");
                }
            }
        } catch (Exception e) {
            log.error("注销令牌失败", e);
        }
    }
    
    /**
     * 注销用户所有令牌
     *
     * @param userId 用户ID
     */
    public void logoutUser(String userId) {
        blacklistService.blacklistUserTokens(userId);
    }
}