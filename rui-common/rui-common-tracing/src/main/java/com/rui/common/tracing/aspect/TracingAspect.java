package com.rui.common.tracing.aspect;

import com.rui.common.tracing.annotation.Traced;
import com.rui.common.tracing.manager.TracingManager;
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
//@Component  // 暂时禁用以隔离问题
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
        if (traced == null) {
            // 如果方法上没有注解，检查类上的注解
            traced = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), Traced.class);
        }
        
        if (traced == null) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String spanName = getSpanName(traced, signature);
        
        Span span = tracingManager.startSpan(spanName, traced.kind());
        
        try (Scope scope = span.makeCurrent()) {
            // 添加基本属性
            addBasicAttributes(span, joinPoint, traced);
            
            // 记录参数
            if (traced.recordParameters()) {
                recordParameters(span, joinPoint);
            }
            
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 记录返回值
            if (traced.recordReturnValue() && result != null) {
                span.setAttribute("return.type", result.getClass().getSimpleName());
                span.setAttribute("return.value", String.valueOf(result));
            }
            
            span.setStatus(StatusCode.OK);
            return result;
            
        } catch (Throwable throwable) {
            // 记录异常
            if (traced.recordException()) {
                span.recordException(throwable);
                span.setAttribute("error.type", throwable.getClass().getSimpleName());
                span.setAttribute("error.message", throwable.getMessage());
            }
            
            span.setStatus(StatusCode.ERROR, throwable.getMessage());
            throw throwable;
            
        } finally {
            tracingManager.finishSpan(span);
        }
    }

    /**
     * 获取Span名称
     */
    private String getSpanName(Traced traced, MethodSignature signature) {
        if (StringUtils.hasText(traced.value())) {
            return traced.value();
        }
        
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        return className + "." + methodName;
    }

    /**
     * 记录方法参数
     */
    private void recordParameters(Span span, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        
        IntStream.range(0, Math.min(parameters.length, args.length))
                .forEach(i -> {
                    String paramName = parameters[i].getName();
                    Object paramValue = args[i];
                    if (paramValue != null) {
                        span.setAttribute("param." + paramName, String.valueOf(paramValue));
                    }
                });
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
     * 获取方法的完整签名
     */
    private String getMethodSignature(Method method) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getDeclaringClass().getSimpleName())
                .append(".")
                .append(method.getName())
                .append("(");
        
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                signature.append(", ");
            }
            signature.append(parameters[i].getType().getSimpleName());
        }
        
        signature.append(")");
        return signature.toString();
    }

    /**
     * 检查是否应该跳过追踪
     */
    private boolean shouldSkipTracing(ProceedingJoinPoint joinPoint) {
        // 可以在这里添加跳过追踪的逻辑
        // 例如：某些特定的方法或类
        return false;
    }

    /**
     * 获取操作类型
     */
    private String getOperationType(MethodSignature signature) {
        String methodName = signature.getName();
        
        if (methodName.startsWith("get") || methodName.startsWith("find") || methodName.startsWith("query")) {
            return "READ";
        } else if (methodName.startsWith("save") || methodName.startsWith("create") || methodName.startsWith("insert")) {
            return "CREATE";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        }
        
        return "UNKNOWN";
    }

    /**
     * 添加性能指标
     */
    private void addPerformanceMetrics(Span span, long startTime, long endTime) {
        long duration = endTime - startTime;
        span.setAttribute("performance.duration_ms", duration);
        
        if (duration > 1000) {
            span.setAttribute("performance.slow_operation", true);
        }
    }

    /**
     * 记录业务事件
     */
    private void recordBusinessEvent(Span span, String event, Object... attributes) {
        span.addEvent(event);
        
        for (int i = 0; i < attributes.length; i += 2) {
            if (i + 1 < attributes.length) {
                span.setAttribute(String.valueOf(attributes[i]), String.valueOf(attributes[i + 1]));
            }
        }
    }
}