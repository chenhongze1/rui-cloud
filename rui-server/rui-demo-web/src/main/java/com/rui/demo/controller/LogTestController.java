package com.rui.demo.controller;

import com.rui.common.log.LogUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志模块测试控制器
 * 用于验证rui-common-log模块的功能
 */
@RestController
@RequestMapping("/api/log")
public class LogTestController {

    /**
     * 测试不同级别的日志记录
     */
    @GetMapping("/test")
    public Map<String, Object> testLog() {
        Map<String, Object> result = new HashMap<>();
        
        // 测试不同级别的日志
        LogUtil.info(LogTestController.class, "这是一条INFO级别的日志");
        LogUtil.warn(LogTestController.class, "这是一条WARN级别的日志");
        LogUtil.error(LogTestController.class, "这是一条ERROR级别的日志");
        LogUtil.debug(LogTestController.class, "这是一条DEBUG级别的日志");
        
        // 测试业务日志
        LogUtil.business(LogTestController.class, "用户访问", "日志测试接口");
        
        // 测试性能日志
        LogUtil.performance(LogTestController.class, "日志测试接口", 100L);
        
        result.put("message", "日志测试完成");
        result.put("traceId", LogUtil.getTraceId());
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 测试带参数的日志记录
     */
    @PostMapping("/test-with-params")
    public Map<String, Object> testLogWithParams(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        String userId = (String) params.get("userId");
        String action = (String) params.get("action");
        
        // 设置用户ID到trace上下文
        LogUtil.setTraceId("USER_" + userId + "_" + System.currentTimeMillis());
        
        LogUtil.info(LogTestController.class, "用户操作日志 - 用户ID: {}, 操作: {}", userId, action);
        LogUtil.business(LogTestController.class, action, "用户 " + userId + " 执行了 " + action + " 操作");
        
        result.put("message", "带参数的日志测试完成");
        result.put("userId", userId);
        result.put("action", action);
        result.put("traceId", LogUtil.getTraceId());
        
        return result;
    }
    
    /**
     * 测试异常日志记录
     */
    @GetMapping("/test-error")
    public Map<String, Object> testErrorLog() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 模拟一个异常
            int division = 10 / 0;
        } catch (Exception e) {
            LogUtil.error(LogTestController.class, "发生异常: " + e.getMessage(), e);
        }
        
        result.put("message", "异常日志测试完成");
        result.put("traceId", LogUtil.getTraceId());
        
        return result;
    }
    
    /**
     * 获取当前trace ID
     */
    @GetMapping("/trace-id")
    public Map<String, Object> getTraceId() {
        Map<String, Object> result = new HashMap<>();
        result.put("traceId", LogUtil.getTraceId());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}