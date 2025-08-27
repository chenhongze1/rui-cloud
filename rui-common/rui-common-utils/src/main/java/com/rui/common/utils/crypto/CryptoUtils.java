package com.rui.common.utils.crypto;

import com.rui.common.core.exception.ServiceException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 加密解密工具类
 * 
 * @author ruoyi
 */
public class CryptoUtils {
    
    static {
        // 添加BouncyCastle提供者
        Security.addProvider(new BouncyCastleProvider());
    }
    
    // AES相关常量
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_LENGTH = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    // RSA相关常量
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final int RSA_KEY_LENGTH = 2048;
    
    /**
     * 生成AES密钥
     * 
     * @return Base64编码的密钥
     */
    public static String generateAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(AES_KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("生成AES密钥失败: " + e.getMessage());
        }
    }
    
    /**
     * AES加密
     * 
     * @param plaintext 明文
     * @param key Base64编码的密钥
     * @return Base64编码的密文
     */
    public static String encryptAES(String plaintext, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            
            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // 将IV和密文合并
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, encryptedWithIv, GCM_IV_LENGTH, ciphertext.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            throw new ServiceException("AES加密失败: " + e.getMessage());
        }
    }
    
    /**
     * AES解密
     * 
     * @param ciphertext Base64编码的密文
     * @param key Base64编码的密钥
     * @return 明文
     */
    public static String decryptAES(String ciphertext, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            
            byte[] encryptedWithIv = Base64.getDecoder().decode(ciphertext);
            
            // 提取IV和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ServiceException("AES解密失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成RSA密钥对
     * 
     * @return 密钥对（公钥和私钥的Base64编码）
     */
    public static KeyPairResult generateRSAKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyPairGenerator.initialize(RSA_KEY_LENGTH);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            
            return new KeyPairResult(publicKey, privateKey);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("生成RSA密钥对失败: " + e.getMessage());
        }
    }
    
    /**
     * RSA公钥加密
     * 
     * @param plaintext 明文
     * @param publicKey Base64编码的公钥
     * @return Base64编码的密文
     */
    public static String encryptRSA(String plaintext, String publicKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey pubKey = keyFactory.generatePublic(keySpec);
            
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new ServiceException("RSA加密失败: " + e.getMessage());
        }
    }
    
    /**
     * RSA私钥解密
     * 
     * @param ciphertext Base64编码的密文
     * @param privateKey Base64编码的私钥
     * @return 明文
     */
    public static String decryptRSA(String ciphertext, String privateKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey privKey = keyFactory.generatePrivate(keySpec);
            
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privKey);
            
            byte[] encrypted = Base64.getDecoder().decode(ciphertext);
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ServiceException("RSA解密失败: " + e.getMessage());
        }
    }
    
    /**
     * MD5哈希
     * 
     * @param input 输入字符串
     * @return MD5哈希值（十六进制）
     */
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("MD5哈希失败: " + e.getMessage());
        }
    }
    
    /**
     * SHA-256哈希
     * 
     * @param input 输入字符串
     * @return SHA-256哈希值（十六进制）
     */
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("SHA-256哈希失败: " + e.getMessage());
        }
    }
    
    /**
     * SHA-512哈希
     * 
     * @param input 输入字符串
     * @return SHA-512哈希值（十六进制）
     */
    public static String sha512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("SHA-512哈希失败: " + e.getMessage());
        }
    }
    
    /**
     * HMAC-SHA256
     * 
     * @param data 数据
     * @param key 密钥
     * @return HMAC值（十六进制）
     */
    public static String hmacSha256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new ServiceException("HMAC-SHA256失败: " + e.getMessage());
        }
    }
    
    /**
     * Base64编码
     * 
     * @param input 输入字符串
     * @return Base64编码结果
     */
    public static String base64Encode(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Base64解码
     * 
     * @param input Base64编码的字符串
     * @return 解码结果
     */
    public static String base64Decode(String input) {
        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Base64解码失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成随机盐值
     * 
     * @param length 盐值长度
     * @return Base64编码的盐值
     */
    public static String generateSalt(int length) {
        try {
            byte[] salt = new byte[length];
            SecureRandom.getInstanceStrong().nextBytes(salt);
            return Base64.getEncoder().encodeToString(salt);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("生成盐值失败: " + e.getMessage());
        }
    }
    
    /**
     * 带盐值的密码哈希
     * 
     * @param password 密码
     * @param salt 盐值
     * @return 哈希结果
     */
    public static String hashPassword(String password, String salt) {
        return sha256(password + salt);
    }
    
    /**
     * 验证密码
     * 
     * @param password 输入的密码
     * @param salt 盐值
     * @param hashedPassword 存储的哈希密码
     * @return 是否匹配
     */
    public static boolean verifyPassword(String password, String salt, String hashedPassword) {
        String computedHash = hashPassword(password, salt);
        return computedHash.equals(hashedPassword);
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * 密钥对结果类
     */
    public static class KeyPairResult {
        private final String publicKey;
        private final String privateKey;
        
        public KeyPairResult(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
        
        public String getPublicKey() {
            return publicKey;
        }
        
        public String getPrivateKey() {
            return privateKey;
        }
        
        @Override
        public String toString() {
            return String.format("KeyPairResult{publicKey='%s...', privateKey='%s...'}", 
                               publicKey.substring(0, Math.min(20, publicKey.length())),
                               privateKey.substring(0, Math.min(20, privateKey.length())));
        }
    }
}