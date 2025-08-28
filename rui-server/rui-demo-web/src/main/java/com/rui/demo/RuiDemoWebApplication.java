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
    DataSourceAutoConfiguration.class
})
public class RuiDemoWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuiDemoWebApplication.class, args);
    }

}