package com.rui.common.core.test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 测试配置类
 * 为测试提供Spring Boot配置
 *
 * @author rui
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.rui.common.core")
public class TestConfiguration {
}