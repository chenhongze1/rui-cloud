package com.rui.common.log.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rui.common.log.annotation.Logged;
import com.rui.common.log.manager.LogManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 日志切面
 * 实现@Logged注解的自动日志记录功能
 *
 * @author rui
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@ConditionalOnProperty(prefix = "rui.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAspect {

    private final LogManager logManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 敏感参数名称
    private static final String[] SENSITIVE_PARAM_NAMES = {
        "password", "passwd", "pwd", "secret", "token", "key",
        "authorization", "auth", "credential", "private",
        "ssn", "social", "credit", "card", "phone", "mobile",
        "email", "address", "id_card", "身份证", "密码", "手机", "邮箱"
    };

    /**
     * 拦截@Logged注解的方法
     */
    @Around("@annotation(logged) || @within(logged)")
    public Object logMethod(ProceedingJoinPoint joinPoint, Logged logged) throws Throwable {
        // 如果方法上没有@Logged注解，则查找类上的注解
        if (logged == null) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            logged = AnnotationUtils.findAnnotation(method, Logged.class);
            if (logged == null) {
                logged = AnnotationUtils.findAnnotation(method.getDeclaringClass(), Logged.class);
            }
        }

        if (logged == null) {
            return joinPoint.proceed();
        }

        String methodName = getMethodName(joinPoint);
        String operation = StringUtils.hasText(logged.operation()) ? logged.operation() : methodName;
        String module = StringUtils.hasText(logged.module()) ? logged.module() : getClassName(joinPoint);
        
        // 记录方法开始
        long startTime = System.currentTimeMillis();
        logMethodStart(joinPoint, logged, operation, module);
        
        Object result = null;
        Throwable exception = null;
        
        try {
            // 执行方法
            result = joinPoint.proceed();
            return result;
            
        } catch (Throwable throwable) {
            exception = throwable;
            throw throwable;
            
        } finally {
            // 计算执行时间
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录方法完成
            logMethodCompletion(joinPoint, logged, operation, module, duration, result, exception);
        }
    }

    /**
     * 记录方法开始日志
     */
    private void logMethodStart(ProceedingJoinPoint joinPoint, Logged logged, String operation, String module) {
        if (logged.level() == Logged.LogLevel.TRACE || logged.level() == Logged.LogLevel.DEBUG) {
            Map<String, Object> logData = createBaseLogData(joinPoint, logged, operation, module);
            
            // 记录参数
            if (logged.logParameters()) {
                logData.put("parameters", getMethodParameters(joinPoint, logged.includeSensitive()));
            }
            
            logData.put("event", "method_start");
            
            writeLog(logged, "Method started: " + operation, logData);
        }
    }

    /**
     * 记录方法完成日志
     */
    private void logMethodCompletion(ProceedingJoinPoint joinPoint, Logged logged, String operation, 
                                   String module, long duration, Object result, Throwable exception) {
        
        Map<String, Object> logData = createBaseLogData(joinPoint, logged, operation, module);
        logData.put("duration", duration);
        logData.put("event", "method_completed");
        
        // 记录返回值
        if (logged.logReturnValue() && result != null && exception == null) {
            logData.put("returnValue", sanitizeData(result, logged.includeSensitive()));
        }
        
        // 记录异常
        if (exception != null && logged.logException()) {
            logData.put("exception", exception.getClass().getSimpleName());
            logData.put("exceptionMessage", exception.getMessage());
        }
        
        // 判断是否为慢操作
        boolean isSlow = duration > logged.slowThreshold();
        if (isSlow) {
            logData.put("slow", true);
        }
        
        // 根据日志类型记录不同的日志
        switch (logged.type()) {
            case BUSINESS:
                logManager.business(module, operation, 
                    exception != null ? "FAILED" : "SUCCESS", logData);
                break;
                
            case AUDIT:
                logManager.audit(operation, module, 
                    exception != null ? "FAILED" : "SUCCESS", logData);
                break;
                
            case PERFORMANCE:
                // 性能监控功能已迁移到 rui-common-monitoring 模块
                // 建议使用 @PerformanceMonitored 注解替代 @Logged(type = LogType.PERFORMANCE)
                log.warn("@Logged(type = LogType.PERFORMANCE) 已废弃，请使用 rui-common-monitoring 模块的 @PerformanceMonitored 注解");
                break;
                
            case SECURITY:
                String level = exception != null ? "HIGH" : "INFO";
                logManager.security(operation, level, 
                    "Method execution: " + (exception != null ? "failed" : "completed"), logData);
                break;
                
            case GENERAL:
            default:
                String message = String.format("Method %s %s in %dms", 
                    operation, exception != null ? "failed" : "completed", duration);
                
                if (exception != null) {
                    logManager.error(message, exception, logData);
                } else if (isSlow) {
                    writeLog(Logged.LogLevel.WARN, message, logData);
                } else {
                    writeLog(logged.level(), message, logData);
                }
                break;
        }
    }

    /**
     * 创建基础日志数据
     */
    private Map<String, Object> createBaseLogData(ProceedingJoinPoint joinPoint, Logged logged, 
                                                String operation, String module) {
        Map<String, Object> logData = new HashMap<>();
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        logData.put("className", signature.getDeclaringType().getName());
        logData.put("methodName", signature.getName());
        logData.put("operation", operation);
        logData.put("module", module);
        
        // 添加自定义属性
        for (String attribute : logged.attributes()) {
            if (attribute.contains("=")) {
                String[] parts = attribute.split("=", 2);
                logData.put(parts[0].trim(), parts[1].trim());
            }
        }
        
        return logData;
    }

    /**
     * 获取方法参数
     */
    private Map<String, Object> getMethodParameters(ProceedingJoinPoint joinPoint, boolean includeSensitive) {
        Map<String, Object> parameters = new HashMap<>();
        
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Parameter[] methodParams = signature.getMethod().getParameters();
            Object[] args = joinPoint.getArgs();
            
            if (methodParams.length > 0 && args.length > 0) {
                IntStream.range(0, Math.min(methodParams.length, args.length))
                    .forEach(i -> {
                        String paramName = methodParams[i].getName();
                        Object paramValue = args[i];
                        
                        if (paramValue != null) {
                            if (includeSensitive || !isSensitiveParameter(paramName)) {
                                parameters.put(paramName, sanitizeData(paramValue, includeSensitive));
                            } else {
                                parameters.put(paramName, "[SENSITIVE_DATA_HIDDEN]");
                            }
                        } else {
                            parameters.put(paramName, null);
                        }
                    });
            }
        } catch (Exception e) {
            log.warn("Failed to extract method parameters", e);
            parameters.put("error", "Failed to extract parameters: " + e.getMessage());
        }
        
        return parameters;
    }

    /**
     * 检查是否为敏感参数
     */
    private boolean isSensitiveParameter(String paramName) {
        if (paramName == null) {
            return false;
        }
        
        String lowerName = paramName.toLowerCase();
        return Arrays.stream(SENSITIVE_PARAM_NAMES)
            .anyMatch(lowerName::contains);
    }

    /**
     * 清理数据中的敏感信息
     */
    private Object sanitizeData(Object data, boolean includeSensitive) {
        if (data == null) {
            return null;
        }
        
        if (includeSensitive) {
            return limitDataSize(data);
        }
        
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            
            // 限制长度
            if (jsonString.length() > 2000) {
                jsonString = jsonString.substring(0, 2000) + "...";
            }
            
            // 替换敏感字段
            for (String sensitiveField : SENSITIVE_PARAM_NAMES) {
                jsonString = jsonString.replaceAll(
                    "(\"" + sensitiveField + "\"\s*:\s*\")[^\"]*(\")?",
                    "$1[HIDDEN]$2"
                );
            }
            
            return objectMapper.readValue(jsonString, Object.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to sanitize data", e);
            return "[DATA_SANITIZATION_FAILED]";
        }
    }

    /**
     * 限制数据大小
     */
    private Object limitDataSize(Object data) {
        if (data == null) {
            return null;
        }
        
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            if (jsonString.length() > 2000) {
                return jsonString.substring(0, 2000) + "...";
            }
            return data;
        } catch (JsonProcessingException e) {
            return data.toString();
        }
    }

    /**
     * 获取方法名
     */
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getName();
    }

    /**
     * 获取类名
     */
    private String getClassName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName();
    }

    /**
     * 写入日志
     */
    private void writeLog(Logged logged, String message, Map<String, Object> logData) {
        writeLog(logged.level(), message, logData);
    }

    /**
     * 根据级别写入日志
     */
    private void writeLog(Logged.LogLevel level, String message, Map<String, Object> logData) {
        String logMessage = message;
        if (logData != null && !logData.isEmpty()) {
            try {
                logMessage += " - " + objectMapper.writeValueAsString(logData);
            } catch (JsonProcessingException e) {
                logMessage += " - " + logData.toString();
            }
        }
        
        switch (level) {
            case TRACE:
                log.trace(logMessage);
                break;
            case DEBUG:
                log.debug(logMessage);
                break;
            case INFO:
                log.info(logMessage);
                break;
            case WARN:
                log.warn(logMessage);
                break;
            case ERROR:
                log.error(logMessage);
                break;
            default:
                log.info(logMessage);
                break;
        }
    }
}