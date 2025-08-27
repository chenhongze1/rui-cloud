package com.rui.common.web.exception;

import com.rui.common.core.constant.HttpStatus;
import com.rui.common.core.domain.R;
import com.rui.common.core.enums.ErrorCode;
import com.rui.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author rui
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SecureExceptionHandler secureHandler;

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = e.getCode() != null ? ErrorCode.getByCode(e.getCode()) : ErrorCode.BUSINESS_ERROR;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        String safeMessage = secureHandler.handleExceptionMessage(e, errorCode);
        return R.fail(errorCode.getCode(), safeMessage).put("errorId", errorId);
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                       HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.REQUEST_METHOD_NOT_SUPPORTED;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        return R.fail(errorCode.getCode(), errorCode.getMessage()).put("errorId", errorId);
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public R<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.SYSTEM_ERROR;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        return R.fail(errorCode.getCode(), errorCode.getMessage()).put("errorId", errorId);
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        return R.fail(errorCode.getCode(), errorCode.getMessage()).put("errorId", errorId);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.PARAM_INVALID;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        String message = e.getAllErrors().get(0).getDefaultMessage();
        String safeMessage = secureHandler.getUserFriendlyMessage(errorCode, message);
        
        return R.fail(errorCode.getCode(), safeMessage).put("errorId", errorId);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.PARAM_INVALID;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        String safeMessage = secureHandler.getUserFriendlyMessage(errorCode, message);
        
        return R.fail(errorCode.getCode(), safeMessage).put("errorId", errorId);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> constraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.PARAM_INVALID;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        String safeMessage = secureHandler.getUserFriendlyMessage(errorCode, message);
        
        return R.fail(errorCode.getCode(), safeMessage).put("errorId", errorId);
    }

    /**
     * 认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public R<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        return R.fail(errorCode.getCode(), errorCode.getMessage()).put("errorId", errorId);
    }

    /**
     * 权限不足异常
     */
    @ExceptionHandler({org.springframework.security.access.AccessDeniedException.class, java.nio.file.AccessDeniedException.class})
    public R<Void> handleAccessDeniedException(Exception e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        return R.fail(errorCode.getCode(), errorCode.getMessage()).put("errorId", errorId);
    }

    /**
     * 数据库异常
     */
    @ExceptionHandler({SQLException.class, DataAccessException.class})
    public R<Void> handleDatabaseException(Exception e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.DATABASE_ERROR;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        return R.fail(errorCode.getCode(), errorCode.getMessage()).put("errorId", errorId);
    }

    /**
     * 重复键异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.DUPLICATE_KEY_ERROR;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        return R.fail(errorCode.getCode(), errorCode.getMessage()).put("errorId", errorId);
    }

    /**
     * 超时异常
     */
    @ExceptionHandler(TimeoutException.class)
    public R<Void> handleTimeoutException(TimeoutException e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.REQUEST_TIMEOUT;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        return R.fail(errorCode.getCode(), errorCode.getMessage()).put("errorId", errorId);
    }

    /**
     * 404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public R<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        String errorId = secureHandler.generateErrorId();
        ErrorCode errorCode = ErrorCode.DATA_NOT_FOUND;
        
        secureHandler.logDetailedException(e, errorId, request.getRequestURI());
        
        return R.fail(errorCode.getCode(), "请求的资源不存在").put("errorId", errorId);
    }
}