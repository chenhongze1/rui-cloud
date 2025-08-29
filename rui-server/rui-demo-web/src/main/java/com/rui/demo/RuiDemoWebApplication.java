package com.rui.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * RUI Demo Web 应用启动类
 *
 * @author RUI
 * @since 1.0.0
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.aop.AopAutoConfiguration.class,  // 临时禁用AOP自动配置以隔离问题
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,  // 禁用Spring Security自动配置
    org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class
})
public class RuiDemoWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuiDemoWebApplication.class, args);
    }

}