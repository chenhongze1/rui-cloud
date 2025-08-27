package com.rui.common.core.exception;

import com.rui.common.core.enums.ErrorCode;

/**
 * 业务异常
 *
 * @author rui
 */
public final class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误提示
     */
    private String message;

    /**
     * 错误明细，内部调试错误
     */
    private String detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public ServiceException() {
    }

    public ServiceException(String message) {
        this.message = message;
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
    }

    public ServiceException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    public ServiceException(ErrorCode errorCode) {
        this.message = errorCode.getMessage();
        this.code = errorCode.getCode();
    }

    public ServiceException(ErrorCode errorCode, String customMessage) {
        this.message = customMessage;
        this.code = errorCode.getCode();
    }

    public ServiceException(ErrorCode errorCode, String customMessage, String detailMessage) {
        this.message = customMessage;
        this.code = errorCode.getCode();
        this.detailMessage = detailMessage;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Integer getCode() {
        return code;
    }

    public ServiceException setMessage(String message) {
        this.message = message;
        return this;
    }

    public ServiceException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码枚举
     * @return 业务异常
     */
    public static ServiceException of(ErrorCode errorCode) {
        return new ServiceException(errorCode);
    }

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码枚举
     * @param customMessage 自定义消息
     * @return 业务异常
     */
    public static ServiceException of(ErrorCode errorCode, String customMessage) {
        return new ServiceException(errorCode, customMessage);
    }

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码枚举
     * @param customMessage 自定义消息
     * @param detailMessage 详细消息
     * @return 业务异常
     */
    public static ServiceException of(ErrorCode errorCode, String customMessage, String detailMessage) {
        return new ServiceException(errorCode, customMessage, detailMessage);
    }

    /**
     * 抛出业务异常
     *
     * @param errorCode 错误码枚举
     */
    public static void throwException(ErrorCode errorCode) {
        throw new ServiceException(errorCode);
    }

    /**
     * 抛出业务异常
     *
     * @param errorCode 错误码枚举
     * @param customMessage 自定义消息
     */
    public static void throwException(ErrorCode errorCode, String customMessage) {
        throw new ServiceException(errorCode, customMessage);
    }
}