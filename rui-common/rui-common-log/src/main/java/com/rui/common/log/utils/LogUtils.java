package com.rui.common.log.utils;

import com.rui.common.core.context.SecurityContextHolder;
import com.rui.common.core.utils.IpUtils;
import com.rui.common.core.utils.ServletUtils;
import com.rui.common.core.utils.SpringUtils;
import com.rui.common.log.entity.LogInfo;
import com.rui.common.log.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 日志工具类
 * 
 * @author rui
 */
@Slf4j
public class LogUtils {
    
    /**
     * 记录操作日志
     * 
     * @param module 操作模块
     * @param type 操作类型
     * @param description 操作描述
     */
    public static void recordOperation(String module, String type, String description) {
        recordOperation(module, type, description, null, null, null);
    }
    
    /**
     * 记录操作日志
     * 
     * @param module 操作模块
     * @param type 操作类型
     * @param description 操作描述
     * @param businessId 业务ID
     */
    public static void recordOperation(String module, String type, String description, String businessId) {
        recordOperation(module, type, description, businessId, null, null);
    }
    
    /**
     * 记录操作日志
     * 
     * @param module 操作模块
     * @param type 操作类型
     * @param description 操作描述
     * @param businessId 业务ID
     * @param requestArgs 请求参数
     * @param result 执行结果
     */
    public static void recordOperation(String module, String type, String description, 
                                     String businessId, String requestArgs, String result) {
        try {
            LogInfo logInfo = buildBaseLogInfo("OPERATION");
            logInfo.setOperationModule(module)
                    .setOperationType(type)
                    .setOperationDescription(description)
                    .setBusinessId(businessId)
                    .setRequestArgs(requestArgs)
                    .setResponseResult(result);
            
            getLogService().saveOperationLog(logInfo);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
    
    /**
     * 记录错误日志
     * 
     * @param exception 异常信息
     */
    public static void recordError(Exception exception) {
        recordError(exception, null);
    }
    
    /**
     * 记录错误日志
     * 
     * @param exception 异常信息
     * @param description 错误描述
     */
    public static void recordError(Exception exception, String description) {
        try {
            LogInfo logInfo = buildBaseLogInfo("ERROR");
            logInfo.setOperationDescription(description)
                    .setExceptionMessage(exception.getMessage());
            
            // 记录堆栈信息
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : exception.getStackTrace()) {
                stackTrace.append(element.toString()).append("\n");
                if (stackTrace.length() > 5000) {
                    stackTrace.append("...");
                    break;
                }
            }
            logInfo.setExceptionStackTrace(stackTrace.toString());
            
            getLogService().saveErrorLog(logInfo);
        } catch (Exception e) {
            log.error("记录错误日志失败", e);
        }
    }
    
    /**
     * 异步记录操作日志
     * 
     * @param module 操作模块
     * @param type 操作类型
     * @param description 操作描述
     */
    public static void recordOperationAsync(String module, String type, String description) {
        recordOperationAsync(module, type, description, null, null, null);
    }
    
    /**
     * 异步记录操作日志
     * 
     * @param module 操作模块
     * @param type 操作类型
     * @param description 操作描述
     * @param businessId 业务ID
     * @param requestArgs 请求参数
     * @param result 执行结果
     */
    public static void recordOperationAsync(String module, String type, String description, 
                                          String businessId, String requestArgs, String result) {
        try {
            LogInfo logInfo = buildBaseLogInfo("OPERATION");
            logInfo.setOperationModule(module)
                    .setOperationType(type)
                    .setOperationDescription(description)
                    .setBusinessId(businessId)
                    .setRequestArgs(requestArgs)
                    .setResponseResult(result);
            
            getLogService().saveLogAsync(logInfo);
        } catch (Exception e) {
            log.error("异步记录操作日志失败", e);
        }
    }
    
    /**
     * 异步记录错误日志
     * 
     * @param exception 异常信息
     */
    public static void recordErrorAsync(Exception exception) {
        recordErrorAsync(exception, null);
    }
    
    /**
     * 异步记录错误日志
     * 
     * @param exception 异常信息
     * @param description 错误描述
     */
    public static void recordErrorAsync(Exception exception, String description) {
        try {
            LogInfo logInfo = buildBaseLogInfo("ERROR");
            logInfo.setOperationDescription(description)
                    .setExceptionMessage(exception.getMessage());
            
            // 记录堆栈信息
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : exception.getStackTrace()) {
                stackTrace.append(element.toString()).append("\n");
                if (stackTrace.length() > 5000) {
                    stackTrace.append("...");
                    break;
                }
            }
            logInfo.setExceptionStackTrace(stackTrace.toString());
            
            getLogService().saveLogAsync(logInfo);
        } catch (Exception e) {
            log.error("异步记录错误日志失败", e);
        }
    }
    
    /**
     * 构建基础日志信息
     * 
     * @param logType 日志类型
     * @return 日志信息
     */
    private static LogInfo buildBaseLogInfo(String logType) {
        LogInfo logInfo = new LogInfo();
        
        // 基础信息
        logInfo.setLogId(UUID.randomUUID().toString().replace("-", ""))
                .setLogType(logType)
                .setCreateTime(LocalDateTime.now())
                .setThreadName(Thread.currentThread().getName());
        
        // 请求信息
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logInfo.setRequestUri(request.getRequestURI())
                        .setRequestMethod(request.getMethod())
                        .setClientIp(IpUtils.getIpAddr(request))
                        .setUserAgent(request.getHeader("User-Agent"));
                
                // 链路追踪ID
                String traceId = request.getHeader("X-Trace-Id");
                if (StringUtils.hasText(traceId)) {
                    logInfo.setTraceId(traceId);
                }
            }
        } catch (Exception e) {
            log.debug("获取请求信息失败: {}", e.getMessage());
        }
        
        // 用户信息
        try {
            logInfo.setUserId(SecurityContextHolder.getUserId())
                    .setUsername(SecurityContextHolder.getUsername())
                    .setTenantId(SecurityContextHolder.getTenantId());
        } catch (Exception e) {
            log.debug("获取用户信息失败: {}", e.getMessage());
        }
        
        // 如果没有链路追踪ID，生成一个
        if (logInfo.getTraceId() == null) {
            logInfo.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        }
        
        return logInfo;
    }
    
    /**
     * 获取日志服务
     * 
     * @return 日志服务
     */
    private static LogService getLogService() {
        return SpringUtils.getBean(LogService.class);
    }
    
    /**
     * 获取调用者信息
     * 
     * @return 调用者信息
     */
    public static String getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 3) {
            StackTraceElement caller = stackTrace[3];
            return caller.getClassName() + "." + caller.getMethodName() + "(" + caller.getFileName() + ":" + caller.getLineNumber() + ")";
        }
        return "unknown";
    }
    
    /**
     * 获取当前线程的链路追踪ID
     * 
     * @return 链路追踪ID
     */
    public static String getCurrentTraceId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String traceId = request.getHeader("X-Trace-Id");
                if (StringUtils.hasText(traceId)) {
                    return traceId;
                }
            }
        } catch (Exception e) {
            log.debug("获取链路追踪ID失败: {}", e.getMessage());
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}