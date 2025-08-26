package com.rui.common.log.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rui.common.core.context.SecurityContextHolder;
import com.rui.common.core.utils.IpUtils;
import com.rui.common.core.utils.ServletUtils;
import com.rui.common.log.annotation.OperationLog;
import com.rui.common.log.config.LogProperties;
import com.rui.common.log.entity.LogInfo;
import com.rui.common.log.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * 操作日志切面
 * 
 * @author rui
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    
    private final LogService logService;
    private final LogProperties logProperties;
    private final ObjectMapper objectMapper;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    /**
     * 定义切点
     */
    @Pointcut("@annotation(com.rui.common.log.annotation.OperationLog)")
    public void operationLogPointcut() {
    }
    
    /**
     * 环绕通知
     */
    @Around("operationLogPointcut() && @annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        if (!logProperties.getEnabled() || !logProperties.getOperationLog().getEnabled()) {
            return joinPoint.proceed();
        }
        
        long startTime = System.currentTimeMillis();
        LogInfo logInfo = buildBaseLogInfo(joinPoint, operationLog);
        
        try {
            // 记录请求参数
            if (operationLog.includeArgs() && logProperties.getOperationLog().getIncludeArgs()) {
                recordRequestArgs(joinPoint, logInfo);
            }
            
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 记录返回结果
            if (operationLog.includeResult() && logProperties.getOperationLog().getIncludeResult()) {
                recordResult(result, logInfo);
            }
            
            // 计算执行时间
            logInfo.setExecutionTime(System.currentTimeMillis() - startTime);
            
            // 记录操作日志
            logService.saveOperationLog(logInfo);
            
            return result;
            
        } catch (Exception e) {
            // 记录异常信息
            if (operationLog.includeException() && logProperties.getOperationLog().getIncludeException()) {
                recordException(e, logInfo);
            }
            
            // 计算执行时间
            logInfo.setExecutionTime(System.currentTimeMillis() - startTime);
            
            // 记录操作日志
            logService.saveOperationLog(logInfo);
            
            throw e;
        }
    }
    
    /**
     * 构建基础日志信息
     */
    private LogInfo buildBaseLogInfo(JoinPoint joinPoint, OperationLog operationLog) {
        LogInfo logInfo = new LogInfo();
        
        // 基础信息
        logInfo.setLogId(UUID.randomUUID().toString().replace("-", ""))
                .setLogType("OPERATION")
                .setCreateTime(LocalDateTime.now())
                .setClassName(joinPoint.getTarget().getClass().getName())
                .setMethodName(joinPoint.getSignature().getName())
                .setThreadName(Thread.currentThread().getName());
        
        // 操作信息
        logInfo.setOperationModule(operationLog.module())
                .setOperationType(operationLog.type())
                .setOperationDescription(operationLog.description());
        
        // 请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logInfo.setRequestUri(request.getRequestURI())
                    .setRequestMethod(request.getMethod())
                    .setClientIp(IpUtils.getIpAddr(request))
                    .setUserAgent(request.getHeader("User-Agent"));
        }
        
        // 用户信息
        try {
            logInfo.setUserId(SecurityContextHolder.getUserId())
                    .setUsername(SecurityContextHolder.getUsername())
                    .setTenantId(SecurityContextHolder.getTenantId());
        } catch (Exception e) {
            log.debug("获取用户信息失败: {}", e.getMessage());
        }
        
        // 链路追踪ID
        logInfo.setTraceId(getTraceId());
        
        // SpEL表达式解析
        parseSpelExpressions(joinPoint, operationLog, logInfo);
        
        return logInfo;
    }
    
    /**
     * 记录请求参数
     */
    private void recordRequestArgs(JoinPoint joinPoint, LogInfo logInfo) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String argsJson = objectMapper.writeValueAsString(args);
                if (argsJson.length() > logProperties.getOperationLog().getMaxArgsLength()) {
                    argsJson = argsJson.substring(0, logProperties.getOperationLog().getMaxArgsLength()) + "...";
                }
                logInfo.setRequestArgs(argsJson);
            }
        } catch (Exception e) {
            log.warn("记录请求参数失败: {}", e.getMessage());
            logInfo.setRequestArgs("记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录返回结果
     */
    private void recordResult(Object result, LogInfo logInfo) {
        try {
            if (result != null) {
                String resultJson = objectMapper.writeValueAsString(result);
                if (resultJson.length() > logProperties.getOperationLog().getMaxResultLength()) {
                    resultJson = resultJson.substring(0, logProperties.getOperationLog().getMaxResultLength()) + "...";
                }
                logInfo.setResponseResult(resultJson);
            }
        } catch (Exception e) {
            log.warn("记录返回结果失败: {}", e.getMessage());
            logInfo.setResponseResult("记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录异常信息
     */
    private void recordException(Exception exception, LogInfo logInfo) {
        logInfo.setExceptionMessage(exception.getMessage());
        
        // 记录堆栈信息
        StringBuilder stackTrace = new StringBuilder();
        for (StackTraceElement element : exception.getStackTrace()) {
            stackTrace.append(element.toString()).append("\n");
        }
        
        String stackTraceStr = stackTrace.toString();
        if (stackTraceStr.length() > 5000) {
            stackTraceStr = stackTraceStr.substring(0, 5000) + "...";
        }
        logInfo.setExceptionStackTrace(stackTraceStr);
    }
    
    /**
     * 解析SpEL表达式
     */
    private void parseSpelExpressions(JoinPoint joinPoint, OperationLog operationLog, LogInfo logInfo) {
        try {
            EvaluationContext context = new StandardEvaluationContext();
            
            // 设置方法参数
            Object[] args = joinPoint.getArgs();
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    context.setVariable("arg" + i, args[i]);
                }
            }
            
            // 解析业务ID
            if (StringUtils.hasText(operationLog.businessId())) {
                Expression expression = parser.parseExpression(operationLog.businessId());
                Object value = expression.getValue(context);
                if (value != null) {
                    logInfo.setBusinessId(value.toString());
                }
            }
            
            // 解析操作人
            if (StringUtils.hasText(operationLog.operator())) {
                Expression expression = parser.parseExpression(operationLog.operator());
                Object value = expression.getValue(context);
                if (value != null) {
                    logInfo.setOperator(value.toString());
                }
            }
            
        } catch (Exception e) {
            log.warn("解析SpEL表达式失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取链路追踪ID
     */
    private String getTraceId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String traceId = request.getHeader("X-Trace-Id");
                if (StringUtils.hasText(traceId)) {
                    return traceId;
                }
            }
        } catch (Exception e) {
            log.debug("获取链路追踪ID失败: {}", e.getMessage());
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}