package com.rui.common.log.service;

import com.rui.common.log.entity.LogInfo;

/**
 * 日志服务接口
 * 
 * @author rui
 */
public interface LogService {
    
    /**
     * 保存操作日志
     * 
     * @param logInfo 日志信息
     */
    void saveOperationLog(LogInfo logInfo);
    
    /**
     * 保存访问日志
     * 
     * @param logInfo 日志信息
     */
    void saveAccessLog(LogInfo logInfo);
    
    /**
     * 保存错误日志
     * 
     * @param logInfo 日志信息
     */
    void saveErrorLog(LogInfo logInfo);
    
    /**
     * 异步保存日志
     * 
     * @param logInfo 日志信息
     */
    void saveLogAsync(LogInfo logInfo);
    
    /**
     * 批量保存日志
     * 
     * @param logInfos 日志信息列表
     */
    void saveLogs(java.util.List<LogInfo> logInfos);
}