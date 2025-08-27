package com.rui.common.web.exception;

import com.rui.common.core.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 安全异常处理器
 * 负责异常信息的脱敏处理，避免敏感信息泄露
 *
 * @author rui
 */
@Slf4j
@Component
public class SecureExceptionHandler {

    @Value("${security.exception.mask-sensitive-info:true}")
    private boolean maskSensitiveInfo;

    @Value("${security.exception.show-stack-trace:false}")
    private boolean showStackTrace;

    @Value("${security.exception.max-message-length:200}")
    private int maxMessageLength;

    // 敏感信息正则表达式
    private static final Pattern[] SENSITIVE_PATTERNS = {
            Pattern.compile("password[\s]*[=:][\s]*[\w]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("token[\s]*[=:][\s]*[\w\-\.]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("secret[\s]*[=:][\s]*[\w]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("key[\s]*[=:][\s]*[\w\-]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("authorization[\s]*[=:][\s]*[\w\s\-\.]+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\b\d{15,19}\b"), // 银行卡号
            Pattern.compile("\b\d{17}[\dXx]\b"), // 身份证号
            Pattern.compile("\b1[3-9]\d{9}\b"), // 手机号
            Pattern.compile("[\w\.-]+@[\w\.-]+\.[a-zA-Z]{2,}"), // 邮箱
            Pattern.compile("\b(?:\d{1,3}\.){3}\d{1,3}\b") // IP地址
    };

    /**
     * 处理异常信息，进行安全脱敏
     *
     * @param exception 原始异常
     * @param errorCode 错误码
     * @return 脱敏后的异常信息
     */
    public String handleExceptionMessage(Throwable exception, ErrorCode errorCode) {
        String originalMessage = exception.getMessage();
        
        // 如果是系统错误或安全错误，使用预定义的安全消息
        if (ErrorCode.isSystemError(errorCode.getCode()) || ErrorCode.isSecurityError(errorCode.getCode())) {
            return errorCode.getMessage();
        }
        
        // 对业务异常进行脱敏处理
        if (originalMessage == null) {
            return errorCode.getMessage();
        }
        
        String safeMessage = originalMessage;
        
        // 脱敏处理
        if (maskSensitiveInfo) {
            safeMessage = maskSensitiveInformation(safeMessage);
        }
        
        // 限制消息长度
        if (safeMessage.length() > maxMessageLength) {
            safeMessage = safeMessage.substring(0, maxMessageLength) + "...";
        }
        
        return safeMessage;
    }

    /**
     * 脱敏敏感信息
     *
     * @param message 原始消息
     * @return 脱敏后的消息
     */
    private String maskSensitiveInformation(String message) {
        String maskedMessage = message;
        
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            maskedMessage = pattern.matcher(maskedMessage).replaceAll("***");
        }
        
        return maskedMessage;
    }

    /**
     * 生成安全的错误ID
     *
     * @return 错误ID
     */
    public String generateErrorId() {
        return "ERR-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int) (Math.random() * 0xFFFF));
    }

    /**
     * 记录详细的异常信息（仅用于日志）
     *
     * @param exception 异常
     * @param errorId 错误ID
     * @param requestUri 请求URI
     */
    public void logDetailedException(Throwable exception, String errorId, String requestUri) {
        if (showStackTrace) {
            log.error("[{}] 请求地址: {}, 详细异常信息:", errorId, requestUri, exception);
        } else {
            log.error("[{}] 请求地址: {}, 异常类型: {}, 异常消息: {}", 
                     errorId, requestUri, exception.getClass().getSimpleName(), exception.getMessage());
        }
    }

    /**
     * 判断是否应该暴露详细错误信息
     *
     * @param errorCode 错误码
     * @return 是否暴露详细信息
     */
    public boolean shouldExposeDetailedError(ErrorCode errorCode) {
        // 只有业务错误才暴露详细信息
        return ErrorCode.isBusinessError(errorCode.getCode());
    }

    /**
     * 获取用户友好的错误消息
     *
     * @param errorCode 错误码
     * @param originalMessage 原始消息
     * @return 用户友好的消息
     */
    public String getUserFriendlyMessage(ErrorCode errorCode, String originalMessage) {
        // 对于系统错误和安全错误，返回通用消息
        if (ErrorCode.isSystemError(errorCode.getCode())) {
            return "系统繁忙，请稍后重试";
        }
        
        if (ErrorCode.isSecurityError(errorCode.getCode())) {
            return "访问受限，请检查权限";
        }
        
        // 对于业务错误，返回脱敏后的原始消息
        return maskSensitiveInfo ? maskSensitiveInformation(originalMessage) : originalMessage;
    }
}