package com.rui.common.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 配置验证器
 * 提供配置的自动验证和约束检查功能
 *
 * @author rui
 */
@Slf4j
@Component
public class ConfigValidator {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired(required = false)
    private Validator validator;
    
    /**
     * 自定义验证规则
     */
    private final Map<String, List<ValidationRule>> customRules = new ConcurrentHashMap<>();
    
    /**
     * 验证结果缓存
     */
    private final Map<String, ValidationResult> validationCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing ConfigValidator...");
        registerDefaultValidationRules();
        log.info("ConfigValidator initialized successfully");
    }

    /**
     * 注册默认验证规则
     */
    private void registerDefaultValidationRules() {
        // 端口号验证
        addValidationRule("port", new PortValidationRule());
        
        // URL验证
        addValidationRule("url", new UrlValidationRule());
        
        // 文件路径验证
        addValidationRule("path", new PathValidationRule());
        
        // 数据库连接验证
        addValidationRule("database", new DatabaseValidationRule());
        
        // Redis连接验证
        addValidationRule("redis", new RedisValidationRule());
        
        // 邮箱验证
        addValidationRule("email", new EmailValidationRule());
        
        // 时间间隔验证
        addValidationRule("duration", new DurationValidationRule());
    }

    /**
     * 验证配置对象
     */
    public ValidationResult validateConfiguration(Object config) {
        if (config == null) {
            return ValidationResult.error("Configuration object is null");
        }
        
        String configKey = config.getClass().getSimpleName();
        
        // 检查缓存
        ValidationResult cachedResult = validationCache.get(configKey);
        if (cachedResult != null && cachedResult.getTimestamp() > System.currentTimeMillis() - 60000) {
            return cachedResult;
        }
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // JSR-303/JSR-380 Bean Validation
            if (validator != null && config.getClass().isAnnotationPresent(Validated.class)) {
                Set<ConstraintViolation<Object>> violations = validator.validate(config);
                for (ConstraintViolation<Object> violation : violations) {
                    errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
                }
            }
            
            // 自定义验证规则
            validateWithCustomRules(config, errors, warnings);
            
            // 字段级验证
            validateFields(config, errors, warnings);
            
            // 业务逻辑验证
            validateBusinessLogic(config, errors, warnings);
            
            ValidationResult result;
            if (!errors.isEmpty()) {
                result = ValidationResult.error("Validation failed", errors, warnings);
            } else if (!warnings.isEmpty()) {
                result = ValidationResult.warning("Validation completed with warnings", warnings);
            } else {
                result = ValidationResult.success();
            }
            
            // 缓存结果
            validationCache.put(configKey, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during configuration validation", e);
            return ValidationResult.error("Validation error: " + e.getMessage());
        }
    }

    /**
     * 使用自定义规则验证
     */
    private void validateWithCustomRules(Object config, List<String> errors, List<String> warnings) {
        String configType = config.getClass().getSimpleName().toLowerCase();
        
        for (Map.Entry<String, List<ValidationRule>> entry : customRules.entrySet()) {
            String ruleType = entry.getKey();
            if (configType.contains(ruleType)) {
                for (ValidationRule rule : entry.getValue()) {
                    ValidationResult result = rule.validate(config);
                    if (!result.isValid()) {
                        if (result.getLevel() == ValidationLevel.ERROR) {
                            errors.addAll(result.getErrors());
                        } else if (result.getLevel() == ValidationLevel.WARNING) {
                            warnings.addAll(result.getWarnings());
                        }
                    }
                }
            }
        }
    }

    /**
     * 字段级验证
     */
    private void validateFields(Object config, List<String> errors, List<String> warnings) {
        Field[] fields = config.getClass().getDeclaredFields();
        
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(config);
                validateField(field, value, errors, warnings);
            } catch (IllegalAccessException e) {
                log.warn("Cannot access field: {}", field.getName(), e);
            }
        }
    }

    /**
     * 验证单个字段
     */
    private void validateField(Field field, Object value, List<String> errors, List<String> warnings) {
        String fieldName = field.getName();
        
        // 检查@NotNull
        if (field.isAnnotationPresent(NotNull.class) && value == null) {
            errors.add(fieldName + " cannot be null");
            return;
        }
        
        if (value == null) {
            return;
        }
        
        // 检查@NotEmpty
        if (field.isAnnotationPresent(NotEmpty.class)) {
            if (value instanceof String && ((String) value).isEmpty()) {
                errors.add(fieldName + " cannot be empty");
            } else if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
                errors.add(fieldName + " cannot be empty");
            }
        }
        
        // 检查@NotBlank
        if (field.isAnnotationPresent(NotBlank.class) && value instanceof String) {
            if (!StringUtils.hasText((String) value)) {
                errors.add(fieldName + " cannot be blank");
            }
        }
        
        // 检查@Min和@Max
        if (value instanceof Number) {
            Number numValue = (Number) value;
            
            if (field.isAnnotationPresent(Min.class)) {
                long min = field.getAnnotation(Min.class).value();
                if (numValue.longValue() < min) {
                    errors.add(fieldName + " must be at least " + min);
                }
            }
            
            if (field.isAnnotationPresent(Max.class)) {
                long max = field.getAnnotation(Max.class).value();
                if (numValue.longValue() > max) {
                    errors.add(fieldName + " must be at most " + max);
                }
            }
        }
        
        // 检查@Size
        if (field.isAnnotationPresent(Size.class)) {
            Size sizeAnnotation = field.getAnnotation(Size.class);
            int size = 0;
            
            if (value instanceof String) {
                size = ((String) value).length();
            } else if (value instanceof Collection) {
                size = ((Collection<?>) value).size();
            } else if (value instanceof Map) {
                size = ((Map<?, ?>) value).size();
            }
            
            if (size < sizeAnnotation.min()) {
                errors.add(fieldName + " size must be at least " + sizeAnnotation.min());
            }
            if (size > sizeAnnotation.max()) {
                errors.add(fieldName + " size must be at most " + sizeAnnotation.max());
            }
        }
        
        // 检查@Pattern
        if (field.isAnnotationPresent(jakarta.validation.constraints.Pattern.class) && value instanceof String) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(field.getAnnotation(jakarta.validation.constraints.Pattern.class).regexp());
            if (!pattern.matcher((String) value).matches()) {
                errors.add(fieldName + " does not match required pattern");
            }
        }
        
        // 检查@Email
        if (field.isAnnotationPresent(Email.class) && value instanceof String) {
            String email = (String) value;
            if (!isValidEmail(email)) {
                errors.add(fieldName + " is not a valid email address");
            }
        }
    }

    /**
     * 业务逻辑验证
     */
    private void validateBusinessLogic(Object config, List<String> errors, List<String> warnings) {
        // 检查配置的一致性和业务规则
        if (config.getClass().getSimpleName().toLowerCase().contains("database")) {
            validateDatabaseConfig(config, errors, warnings);
        } else if (config.getClass().getSimpleName().toLowerCase().contains("redis")) {
            validateRedisConfig(config, errors, warnings);
        } else if (config.getClass().getSimpleName().toLowerCase().contains("security")) {
            validateSecurityConfig(config, errors, warnings);
        }
    }

    /**
     * 验证数据库配置
     */
    private void validateDatabaseConfig(Object config, List<String> errors, List<String> warnings) {
        try {
            // 检查连接池配置的合理性
            Field maxPoolSizeField = findField(config.getClass(), "maxPoolSize", "maximumPoolSize");
            Field minPoolSizeField = findField(config.getClass(), "minPoolSize", "minimumIdle");
            
            if (maxPoolSizeField != null && minPoolSizeField != null) {
                maxPoolSizeField.setAccessible(true);
                minPoolSizeField.setAccessible(true);
                
                Object maxPoolSize = maxPoolSizeField.get(config);
                Object minPoolSize = minPoolSizeField.get(config);
                
                if (maxPoolSize instanceof Number && minPoolSize instanceof Number) {
                    int max = ((Number) maxPoolSize).intValue();
                    int min = ((Number) minPoolSize).intValue();
                    
                    if (min > max) {
                        errors.add("Minimum pool size cannot be greater than maximum pool size");
                    }
                    
                    if (max > 100) {
                        warnings.add("Maximum pool size is very large (" + max + "), consider reducing it");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error validating database config", e);
        }
    }

    /**
     * 验证Redis配置
     */
    private void validateRedisConfig(Object config, List<String> errors, List<String> warnings) {
        try {
            Field timeoutField = findField(config.getClass(), "timeout", "connectionTimeout");
            if (timeoutField != null) {
                timeoutField.setAccessible(true);
                Object timeout = timeoutField.get(config);
                
                if (timeout instanceof Number) {
                    long timeoutValue = ((Number) timeout).longValue();
                    if (timeoutValue < 1000) {
                        warnings.add("Redis timeout is very short (" + timeoutValue + "ms), consider increasing it");
                    } else if (timeoutValue > 30000) {
                        warnings.add("Redis timeout is very long (" + timeoutValue + "ms), consider reducing it");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error validating Redis config", e);
        }
    }

    /**
     * 验证安全配置
     */
    private void validateSecurityConfig(Object config, List<String> errors, List<String> warnings) {
        try {
            // 检查JWT密钥长度
            Field jwtSecretField = findField(config.getClass(), "jwtSecret", "secret", "key");
            if (jwtSecretField != null) {
                jwtSecretField.setAccessible(true);
                Object jwtSecret = jwtSecretField.get(config);
                
                if (jwtSecret instanceof String) {
                    String secret = (String) jwtSecret;
                    if (secret.length() < 32) {
                        errors.add("JWT secret is too short, should be at least 32 characters");
                    } else if (secret.equals("default") || secret.equals("secret")) {
                        errors.add("JWT secret should not use default values");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error validating security config", e);
        }
    }

    /**
     * 查找字段
     */
    private Field findField(Class<?> clazz, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // 继续查找下一个字段名
            }
        }
        return null;
    }

    /**
     * 验证邮箱格式
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    /**
     * 添加自定义验证规则
     */
    public void addValidationRule(String type, ValidationRule rule) {
        customRules.computeIfAbsent(type, k -> new ArrayList<>()).add(rule);
    }

    /**
     * 移除验证规则
     */
    public void removeValidationRule(String type, ValidationRule rule) {
        List<ValidationRule> rules = customRules.get(type);
        if (rules != null) {
            rules.remove(rule);
            if (rules.isEmpty()) {
                customRules.remove(type);
            }
        }
    }

    /**
     * 清除验证缓存
     */
    public void clearValidationCache() {
        validationCache.clear();
    }

    /**
     * 验证规则接口
     */
    public interface ValidationRule {
        ValidationResult validate(Object config);
    }

    /**
     * 端口验证规则
     */
    public static class PortValidationRule implements ValidationRule {
        @Override
        public ValidationResult validate(Object config) {
            // 实现端口验证逻辑
            return ValidationResult.success();
        }
    }

    /**
     * URL验证规则
     */
    public static class UrlValidationRule implements ValidationRule {
        @Override
        public ValidationResult validate(Object config) {
            // 实现URL验证逻辑
            return ValidationResult.success();
        }
    }

    /**
     * 路径验证规则
     */
    public static class PathValidationRule implements ValidationRule {
        @Override
        public ValidationResult validate(Object config) {
            // 实现路径验证逻辑
            return ValidationResult.success();
        }
    }

    /**
     * 数据库验证规则
     */
    public static class DatabaseValidationRule implements ValidationRule {
        @Override
        public ValidationResult validate(Object config) {
            // 实现数据库配置验证逻辑
            return ValidationResult.success();
        }
    }

    /**
     * Redis验证规则
     */
    public static class RedisValidationRule implements ValidationRule {
        @Override
        public ValidationResult validate(Object config) {
            // 实现Redis配置验证逻辑
            return ValidationResult.success();
        }
    }

    /**
     * 邮箱验证规则
     */
    public static class EmailValidationRule implements ValidationRule {
        @Override
        public ValidationResult validate(Object config) {
            // 实现邮箱配置验证逻辑
            return ValidationResult.success();
        }
    }

    /**
     * 时间间隔验证规则
     */
    public static class DurationValidationRule implements ValidationRule {
        @Override
        public ValidationResult validate(Object config) {
            // 实现时间间隔验证逻辑
            return ValidationResult.success();
        }
    }

    /**
     * 验证级别
     */
    public enum ValidationLevel {
        ERROR, WARNING, INFO
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private boolean valid;
        private ValidationLevel level;
        private String message;
        private List<String> errors;
        private List<String> warnings;
        private long timestamp;
        
        private ValidationResult(boolean valid, ValidationLevel level, String message, 
                               List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.level = level;
            this.message = message;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, ValidationLevel.INFO, "Validation successful", null, null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, ValidationLevel.ERROR, message, 
                Arrays.asList(message), null);
        }
        
        public static ValidationResult error(String message, List<String> errors) {
            return new ValidationResult(false, ValidationLevel.ERROR, message, errors, null);
        }
        
        public static ValidationResult error(String message, List<String> errors, List<String> warnings) {
            return new ValidationResult(false, ValidationLevel.ERROR, message, errors, warnings);
        }
        
        public static ValidationResult warning(String message, List<String> warnings) {
            return new ValidationResult(true, ValidationLevel.WARNING, message, null, warnings);
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public ValidationLevel getLevel() { return level; }
        public String getMessage() { return message; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public long getTimestamp() { return timestamp; }
    }
}