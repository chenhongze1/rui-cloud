package com.rui.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RUI框架演示应用启动类
 * 
 * @author RUI Framework
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = {
    "com.rui.demo",
    "com.rui.common"
})
public class RuiDemoWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuiDemoWebApplication.class, args);
    }

}