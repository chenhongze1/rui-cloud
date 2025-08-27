package com.rui.common.config.encryption;

/**
 * 配置加密异常
 *
 * @author rui
 * @since 1.0.0
 */
public class ConfigEncryptionException extends RuntimeException {

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public ConfigEncryptionException(String message) {
        super(message);
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     * @param cause   原因
     */
    public ConfigEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     *
     * @param cause 原因
     */
    public ConfigEncryptionException(Throwable cause) {
        super(cause);
    }
}