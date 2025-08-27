package com.rui.common.security.filter;

import com.rui.common.security.config.SecurityHeaderConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 安全头过滤器
 * 自动添加安全相关的HTTP头
 *
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityHeaderFilter implements Filter {

    private final SecurityHeaderConfig securityHeaderConfig;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (securityHeaderConfig.isEnabled()) {
            addSecurityHeaders(httpRequest, httpResponse);
        }

        chain.doFilter(request, response);
    }

    /**
     * 添加安全头
     */
    private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        // Content Security Policy
        addCspHeaders(response);
        
        // HTTP Strict Transport Security
        addHstsHeaders(request, response);
        
        // X-Frame-Options
        addFrameOptionsHeaders(response);
        
        // X-Content-Type-Options
        addContentTypeOptionsHeaders(response);
        
        // X-XSS-Protection
        addXssProtectionHeaders(response);
        
        // Referrer Policy
        addReferrerPolicyHeaders(response);
        
        // Permissions Policy
        addPermissionsPolicyHeaders(response);
        
        // 自定义安全头
        addCustomHeaders(response);
        
        // 移除可能泄露服务器信息的头
        removeServerHeaders(response);
    }

    /**
     * 添加 Content Security Policy 头
     */
    private void addCspHeaders(HttpServletResponse response) {
        SecurityHeaderConfig.CspConfig csp = securityHeaderConfig.getCsp();
        if (csp.isEnabled() && StringUtils.hasText(csp.getPolicy())) {
            String headerName = csp.isReportOnly() ? "Content-Security-Policy-Report-Only" : "Content-Security-Policy";
            String policy = csp.getPolicy();
            
            if (csp.isReportOnly() && StringUtils.hasText(csp.getReportUri())) {
                policy += "; report-uri " + csp.getReportUri();
            }
            
            response.setHeader(headerName, policy);
            log.debug("添加 CSP 头: {} = {}", headerName, policy);
        }
    }

    /**
     * 添加 HSTS 头
     */
    private void addHstsHeaders(HttpServletRequest request, HttpServletResponse response) {
        SecurityHeaderConfig.HstsConfig hsts = securityHeaderConfig.getHsts();
        if (hsts.isEnabled() && request.isSecure()) {
            StringBuilder hstsValue = new StringBuilder();
            hstsValue.append("max-age=").append(hsts.getMaxAge().getSeconds());
            
            if (hsts.isIncludeSubdomains()) {
                hstsValue.append("; includeSubDomains");
            }
            
            if (hsts.isPreload()) {
                hstsValue.append("; preload");
            }
            
            response.setHeader("Strict-Transport-Security", hstsValue.toString());
            log.debug("添加 HSTS 头: {}", hstsValue);
        }
    }

    /**
     * 添加 X-Frame-Options 头
     */
    private void addFrameOptionsHeaders(HttpServletResponse response) {
        SecurityHeaderConfig.FrameOptionsConfig frameOptions = securityHeaderConfig.getFrameOptions();
        if (frameOptions.isEnabled()) {
            String policy = frameOptions.getPolicy();
            if ("ALLOW-FROM".equalsIgnoreCase(policy) && StringUtils.hasText(frameOptions.getAllowFrom())) {
                policy += " " + frameOptions.getAllowFrom();
            }
            
            response.setHeader("X-Frame-Options", policy);
            log.debug("添加 X-Frame-Options 头: {}", policy);
        }
    }

    /**
     * 添加 X-Content-Type-Options 头
     */
    private void addContentTypeOptionsHeaders(HttpServletResponse response) {
        if (securityHeaderConfig.isContentTypeOptions()) {
            response.setHeader("X-Content-Type-Options", "nosniff");
            log.debug("添加 X-Content-Type-Options 头: nosniff");
        }
    }

    /**
     * 添加 X-XSS-Protection 头
     */
    private void addXssProtectionHeaders(HttpServletResponse response) {
        SecurityHeaderConfig.XssProtectionConfig xss = securityHeaderConfig.getXssProtection();
        if (xss.isEnabled()) {
            StringBuilder xssValue = new StringBuilder();
            xssValue.append(xss.isProtection() ? "1" : "0");
            
            if (xss.isProtection() && xss.isBlock()) {
                xssValue.append("; mode=block");
            }
            
            response.setHeader("X-XSS-Protection", xssValue.toString());
            log.debug("添加 X-XSS-Protection 头: {}", xssValue);
        }
    }

    /**
     * 添加 Referrer Policy 头
     */
    private void addReferrerPolicyHeaders(HttpServletResponse response) {
        if (StringUtils.hasText(securityHeaderConfig.getReferrerPolicy())) {
            response.setHeader("Referrer-Policy", securityHeaderConfig.getReferrerPolicy());
            log.debug("添加 Referrer-Policy 头: {}", securityHeaderConfig.getReferrerPolicy());
        }
    }

    /**
     * 添加 Permissions Policy 头
     */
    private void addPermissionsPolicyHeaders(HttpServletResponse response) {
        SecurityHeaderConfig.PermissionsPolicyConfig permissions = securityHeaderConfig.getPermissionsPolicy();
        if (permissions.isEnabled()) {
            Map<String, java.util.List<String>> policies = permissions.getPolicies();
            if (policies == null || policies.isEmpty()) {
                policies = permissions.getDefaultPolicies();
            }
            
            String policyValue = policies.entrySet().stream()
                    .map(entry -> entry.getKey() + "=(" + String.join(" ", entry.getValue()) + ")")
                    .collect(Collectors.joining(", "));
            
            response.setHeader("Permissions-Policy", policyValue);
            log.debug("添加 Permissions-Policy 头: {}", policyValue);
        }
    }

    /**
     * 添加自定义头
     */
    private void addCustomHeaders(HttpServletResponse response) {
        Map<String, String> customHeaders = securityHeaderConfig.getCustomHeaders();
        if (customHeaders != null && !customHeaders.isEmpty()) {
            customHeaders.forEach((name, value) -> {
                response.setHeader(name, value);
                log.debug("添加自定义安全头: {} = {}", name, value);
            });
        }
    }

    /**
     * 移除可能泄露服务器信息的头
     */
    private void removeServerHeaders(HttpServletResponse response) {
        // 移除服务器信息
        response.setHeader("Server", "");
        response.setHeader("X-Powered-By", "");
        
        log.debug("移除服务器信息头");
    }
}