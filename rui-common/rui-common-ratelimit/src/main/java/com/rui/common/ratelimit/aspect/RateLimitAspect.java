package com.rui.common.ratelimit.aspect;

import com.rui.common.core.exception.ServiceException;
import com.rui.common.core.utils.ServletUtils;
import com.rui.common.core.utils.StringUtils;
import com.rui.common.ratelimit.annotation.RateLimit;
import com.rui.common.ratelimit.enums.LimitType;
import com.rui.common.ratelimit.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 限流切面
 * 
 * @author rui
 */
@Slf4j
@Aspect
@Order(1)
//@Component  // 暂时禁用以隔离问题
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final RateLimitService rateLimitService;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) {
        // 构建限流key
        String key = buildKey(point, rateLimit);
        
        // 尝试获取令牌
        boolean allowed = rateLimitService.tryAcquire(key, rateLimit.count(), 
                rateLimit.time(), rateLimit.timeUnit());
        
        if (!allowed) {
            log.warn("限流触发: key={}, message={}", key, rateLimit.message());
            
            if (rateLimit.throwException()) {
                throw new ServiceException(rateLimit.message());
            }
        }
    }
    
    /**
     * 构建限流key
     */
    private String buildKey(JoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 根据限流类型构建key
        switch (rateLimit.limitType()) {
            case IP:
                keyBuilder.append(getClientIP()).append(":");
                break;
            case USER:
                keyBuilder.append(getCurrentUserId()).append(":");
                break;
            case METHOD:
                keyBuilder.append(getMethodName(point)).append(":");
                break;
            case PARAM:
                keyBuilder.append(getMethodParams(point)).append(":");
                break;
            case CUSTOM:
                // 自定义key，支持SpEL表达式
                if (StringUtils.isNotBlank(rateLimit.key())) {
                    keyBuilder.append(parseSpEL(point, rateLimit.key())).append(":");
                }
                break;
            case DEFAULT:
            default:
                // 默认使用方法名
                keyBuilder.append(getMethodName(point)).append(":");
                break;
        }
        
        // 添加自定义key
        if (StringUtils.isNotBlank(rateLimit.key()) && rateLimit.limitType() != LimitType.CUSTOM) {
            keyBuilder.append(parseSpEL(point, rateLimit.key()));
        } else {
            keyBuilder.append("default");
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIP() {
        try {
            return ServletUtils.getClientIP();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            // 这里可以从SecurityContext或其他地方获取用户ID
            // 暂时返回固定值，实际项目中需要根据具体情况实现
            return "anonymous";
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 获取方法名
     */
    private String getMethodName(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
    
    /**
     * 获取方法参数
     */
    private String getMethodParams(JoinPoint point) {
        Object[] args = point.getArgs();
        return Arrays.toString(args);
    }
    
    /**
     * 解析SpEL表达式
     */
    private String parseSpEL(JoinPoint point, String spel) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            Object[] args = point.getArgs();
            String[] paramNames = signature.getParameterNames();
            
            EvaluationContext context = new StandardEvaluationContext();
            
            // 设置方法参数
            if (paramNames != null && args != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            
            // 设置方法信息
            context.setVariable("method", method);
            context.setVariable("target", point.getTarget());
            
            Expression expression = parser.parseExpression(spel);
            Object value = expression.getValue(context);
            
            return value != null ? value.toString() : "";
            
        } catch (Exception e) {
            log.warn("解析SpEL表达式失败: {}", spel, e);
            return spel;
        }
    }
}