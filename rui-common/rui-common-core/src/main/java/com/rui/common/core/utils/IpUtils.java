package com.rui.common.core.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * IP工具类
 * 用于获取客户端真实IP地址
 */
public class IpUtils {
    
    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    
    /**
     * 获取客户端IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 对于通过多个代理的情况，第一个IP为客户端真实IP，多个IP按照','分割
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        // 处理本地访问
        if (LOCALHOST_IPV6.equals(ip)) {
            ip = LOCALHOST_IPV4;
        }
        
        return ip;
    }
    
    /**
     * 判断是否为内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        
        if (LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip)) {
            return true;
        }
        
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            
            // 10.0.0.0/8
            if (first == 10) {
                return true;
            }
            
            // 172.16.0.0/12
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            
            // 192.168.0.0/16
            if (first == 192 && second == 168) {
                return true;
            }
            
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}