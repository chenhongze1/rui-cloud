package com.rui.common.core.context;

/**
 * 安全上下文持有者
 * 用于在当前线程中存储和获取安全相关信息
 */
public class SecurityContextHolder {
    
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();
    
    /**
     * 获取当前安全上下文
     */
    public static SecurityContext getContext() {
        SecurityContext context = contextHolder.get();
        if (context == null) {
            context = new SecurityContext();
            contextHolder.set(context);
        }
        return context;
    }
    
    /**
     * 设置安全上下文
     */
    public static void setContext(SecurityContext context) {
        contextHolder.set(context);
    }
    
    /**
     * 清除当前线程的安全上下文
     */
    public static void clearContext() {
        contextHolder.remove();
    }
    
    /**
     * 获取当前用户ID
     */
    public static String getUserId() {
        Long userId = getContext().getUserIdLong();
        return userId != null ? userId.toString() : null;
    }
    
    /**
     * 获取当前用户ID（Long类型）
     */
    public static Long getUserIdLong() {
        return getContext().getUserIdLong();
    }
    
    /**
     * 设置当前用户ID
     */
    public static void setUserId(Long userId) {
        getContext().setUserId(userId);
    }
    
    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        return getContext().getUsername();
    }
    
    /**
     * 设置当前用户名
     */
    public static void setUsername(String username) {
        getContext().setUsername(username);
    }
    
    /**
     * 获取当前租户ID
     */
    public static String getTenantId() {
        return getContext().getTenantId();
    }
    
    /**
     * 设置当前租户ID
     */
    public static void setTenantId(String tenantId) {
        getContext().setTenantId(tenantId);
    }
}