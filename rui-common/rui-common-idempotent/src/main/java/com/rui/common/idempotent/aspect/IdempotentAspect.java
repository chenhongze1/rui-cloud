package com.rui.common.idempotent.aspect;

import com.rui.common.core.exception.ServiceException;
import com.rui.common.core.utils.ServletUtils;
import com.rui.common.core.utils.StringUtils;
import com.rui.common.idempotent.annotation.Idempotent;
import com.rui.common.idempotent.enums.IdempotentType;
import com.rui.common.idempotent.service.IdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 幂等性切面
 * 
 * @author rui
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {
    
    private final IdempotentService idempotentService;
    
    private final ExpressionParser parser = new SpelExpressionParser();
    
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = buildIdempotentKey(joinPoint, idempotent);
        
        // 检查是否重复请求
        if (idempotentService.isDuplicate(key, idempotent.expireTime(), idempotent.timeUnit())) {
            log.warn("检测到重复请求, key: {}", key);
            throw new ServiceException(idempotent.message());
        }
        
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 如果配置了删除key，则在执行完成后删除
            if (idempotent.delKey()) {
                idempotentService.deleteIdempotent(key);
            }
            
            return result;
        } catch (Exception e) {
            // 如果执行失败且配置了删除key，则删除key以允许重试
            if (idempotent.delKey()) {
                idempotentService.deleteIdempotent(key);
            }
            throw e;
        }
    }
    
    /**
     * 构建幂等性key
     */
    private String buildIdempotentKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        
        StringBuilder keyBuilder = new StringBuilder("idempotent:");
        keyBuilder.append(className).append(":").append(methodName).append(":");
        
        String keyPart = "";
        switch (idempotent.type()) {
            case SPEL:
                keyPart = parseSpEL(idempotent.key(), joinPoint);
                break;
            case HEADER:
                keyPart = getHeaderValue(idempotent.key());
                break;
            case PARAM:
                keyPart = getParameterValue(idempotent.key());
                break;
            case METHOD_PARAM:
                keyPart = getMethodParamValue(joinPoint);
                break;
            case USER_ID:
                keyPart = getCurrentUserId();
                break;
            case IP:
                keyPart = getClientIP();
                break;
            case TOKEN:
                keyPart = getTokenValue();
                break;
            default:
                keyPart = getMethodParamValue(joinPoint);
        }
        
        keyBuilder.append(DigestUtils.md5DigestAsHex(keyPart.getBytes(StandardCharsets.UTF_8)));
        return keyBuilder.toString();
    }
    
    /**
     * 解析SpEL表达式
     */
    private String parseSpEL(String spel, ProceedingJoinPoint joinPoint) {
        if (StringUtils.isBlank(spel)) {
            return getMethodParamValue(joinPoint);
        }
        
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            
            Expression expression = parser.parseExpression(spel);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.warn("解析SpEL表达式失败: {}", spel, e);
            return getMethodParamValue(joinPoint);
        }
    }
    
    /**
     * 获取请求头值
     */
    private String getHeaderValue(String headerName) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null && StringUtils.isNotBlank(headerName)) {
            return request.getHeader(headerName);
        }
        return "";
    }
    
    /**
     * 获取请求参数值
     */
    private String getParameterValue(String paramName) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null && StringUtils.isNotBlank(paramName)) {
            return request.getParameter(paramName);
        }
        return "";
    }
    
    /**
     * 获取方法参数值
     */
    private String getMethodParamValue(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            return Arrays.stream(args)
                    .map(arg -> arg != null ? arg.toString() : "null")
                    .collect(Collectors.joining(","));
        }
        return "";
    }
    
    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        // TODO: 从SecurityContext或其他地方获取当前用户ID
        // 这里需要根据实际的用户认证体系来实现
        return "anonymous";
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIP() {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            return ServletUtils.getClientIP(request);
        }
        return "unknown";
    }
    
    /**
     * 获取Token值
     */
    private String getTokenValue() {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            String token = request.getHeader("Authorization");
            if (StringUtils.isNotBlank(token) && token.startsWith("Bearer ")) {
                return token.substring(7);
            }
        }
        return "";
    }
}