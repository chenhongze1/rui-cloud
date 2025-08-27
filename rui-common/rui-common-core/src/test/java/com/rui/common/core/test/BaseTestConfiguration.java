package com.rui.common.core.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * 测试基础配置
 * 提供统一的测试环境配置
 *
 * @author rui
 */
@TestConfiguration
public class BaseTestConfiguration {

    /**
     * MySQL测试容器
     */
    @Bean
    @Primary
    public MySQLContainer<?> mysqlContainer() {
        MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("test_db")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("sql/test-schema.sql")
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(2));
        
        mysql.start();
        return mysql;
    }

    /**
     * Redis测试容器
     */
    @Bean
    @Primary
    public GenericContainer<?> redisContainer() {
        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(1));
        
        redis.start();
        return redis;
    }

    /**
     * 测试数据源
     */
    @Bean
    @Primary
    public DataSource testDataSource(MySQLContainer<?> mysqlContainer) {
        org.springframework.boot.jdbc.DataSourceBuilder<?> builder = 
            org.springframework.boot.jdbc.DataSourceBuilder.create();
        
        return builder
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(mysqlContainer.getJdbcUrl())
                .username(mysqlContainer.getUsername())
                .password(mysqlContainer.getPassword())
                .build();
    }

    /**
     * 测试Redis连接工厂
     */
    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory(GenericContainer<?> redisContainer) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                redisContainer.getHost(),
                redisContainer.getMappedPort(6379)
        );
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * 测试Redis模板
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> testRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 测试对象映射器
     */
    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    /**
     * 测试配置属性
     */
    @Bean
    @Primary
    public TestProperties testProperties() {
        TestProperties properties = new TestProperties();
        properties.setTestMode(true);
        properties.setMockExternalServices(true);
        properties.setEnableTestContainers(true);
        properties.setTestDataInitialization(true);
        return properties;
    }

    /**
     * 测试配置属性类
     */
    public static class TestProperties {
        private boolean testMode = true;
        private boolean mockExternalServices = true;
        private boolean enableTestContainers = true;
        private boolean testDataInitialization = true;
        private String testDataPath = "classpath:test-data/";
        private Duration testTimeout = Duration.ofMinutes(5);

        // Getters and Setters
        public boolean isTestMode() { return testMode; }
        public void setTestMode(boolean testMode) { this.testMode = testMode; }
        public boolean isMockExternalServices() { return mockExternalServices; }
        public void setMockExternalServices(boolean mockExternalServices) { this.mockExternalServices = mockExternalServices; }
        public boolean isEnableTestContainers() { return enableTestContainers; }
        public void setEnableTestContainers(boolean enableTestContainers) { this.enableTestContainers = enableTestContainers; }
        public boolean isTestDataInitialization() { return testDataInitialization; }
        public void setTestDataInitialization(boolean testDataInitialization) { this.testDataInitialization = testDataInitialization; }
        public String getTestDataPath() { return testDataPath; }
        public void setTestDataPath(String testDataPath) { this.testDataPath = testDataPath; }
        public Duration getTestTimeout() { return testTimeout; }
        public void setTestTimeout(Duration testTimeout) { this.testTimeout = testTimeout; }
    }
}