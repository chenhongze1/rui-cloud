package com.rui.common.config.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 配置加密工具类
 *
 * @author rui
 * @since 1.0.0
 */
public class ConfigEncryption {

    private static final Logger logger = LoggerFactory.getLogger(ConfigEncryption.class);

    /**
     * 默认加密算法
     */
    private static final String DEFAULT_ALGORITHM = "AES";

    /**
     * 默认转换模式
     */
    private static final String DEFAULT_TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * 加密前缀
     */
    private static final String ENCRYPTED_PREFIX = "ENC(";

    /**
     * 加密后缀
     */
    private static final String ENCRYPTED_SUFFIX = ")";

    private final String algorithm;
    private final String transformation;
    private final SecretKey secretKey;

    /**
     * 构造函数
     *
     * @param secretKeyString 密钥字符串
     */
    public ConfigEncryption(String secretKeyString) {
        this(DEFAULT_ALGORITHM, DEFAULT_TRANSFORMATION, secretKeyString);
    }

    /**
     * 构造函数
     *
     * @param algorithm       加密算法
     * @param transformation  转换模式
     * @param secretKeyString 密钥字符串
     */
    public ConfigEncryption(String algorithm, String transformation, String secretKeyString) {
        this.algorithm = algorithm;
        this.transformation = transformation;
        this.secretKey = createSecretKey(algorithm, secretKeyString);
    }

    /**
     * 加密配置值
     *
     * @param plainText 明文
     * @return 加密后的文本
     */
    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);
            return ENCRYPTED_PREFIX + encryptedText + ENCRYPTED_SUFFIX;
        } catch (Exception e) {
            logger.error("Failed to encrypt config value", e);
            throw new ConfigEncryptionException("Failed to encrypt config value", e);
        }
    }

    /**
     * 解密配置值
     *
     * @param encryptedText 加密文本
     * @return 解密后的明文
     */
    public String decrypt(String encryptedText) {
        if (!isEncrypted(encryptedText)) {
            return encryptedText;
        }

        try {
            String actualEncryptedText = extractEncryptedValue(encryptedText);
            byte[] encryptedBytes = Base64.getDecoder().decode(actualEncryptedText);
            
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Failed to decrypt config value: {}", encryptedText, e);
            throw new ConfigEncryptionException("Failed to decrypt config value", e);
        }
    }

    /**
     * 判断是否为加密值
     *
     * @param value 配置值
     * @return true-加密值，false-明文值
     */
    public boolean isEncrypted(String value) {
        return StringUtils.hasText(value) && 
               value.startsWith(ENCRYPTED_PREFIX) && 
               value.endsWith(ENCRYPTED_SUFFIX);
    }

    /**
     * 提取加密值
     *
     * @param encryptedText 完整的加密文本
     * @return 纯加密值
     */
    private String extractEncryptedValue(String encryptedText) {
        return encryptedText.substring(
            ENCRYPTED_PREFIX.length(), 
            encryptedText.length() - ENCRYPTED_SUFFIX.length()
        );
    }

    /**
     * 创建密钥
     *
     * @param algorithm       加密算法
     * @param secretKeyString 密钥字符串
     * @return 密钥对象
     */
    private SecretKey createSecretKey(String algorithm, String secretKeyString) {
        try {
            if (!StringUtils.hasText(secretKeyString)) {
                throw new IllegalArgumentException("Secret key cannot be empty");
            }

            // 如果密钥长度不足，进行填充或截取
            byte[] keyBytes = adjustKeyLength(secretKeyString.getBytes(StandardCharsets.UTF_8), algorithm);
            return new SecretKeySpec(keyBytes, algorithm);
        } catch (Exception e) {
            logger.error("Failed to create secret key", e);
            throw new ConfigEncryptionException("Failed to create secret key", e);
        }
    }

    /**
     * 调整密钥长度
     *
     * @param keyBytes  原始密钥字节
     * @param algorithm 加密算法
     * @return 调整后的密钥字节
     */
    private byte[] adjustKeyLength(byte[] keyBytes, String algorithm) {
        int requiredLength = getRequiredKeyLength(algorithm);
        
        if (keyBytes.length == requiredLength) {
            return keyBytes;
        }
        
        byte[] adjustedKey = new byte[requiredLength];
        if (keyBytes.length > requiredLength) {
            // 截取
            System.arraycopy(keyBytes, 0, adjustedKey, 0, requiredLength);
        } else {
            // 填充
            System.arraycopy(keyBytes, 0, adjustedKey, 0, keyBytes.length);
            for (int i = keyBytes.length; i < requiredLength; i++) {
                adjustedKey[i] = 0;
            }
        }
        
        return adjustedKey;
    }

    /**
     * 获取算法所需的密钥长度
     *
     * @param algorithm 加密算法
     * @return 密钥长度
     */
    private int getRequiredKeyLength(String algorithm) {
        switch (algorithm.toUpperCase()) {
            case "AES":
                return 16; // 128 bits
            case "DES":
                return 8;  // 64 bits
            case "DESEDE":
            case "3DES":
                return 24; // 192 bits
            default:
                return 16; // 默认16字节
        }
    }

    /**
     * 生成随机密钥
     *
     * @param algorithm 加密算法
     * @return Base64编码的密钥字符串
     */
    public static String generateRandomKey(String algorithm) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
            keyGenerator.init(new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new ConfigEncryptionException("Failed to generate random key", e);
        }
    }

    /**
     * 生成AES随机密钥
     *
     * @return Base64编码的AES密钥字符串
     */
    public static String generateAESKey() {
        return generateRandomKey("AES");
    }
}