package com.rui.common.log.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rui.common.log.config.LogProperties;
import com.rui.common.log.entity.LogInfo;
import com.rui.common.log.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 日志服务实现类
 * 
 * @author rui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {
    
    private final LogProperties logProperties;
    private final ObjectMapper objectMapper;
    
    @Override
    public void saveOperationLog(LogInfo logInfo) {
        try {
            // 设置应用信息
            enrichLogInfo(logInfo);
            
            // 输出到日志文件
            log.info("[OPERATION_LOG] {}", objectMapper.writeValueAsString(logInfo));
            
            // 如果启用了数据库保存，可以在这里添加数据库保存逻辑
            // saveToDatabase(logInfo);
            
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }
    
    @Override
    public void saveAccessLog(LogInfo logInfo) {
        try {
            // 设置应用信息
            enrichLogInfo(logInfo);
            
            // 输出到日志文件
            log.info("[ACCESS_LOG] {}", objectMapper.writeValueAsString(logInfo));
            
        } catch (Exception e) {
            log.error("保存访问日志失败", e);
        }
    }
    
    @Override
    public void saveErrorLog(LogInfo logInfo) {
        try {
            // 设置应用信息
            enrichLogInfo(logInfo);
            
            // 输出到日志文件
            log.error("[ERROR_LOG] {}", objectMapper.writeValueAsString(logInfo));
            
        } catch (Exception e) {
            log.error("保存错误日志失败", e);
        }
    }
    
    @Override
    @Async("logTaskExecutor")
    public void saveLogAsync(LogInfo logInfo) {
        CompletableFuture.runAsync(() -> {
            try {
                switch (logInfo.getLogType()) {
                    case "OPERATION":
                        saveOperationLog(logInfo);
                        break;
                    case "ACCESS":
                        saveAccessLog(logInfo);
                        break;
                    case "ERROR":
                        saveErrorLog(logInfo);
                        break;
                    default:
                        log.warn("未知的日志类型: {}", logInfo.getLogType());
                }
            } catch (Exception e) {
                log.error("异步保存日志失败", e);
            }
        });
    }
    
    @Override
    public void saveLogs(List<LogInfo> logInfos) {
        if (logInfos == null || logInfos.isEmpty()) {
            return;
        }
        
        for (LogInfo logInfo : logInfos) {
            saveLogAsync(logInfo);
        }
    }
    
    /**
     * 丰富日志信息
     */
    private void enrichLogInfo(LogInfo logInfo) {
        LogProperties.ElkConfig elkConfig = logProperties.getElk();
        
        // 设置应用信息
        if (logInfo.getApplicationName() == null) {
            logInfo.setApplicationName(elkConfig.getApplicationName());
        }
        
        if (logInfo.getEnvironment() == null) {
            logInfo.setEnvironment(elkConfig.getEnvironment());
        }
        
        if (logInfo.getVersion() == null) {
            logInfo.setVersion(elkConfig.getVersion());
        }
        
        // 设置服务器IP
        if (logInfo.getServerIp() == null) {
            logInfo.setServerIp(getServerIp());
        }
        
        // 设置自定义字段
        if (logInfo.getCustomFields() == null && !elkConfig.getCustomFields().isEmpty()) {
            logInfo.setCustomFields(new java.util.HashMap<>(elkConfig.getCustomFields()));
        }
    }
    
    /**
     * 获取服务器IP
     */
    private String getServerIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.debug("获取服务器IP失败: {}", e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * 保存到数据库（可选实现）
     */
    private void saveToDatabase(LogInfo logInfo) {
        // 这里可以实现数据库保存逻辑
        // 例如：使用JPA、MyBatis等持久化框架保存到数据库
        // logRepository.save(logInfo);
    }
}