package com.rui.common.feign.config;

import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign日志配置
 * 
 * @author rui
 */
@Slf4j
@Configuration
public class FeignLoggerConfig {
    
    /**
     * Feign日志级别
     * NONE: 不记录任何日志（默认）
     * BASIC: 仅记录请求方法、URL、响应状态码和执行时间
     * HEADERS: 记录BASIC级别的基础上，记录请求和响应的头信息
     * FULL: 记录所有请求和响应的明细，包括头信息、请求体、元数据
     */
    @Bean
    public Logger.Level feignLoggerLevel(FeignProperties feignProperties) {
        String logLevel = feignProperties.getLogLevel().toUpperCase();
        switch (logLevel) {
            case "NONE":
                return Logger.Level.NONE;
            case "BASIC":
                return Logger.Level.BASIC;
            case "HEADERS":
                return Logger.Level.HEADERS;
            case "FULL":
                return Logger.Level.FULL;
            default:
                log.warn("未知的Feign日志级别: {}，使用默认级别BASIC", logLevel);
                return Logger.Level.BASIC;
        }
    }
}