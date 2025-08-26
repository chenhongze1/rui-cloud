package com.rui.common.mongo.config;

import com.rui.common.mongo.utils.MongoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Optional;

/**
 * MongoDB自动配置类
 * 
 * @author rui
 */
@Slf4j
@Configuration
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnProperty(prefix = "rui.mongo", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MongoProperties.class)
@EnableMongoRepositories(basePackages = "com.rui.**.repository")
@EnableMongoAuditing
public class MongoAutoConfiguration {
    
    private final MongoProperties mongoProperties;
    
    public MongoAutoConfiguration(MongoProperties mongoProperties) {
        this.mongoProperties = mongoProperties;
        log.info("MongoDB模块已启用");
    }
    
    /**
     * MongoDB工具类
     */
    @Bean
    @ConditionalOnMissingBean
    public MongoUtils mongoUtils() {
        return new MongoUtils();
    }
    
    /**
     * MongoDB验证监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(validator());
    }
    
    /**
     * 验证器工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
    
    /**
     * 审计人员提供者
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rui.mongo.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuditorAware<String> auditorProvider() {
        return new AuditorAware<String>() {
            @Override
            public Optional<String> getCurrentAuditor() {
                // 这里可以集成Spring Security获取当前用户
                // 暂时返回系统用户
                try {
                    // 尝试从SecurityContext获取当前用户
                    // SecurityContext context = SecurityContextHolder.getContext();
                    // Authentication authentication = context.getAuthentication();
                    // if (authentication != null && authentication.isAuthenticated()) {
                    //     return Optional.of(authentication.getName());
                    // }
                    return Optional.of("system");
                } catch (Exception e) {
                    log.warn("获取当前审计人员失败，使用默认值: {}", e.getMessage());
                    return Optional.of("system");
                }
            }
        };
    }
    
    /**
     * MongoDB自定义转换器
     */
    @Bean
    @ConditionalOnMissingBean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(java.util.Collections.emptyList());
    }
}