package com.rui.common.core.enums;

/**
 * 统一错误码枚举
 * 错误码规范：
 * - 系统级错误：1000-1999
 * - 业务级错误：2000-2999
 * - 安全相关错误：3000-3999
 * - 第三方服务错误：4000-4999
 * - 数据相关错误：5000-5999
 *
 * @author rui
 */
public enum ErrorCode {

    // ========== 系统级错误 1000-1999 ==========
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(1000, "系统异常，请稍后重试"),
    PARAM_ERROR(1001, "参数错误"),
    PARAM_MISSING(1002, "缺少必要参数"),
    PARAM_INVALID(1003, "参数格式不正确"),
    REQUEST_METHOD_NOT_SUPPORTED(1004, "请求方法不支持"),
    REQUEST_TIMEOUT(1005, "请求超时"),
    RATE_LIMIT_EXCEEDED(1006, "请求过于频繁，请稍后重试"),
    SERVICE_UNAVAILABLE(1007, "服务暂时不可用"),
    INTERNAL_SERVER_ERROR(1008, "服务器内部错误"),
    
    // ========== 业务级错误 2000-2999 ==========
    BUSINESS_ERROR(2000, "业务处理失败"),
    DATA_NOT_FOUND(2001, "数据不存在"),
    DATA_ALREADY_EXISTS(2002, "数据已存在"),
    OPERATION_NOT_ALLOWED(2003, "操作不被允许"),
    STATUS_ERROR(2004, "状态错误"),
    WORKFLOW_ERROR(2005, "流程错误"),
    
    // ========== 安全相关错误 3000-3999 ==========
    UNAUTHORIZED(3000, "未授权访问"),
    ACCESS_DENIED(3001, "访问被拒绝"),
    TOKEN_INVALID(3002, "令牌无效"),
    TOKEN_EXPIRED(3003, "令牌已过期"),
    TOKEN_BLACKLISTED(3004, "令牌已被注销"),
    LOGIN_FAILED(3005, "登录失败"),
    PASSWORD_ERROR(3006, "密码错误"),
    ACCOUNT_LOCKED(3007, "账户已被锁定"),
    ACCOUNT_DISABLED(3008, "账户已被禁用"),
    PERMISSION_DENIED(3009, "权限不足"),
    CAPTCHA_ERROR(3010, "验证码错误"),
    SIGNATURE_ERROR(3011, "签名验证失败"),
    
    // ========== 第三方服务错误 4000-4999 ==========
    THIRD_PARTY_ERROR(4000, "第三方服务异常"),
    REDIS_ERROR(4001, "缓存服务异常"),
    DATABASE_ERROR(4002, "数据库服务异常"),
    MQ_ERROR(4003, "消息队列服务异常"),
    FILE_UPLOAD_ERROR(4004, "文件上传失败"),
    FILE_DOWNLOAD_ERROR(4005, "文件下载失败"),
    EMAIL_SEND_ERROR(4006, "邮件发送失败"),
    SMS_SEND_ERROR(4007, "短信发送失败"),
    
    // ========== 数据相关错误 5000-5999 ==========
    DATA_ERROR(5000, "数据异常"),
    DATA_VALIDATION_ERROR(5001, "数据验证失败"),
    DATA_CONVERSION_ERROR(5002, "数据转换失败"),
    DATA_INTEGRITY_ERROR(5003, "数据完整性错误"),
    DUPLICATE_KEY_ERROR(5004, "数据重复"),
    FOREIGN_KEY_ERROR(5005, "外键约束错误"),
    TRANSACTION_ERROR(5006, "事务处理失败"),
    LOCK_TIMEOUT_ERROR(5007, "获取锁超时"),
    IDEMPOTENT_ERROR(5008, "重复请求");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据错误码获取枚举
     *
     * @param code 错误码
     * @return 错误码枚举
     */
    public static ErrorCode getByCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }

    /**
     * 判断是否为成功状态
     *
     * @param code 错误码
     * @return 是否成功
     */
    public static boolean isSuccess(int code) {
        return SUCCESS.getCode() == code;
    }

    /**
     * 判断是否为系统错误
     *
     * @param code 错误码
     * @return 是否为系统错误
     */
    public static boolean isSystemError(int code) {
        return code >= 1000 && code < 2000;
    }

    /**
     * 判断是否为业务错误
     *
     * @param code 错误码
     * @return 是否为业务错误
     */
    public static boolean isBusinessError(int code) {
        return code >= 2000 && code < 3000;
    }

    /**
     * 判断是否为安全错误
     *
     * @param code 错误码
     * @return 是否为安全错误
     */
    public static boolean isSecurityError(int code) {
        return code >= 3000 && code < 4000;
    }
}