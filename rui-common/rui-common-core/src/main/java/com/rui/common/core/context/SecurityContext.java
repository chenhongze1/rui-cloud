package com.rui.common.core.context;

/**
 * 安全上下文
 * 存储当前线程的安全相关信息
 */
public class SecurityContext {
    
    private Long userId;
    private String username;
    private String token;
    private String clientIp;
    private String tenantId;
    
    public SecurityContext() {
    }
    
    public SecurityContext(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
    
    public Long getUserIdLong() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}