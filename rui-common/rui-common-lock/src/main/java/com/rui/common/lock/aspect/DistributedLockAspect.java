package com.rui.common.lock.aspect;

import com.rui.common.core.exception.ServiceException;
import com.rui.common.lock.annotation.DistributedLock;
import com.rui.common.lock.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面
 * 
 * @author rui
 */
@Slf4j
@Aspect
@Order(1)
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {
    
    private final DistributedLockService distributedLockService;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = buildLockKey(joinPoint, distributedLock);
        RLock lock = distributedLockService.getLock(lockKey, distributedLock.lockType());
        
        boolean acquired = false;
        try {
            // 尝试获取锁
            acquired = tryAcquireLock(lock, distributedLock);
            
            if (!acquired) {
                // 获取锁失败
                if (distributedLock.throwException()) {
                    throw new ServiceException(distributedLock.failMessage());
                } else {
                    return null;
                }
            }
            
            // 执行业务方法
            return joinPoint.proceed();
            
        } finally {
            // 释放锁
            if (acquired) {
                distributedLockService.unlock(lock);
            }
        }
    }
    
    /**
     * 构建锁的key
     */
    private String buildLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        StringBuilder lockKey = new StringBuilder();
        
        // 锁名称
        if (StringUtils.hasText(distributedLock.name())) {
            lockKey.append(distributedLock.name());
        } else {
            lockKey.append(method.getDeclaringClass().getSimpleName())
                   .append(":")
                   .append(method.getName());
        }
        
        // 锁key（支持SpEL表达式）
        if (StringUtils.hasText(distributedLock.key())) {
            String keyExpression = distributedLock.key();
            if (keyExpression.contains("#")) {
                // 解析SpEL表达式
                String parsedKey = parseSpEL(keyExpression, joinPoint);
                lockKey.append(":").append(parsedKey);
            } else {
                lockKey.append(":").append(keyExpression);
            }
        }
        
        return lockKey.toString();
    }
    
    /**
     * 解析SpEL表达式
     */
    private String parseSpEL(String expression, ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
            
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.warn("解析SpEL表达式失败: {}", expression, e);
            return expression;
        }
    }
    
    /**
     * 尝试获取锁
     */
    private boolean tryAcquireLock(RLock lock, DistributedLock distributedLock) {
        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();
        TimeUnit timeUnit = distributedLock.timeUnit();
        
        try {
            if (waitTime == -1 && leaseTime == -1) {
                return lock.tryLock();
            } else if (waitTime != -1 && leaseTime == -1) {
                return lock.tryLock(waitTime, timeUnit);
            } else if (waitTime == -1 && leaseTime != -1) {
                lock.lock(leaseTime, timeUnit);
                return true;
            } else {
                return lock.tryLock(waitTime, leaseTime, timeUnit);
            }
        } catch (InterruptedException e) {
            log.error("获取分布式锁被中断", e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.error("获取分布式锁异常", e);
            return false;
        }
    }
}