package com.rui.common.log.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rui.common.log.aspect.OperationLogAspect;
import com.rui.common.log.filter.AccessLogFilter;
import com.rui.common.log.service.LogService;
import com.rui.common.log.service.impl.LogServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 日志自动配置类
 * 
 * @author rui
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnProperty(prefix = "rui.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogAutoConfiguration {
    
    private final LogProperties logProperties;
    
    /**
     * 日志服务
     */
    @Bean
    @ConditionalOnMissingBean
    public LogService logService(ObjectMapper objectMapper) {
        return new LogServiceImpl(logProperties, objectMapper);
    }
    
    /**
     * 操作日志切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.log.operation-log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OperationLogAspect operationLogAspect(LogService logService, ObjectMapper objectMapper) {
        return new OperationLogAspect(logService, logProperties, objectMapper);
    }
    
    /**
     * 访问日志过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.log.access-log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AccessLogFilter accessLogFilter(LogService logService, ObjectMapper objectMapper) {
        return new AccessLogFilter(logService, logProperties, objectMapper);
    }
    
    /**
     * 日志任务执行器
     */
    @Bean("logTaskExecutor")
    @ConditionalOnMissingBean(name = "logTaskExecutor")
    public Executor logTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(2);
        
        // 最大线程数
        executor.setMaxPoolSize(10);
        
        // 队列容量
        executor.setQueueCapacity(200);
        
        // 线程名前缀
        executor.setThreadNamePrefix("log-task-");
        
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * ObjectMapper配置
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 配置时间格式
        mapper.findAndRegisterModules();
        
        // 忽略未知属性
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 忽略空值
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        
        return mapper;
    }
}