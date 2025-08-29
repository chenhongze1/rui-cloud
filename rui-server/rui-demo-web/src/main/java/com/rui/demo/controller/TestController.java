package com.rui.demo.controller;

import com.rui.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器
 *
 * @author RUI
 * @since 1.0.0
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public R<String> hello() {
        return R.ok("Hello, RUI Demo Web!");
    }

    @GetMapping("/log")
    public R<String> testLog() {
        return R.ok("Log test endpoint");
    }
}