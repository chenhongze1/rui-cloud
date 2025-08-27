package com.rui.common.core.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
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
import java.util.stream.IntStream;

/**
 * 链路追踪切面
 * 实现@Traced注解的自动追踪功能
 *
 * @author rui
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@ConditionalOnProperty(prefix = "rui.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracingAspect {

    private final TracingManager tracingManager;

    /**
     * 拦截@Traced注解的方法
     */
    @Around("@annotation(traced) || @within(traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        // 如果方法上没有@Traced注解，则查找类上的注解
        if (traced == null) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            traced = AnnotationUtils.findAnnotation(method, Traced.class);
            if (traced == null) {
                traced = AnnotationUtils.findAnnotation(method.getDeclaringClass(), Traced.class);
            }
        }

        if (traced == null || !TracingUtils.isTracingEnabled()) {
            return joinPoint.proceed();
        }

        String spanName = getSpanName(joinPoint, traced);
        SpanKind spanKind = traced.kind();
        
        Span span = TracingUtils.createSpan(spanName, spanKind);
        
        try (Scope scope = span.makeCurrent()) {
            // 添加基本属性
            addBasicAttributes(span, joinPoint, traced);
            
            // 记录参数
            if (traced.recordParameters()) {
                recordParameters(span, joinPoint);
            }
            
            // 执行方法
            Object result = joinPoint.proceed();
            
            // 记录返回值
            if (traced.recordReturnValue() && result != null) {
                recordReturnValue(span, result);
            }
            
            // 设置成功状态
            span.setStatus(StatusCode.OK);
            
            return result;
            
        } catch (Throwable throwable) {
            // 记录异常
            if (traced.recordException()) {
                TracingUtils.recordException(span, throwable);
            }
            throw throwable;
        } finally {
            span.end();
        }
    }

    /**
     * 获取Span名称
     */
    private String getSpanName(ProceedingJoinPoint joinPoint, Traced traced) {
        if (StringUtils.hasText(traced.value())) {
            return traced.value();
        }
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        
        return className + "." + methodName;
    }

    /**
     * 添加基本属性
     */
    private void addBasicAttributes(Span span, ProceedingJoinPoint joinPoint, Traced traced) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        
        // 添加类和方法信息
        span.setAttribute("code.namespace", signature.getDeclaringType().getName());
        span.setAttribute("code.function", signature.getName());
        
        // 添加自定义属性
        for (String attribute : traced.attributes()) {
            if (attribute.contains("=")) {
                String[] parts = attribute.split("=", 2);
                span.setAttribute(parts[0].trim(), parts[1].trim());
            }
        }
        
        // 添加业务属性
        if (StringUtils.hasText(traced.serviceName())) {
            span.setAttribute("business.service", traced.serviceName());
        }
        
        if (StringUtils.hasText(traced.operationType())) {
            span.setAttribute("business.operation", traced.operationType());
        }
    }

    /**
     * 记录方法参数
     */
    private void recordParameters(Span span, ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Parameter[] parameters = signature.getMethod().getParameters();
            Object[] args = joinPoint.getArgs();
            
            if (parameters.length > 0 && args.length > 0) {
                IntStream.range(0, Math.min(parameters.length, args.length))
                    .forEach(i -> {
                        String paramName = parameters[i].getName();
                        Object paramValue = args[i];
                        
                        if (paramValue != null) {
                            String value = sanitizeParameterValue(paramValue);
                            span.setAttribute("method.parameter." + paramName, value);
                        }
                    });
            }
        } catch (Exception e) {
            log.warn("Failed to record method parameters", e);
        }
    }

    /**
     * 记录返回值
     */
    private void recordReturnValue(Span span, Object result) {
        try {
            String value = sanitizeParameterValue(result);
            span.setAttribute("method.return_value", value);
        } catch (Exception e) {
            log.warn("Failed to record return value", e);
        }
    }

    /**
     * 清理参数值，避免记录敏感信息
     */
    private String sanitizeParameterValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        String stringValue = value.toString();
        
        // 限制长度
        if (stringValue.length() > 1000) {
            stringValue = stringValue.substring(0, 1000) + "...";
        }
        
        // 检查是否包含敏感信息
        if (containsSensitiveInfo(stringValue)) {
            return "[SENSITIVE_DATA_HIDDEN]";
        }
        
        return stringValue;
    }

    /**
     * 检查是否包含敏感信息
     */
    private boolean containsSensitiveInfo(String value) {
        if (value == null) {
            return false;
        }
        
        String lowerValue = value.toLowerCase();
        
        // 检查常见的敏感字段
        String[] sensitiveKeywords = {
            "password", "passwd", "pwd", "secret", "token", "key",
            "authorization", "auth", "credential", "private",
            "ssn", "social", "credit", "card", "phone", "mobile",
            "email", "address", "id_card", "身份证", "密码", "手机", "邮箱"
        };
        
        return Arrays.stream(sensitiveKeywords)
            .anyMatch(lowerValue::contains);
    }
}