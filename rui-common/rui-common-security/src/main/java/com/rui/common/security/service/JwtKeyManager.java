package com.rui.common.security.service;

import com.rui.common.security.config.JwtSecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * JWT密钥管理服务
 *
 * @author rui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtKeyManager {

    private final JwtSecurityConfig jwtConfig;
    private final StringRedisTemplate redisTemplate;

    private static final String CURRENT_KEY = "jwt:key:current";
    private static final String PREVIOUS_KEY = "jwt:key:previous";
    private static final String KEY_ROTATION_LOG = "jwt:key:rotation:log";

    private volatile String currentSecretKey;
    private volatile String previousSecretKey;

    /**
     * 获取当前密钥
     */
    public String getCurrentSecretKey() {
        if (currentSecretKey == null) {
            synchronized (this) {
                if (currentSecretKey == null) {
                    initializeKeys();
                }
            }
        }
        return currentSecretKey;
    }

    /**
     * 获取上一个密钥（用于验证旧令牌）
     */
    public String getPreviousSecretKey() {
        return previousSecretKey;
    }

    /**
     * 初始化密钥
     */
    private void initializeKeys() {
        // 从Redis获取当前密钥
        currentSecretKey = redisTemplate.opsForValue().get(CURRENT_KEY);
        previousSecretKey = redisTemplate.opsForValue().get(PREVIOUS_KEY);

        // 如果Redis中没有密钥，则生成新密钥
        if (currentSecretKey == null) {
            if (jwtConfig.getSecret() != null && !jwtConfig.getSecret().isEmpty()) {
                // 使用配置的密钥
                currentSecretKey = jwtConfig.getSecret();
            } else {
                // 生成新密钥
                currentSecretKey = generateSecretKey();
            }
            
            // 保存到Redis
            redisTemplate.opsForValue().set(CURRENT_KEY, currentSecretKey, 
                jwtConfig.getKeyRotationInterval().toSeconds(), TimeUnit.SECONDS);
            
            log.info("JWT密钥已初始化");
        }
    }

    /**
     * 生成安全的密钥
     */
    private String generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA512");
            keyGenerator.init(512, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            log.error("生成密钥失败", e);
            // 降级方案：生成随机字符串
            return generateRandomKey();
        }
    }

    /**
     * 生成随机密钥（降级方案）
     */
    private String generateRandomKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[Math.max(jwtConfig.getMinKeyLength(), 64)];
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    /**
     * 密钥轮换（定时任务）
     */
    @Scheduled(fixedRateString = "#{@jwtSecurityConfig.keyRotationInterval.toMillis()}")
    public void rotateKey() {
        if (!jwtConfig.isEnableKeyRotation()) {
            return;
        }

        try {
            log.info("开始执行JWT密钥轮换");

            // 保存当前密钥为上一个密钥
            previousSecretKey = currentSecretKey;
            redisTemplate.opsForValue().set(PREVIOUS_KEY, previousSecretKey, 
                jwtConfig.getKeyRotationInterval().toSeconds() * 2, TimeUnit.SECONDS);

            // 生成新的当前密钥
            currentSecretKey = generateSecretKey();
            redisTemplate.opsForValue().set(CURRENT_KEY, currentSecretKey, 
                jwtConfig.getKeyRotationInterval().toSeconds(), TimeUnit.SECONDS);

            // 记录轮换日志
            String logEntry = String.format("密钥轮换完成，时间：%s", LocalDateTime.now());
            redisTemplate.opsForList().leftPush(KEY_ROTATION_LOG, logEntry);
            redisTemplate.opsForList().trim(KEY_ROTATION_LOG, 0, 99); // 保留最近100条记录

            log.info("JWT密钥轮换完成");

        } catch (Exception e) {
            log.error("JWT密钥轮换失败", e);
        }
    }

    /**
     * 手动轮换密钥
     */
    public void manualRotateKey() {
        log.info("手动触发JWT密钥轮换");
        rotateKey();
    }

    /**
     * 验证密钥是否有效
     */
    public boolean isValidKey(String key) {
        return key != null && key.length() >= jwtConfig.getMinKeyLength();
    }

    /**
     * 获取密钥轮换历史
     */
    public java.util.List<String> getRotationHistory() {
        return redisTemplate.opsForList().range(KEY_ROTATION_LOG, 0, -1);
    }
}