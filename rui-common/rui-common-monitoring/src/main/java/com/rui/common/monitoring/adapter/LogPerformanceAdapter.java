package com.rui.common.monitoring.adapter;

import com.rui.common.monitoring.service.PerformanceMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志性能监控适配器
 * 提供与原log模块性能监控功能的兼容接口
 * 确保从log模块到monitoring模块的平滑迁移
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogPerformanceAdapter {

    private final PerformanceMonitoringService performanceMonitoringService;

    /**
     * 兼容原log模块的性能记录方法
     * 对应LoggingAspect中的性能监控逻辑
     */
    public void recordPerformance(String operation, long duration, Map<String, Object> logData) {
        // 从logData中提取必要信息
        String module = (String) logData.getOrDefault("module", "unknown");
        Boolean slow = (Boolean) logData.getOrDefault("slow", false);
        Boolean success = !logData.containsKey("exception");
        
        // 构建性能监控数据
        Map<String, Object> performanceData = new HashMap<>();
        performanceData.put("slow", slow);
        performanceData.put("event", "method_completed");
        
        // 复制其他相关数据
        if (logData.containsKey("parameterCount")) {
            performanceData.put("parameterCount", logData.get("parameterCount"));
        }
        if (logData.containsKey("exception")) {
            performanceData.put("exceptionType", logData.get("exception"));
        }
        if (logData.containsKey("exceptionMessage")) {
            performanceData.put("exceptionMessage", logData.get("exceptionMessage"));
        }
        
        // 调用新的性能监控服务
        performanceMonitoringService.recordMethodPerformance(operation, module, duration, success, performanceData);
        
        log.debug("性能数据已通过适配器记录: operation={}, duration={}ms, success={}", operation, duration, success);
    }
    
    /**
     * 兼容原log模块的业务性能记录
     */
    public void recordBusinessPerformance(String operation, long duration, Map<String, Object> metrics) {
        String module = "business";
        boolean success = !metrics.containsKey("error");
        
        Map<String, Object> performanceData = new HashMap<>(metrics);
        performanceData.put("type", "business");
        
        performanceMonitoringService.recordMethodPerformance(operation, module, duration, success, performanceData);
    }
    
    /**
     * 兼容原log模块的审计性能记录
     */
    public void recordAuditPerformance(String operation, long duration, Map<String, Object> auditData) {
        String module = "audit";
        boolean success = "SUCCESS".equals(auditData.get("status"));
        
        Map<String, Object> performanceData = new HashMap<>(auditData);
        performanceData.put("type", "audit");
        
        performanceMonitoringService.recordMethodPerformance(operation, module, duration, success, performanceData);
    }
    
    /**
     * 兼容原log模块的安全性能记录
     */
    public void recordSecurityPerformance(String operation, long duration, Map<String, Object> securityData) {
        String module = "security";
        String level = (String) securityData.getOrDefault("level", "INFO");
        boolean success = !"HIGH".equals(level);
        
        Map<String, Object> performanceData = new HashMap<>(securityData);
        performanceData.put("type", "security");
        performanceData.put("securityLevel", level);
        
        performanceMonitoringService.recordMethodPerformance(operation, module, duration, success, performanceData);
    }
    
    /**
     * 兼容原log模块的通用性能记录
     */
    public void recordGeneralPerformance(String operation, String module, long duration, 
                                        boolean success, boolean isSlow, Map<String, Object> additionalData) {
        Map<String, Object> performanceData = new HashMap<>();
        performanceData.put("slow", isSlow);
        performanceData.put("type", "general");
        
        if (additionalData != null) {
            performanceData.putAll(additionalData);
        }
        
        performanceMonitoringService.recordMethodPerformance(operation, module, duration, success, performanceData);
    }
    
    /**
     * 批量记录性能数据
     * 用于迁移过程中的批量数据处理
     */
    public void batchRecordPerformance(java.util.List<PerformanceRecord> records) {
        for (PerformanceRecord record : records) {
            performanceMonitoringService.recordMethodPerformance(
                record.getOperation(), 
                record.getModule(), 
                record.getDuration(), 
                record.isSuccess(), 
                record.getAdditionalData()
            );
        }
        
        log.info("批量记录了 {} 条性能数据", records.size());
    }
    
    /**
     * 性能记录数据传输对象
     */
    public static class PerformanceRecord {
        private String operation;
        private String module;
        private long duration;
        private boolean success;
        private Map<String, Object> additionalData;
        
        public PerformanceRecord(String operation, String module, long duration, boolean success) {
            this.operation = operation;
            this.module = module;
            this.duration = duration;
            this.success = success;
            this.additionalData = new HashMap<>();
        }
        
        public PerformanceRecord(String operation, String module, long duration, boolean success, Map<String, Object> additionalData) {
            this.operation = operation;
            this.module = module;
            this.duration = duration;
            this.success = success;
            this.additionalData = additionalData != null ? additionalData : new HashMap<>();
        }
        
        // Getters and Setters
        public String getOperation() {
            return operation;
        }
        
        public void setOperation(String operation) {
            this.operation = operation;
        }
        
        public String getModule() {
            return module;
        }
        
        public void setModule(String module) {
            this.module = module;
        }
        
        public long getDuration() {
            return duration;
        }
        
        public void setDuration(long duration) {
            this.duration = duration;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public Map<String, Object> getAdditionalData() {
            return additionalData;
        }
        
        public void setAdditionalData(Map<String, Object> additionalData) {
            this.additionalData = additionalData;
        }
        
        public void addData(String key, Object value) {
            this.additionalData.put(key, value);
        }
    }
}