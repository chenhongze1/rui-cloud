package com.rui.common.feign.retryer;

import com.rui.common.feign.config.FeignProperties;
import feign.RetryableException;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign重试器
 * 
 * @author rui
 */
@Slf4j
public class FeignRetryer implements Retryer {
    
    private final int maxAttempts;
    private final long period;
    private final long maxPeriod;
    private int attempt;
    private long sleptForMillis;
    
    public FeignRetryer(FeignProperties.RetryConfig retryConfig) {
        this.maxAttempts = retryConfig.getMaxAttempts();
        this.period = retryConfig.getPeriod();
        this.maxPeriod = retryConfig.getMaxPeriod();
        this.attempt = 1;
    }
    
    protected FeignRetryer(int maxAttempts, long period, long maxPeriod) {
        this.maxAttempts = maxAttempts;
        this.period = period;
        this.maxPeriod = maxPeriod;
        this.attempt = 1;
    }
    
    @Override
    public void continueOrPropagate(RetryableException e) {
        if (attempt++ >= maxAttempts) {
            log.error("Feign重试次数已达上限: {}, 异常: {}", maxAttempts, e.getMessage());
            throw e;
        }
        
        long interval;
        if (e.retryAfter() != null) {
            interval = e.retryAfter().getTime() - System.currentTimeMillis();
            if (interval > maxPeriod) {
                interval = maxPeriod;
            }
            if (interval < 0) {
                return;
            }
        } else {
            interval = nextMaxInterval();
        }
        
        try {
            log.debug("Feign重试第{}次，等待{}ms", attempt - 1, interval);
            Thread.sleep(interval);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            throw e;
        }
        sleptForMillis += interval;
    }
    
    /**
     * 计算下次重试间隔
     */
    long nextMaxInterval() {
        long interval = (long) (period * Math.pow(1.5, attempt - 1));
        return Math.min(interval, maxPeriod);
    }
    
    @Override
    public Retryer clone() {
        return new FeignRetryer(maxAttempts, period, maxPeriod);
    }
    
    @Override
    public String toString() {
        return String.format("FeignRetryer{maxAttempts=%d, period=%d, maxPeriod=%d, attempt=%d, sleptForMillis=%d}",
                maxAttempts, period, maxPeriod, attempt, sleptForMillis);
    }
}