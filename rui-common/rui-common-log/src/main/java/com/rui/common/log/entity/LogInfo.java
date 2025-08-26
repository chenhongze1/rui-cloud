package com.rui.common.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 日志信息实体
 * 
 * @author rui
 */
@Data
@Accessors(chain = true)
public class LogInfo {
    
    /**
     * 日志ID
     */
    private String logId;
    
    /**
     * 链路追踪ID
     */
    private String traceId;
    
    /**
     * 日志类型（OPERATION-操作日志，ACCESS-访问日志，ERROR-错误日志）
     */
    private String logType;
    
    /**
     * 应用名称
     */
    private String applicationName;
    
    /**
     * 环境
     */
    private String environment;
    
    /**
     * 版本
     */
    private String version;
    
    /**
     * 服务器IP
     */
    private String serverIp;
    
    /**
     * 客户端IP
     */
    private String clientIp;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 租户ID
     */
    private String tenantId;
    
    /**
     * 请求URI
     */
    private String requestUri;
    
    /**
     * 请求方法
     */
    private String requestMethod;
    
    /**
     * 请求参数
     */
    private String requestArgs;
    
    /**
     * 请求头
     */
    private String requestHeaders;
    
    /**
     * 请求体
     */
    private String requestBody;
    
    /**
     * 响应结果
     */
    private String responseResult;
    
    /**
     * 响应状态码
     */
    private Integer responseStatus;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;
    
    /**
     * 操作模块
     */
    private String operationModule;
    
    /**
     * 操作类型
     */
    private String operationType;
    
    /**
     * 操作描述
     */
    private String operationDescription;
    
    /**
     * 业务ID
     */
    private String businessId;
    
    /**
     * 操作人
     */
    private String operator;
    
    /**
     * 异常信息
     */
    private String exceptionMessage;
    
    /**
     * 异常堆栈
     */
    private String exceptionStackTrace;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 浏览器信息
     */
    private String browser;
    
    /**
     * 操作系统
     */
    private String operatingSystem;
    
    /**
     * 自定义字段
     */
    private Map<String, Object> customFields;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 日志级别
     */
    private String logLevel;
    
    /**
     * 线程名称
     */
    private String threadName;
    
    /**
     * 类名
     */
    private String className;
    
    /**
     * 方法名
     */
    private String methodName;
    
    /**
     * 行号
     */
    private Integer lineNumber;
}