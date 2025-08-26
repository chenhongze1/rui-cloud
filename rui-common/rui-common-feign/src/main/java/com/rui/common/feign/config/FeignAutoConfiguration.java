package com.rui.common.feign.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rui.common.feign.decoder.FeignErrorDecoder;
import com.rui.common.feign.interceptor.FeignRequestInterceptor;
import com.rui.common.feign.retryer.FeignRetryer;
import feign.Client;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

/**
 * Feign自动配置
 * 
 * @author rui
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(FeignProperties.class)
@ConditionalOnProperty(prefix = "rui.feign", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignClient")
@Import({FeignLoggerConfig.class})
public class FeignAutoConfiguration {
    
    /**
     * Feign请求拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public FeignRequestInterceptor feignRequestInterceptor(FeignProperties feignProperties) {
        log.info("[Feign模块] 初始化Feign请求拦截器");
        return new FeignRequestInterceptor(feignProperties);
    }
    
    /**
     * Feign错误解码器
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
        log.info("[Feign模块] 初始化Feign错误解码器");
        return new FeignErrorDecoder(objectMapper);
    }
    
    /**
     * Feign重试器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.feign", name = "retry", havingValue = "true", matchIfMissing = true)
    public Retryer feignRetryer(FeignProperties feignProperties) {
        log.info("[Feign模块] 初始化Feign重试器");
        return new FeignRetryer(feignProperties.getRetryConfig());
    }
    
    /**
     * OkHttp客户端
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OkHttpClient.class)
    public OkHttpClient okHttpClient(FeignProperties feignProperties) {
        log.info("[Feign模块] 初始化OkHttp客户端");
        
        return new OkHttpClient.Builder()
                .connectTimeout(feignProperties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(feignProperties.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(feignProperties.getReadTimeout(), TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build();
    }
    
    /**
     * Feign客户端
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OkHttpClient.class)
    public Client feignClient(OkHttpClient okHttpClient) {
        log.info("[Feign模块] 初始化Feign客户端");
        return new feign.okhttp.OkHttpClient(okHttpClient);
    }
}