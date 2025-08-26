package com.rui.common.feign.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rui.common.core.domain.R;
import com.rui.common.core.exception.ServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign错误解码器
 * 
 * @author rui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeignErrorDecoder implements ErrorDecoder {
    
    private final ObjectMapper objectMapper;
    
    private final ErrorDecoder defaultErrorDecoder = new Default();
    
    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            // 读取响应体
            String body = "";
            if (response.body() != null) {
                body = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
            
            log.error("Feign调用失败: {} - {} - {}", methodKey, response.status(), body);
            
            // 根据状态码处理不同的错误
            switch (response.status()) {
                case 400:
                    return new ServiceException("请求参数错误: " + extractErrorMessage(body));
                case 401:
                    return new ServiceException("认证失败，请重新登录");
                case 403:
                    return new ServiceException("权限不足，拒绝访问");
                case 404:
                    return new ServiceException("请求的资源不存在");
                case 500:
                    return new ServiceException("服务器内部错误: " + extractErrorMessage(body));
                case 503:
                    return new ServiceException("服务暂时不可用，请稍后重试");
                default:
                    return new ServiceException("服务调用失败: " + extractErrorMessage(body));
            }
        } catch (IOException e) {
            log.error("解析Feign错误响应失败: {}", methodKey, e);
            return new ServiceException("服务调用失败");
        } catch (Exception e) {
            log.error("处理Feign错误响应异常: {}", methodKey, e);
            return defaultErrorDecoder.decode(methodKey, response);
        }
    }
    
    /**
     * 提取错误信息
     */
    private String extractErrorMessage(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "未知错误";
        }
        
        try {
            // 尝试解析为标准响应格式
            R<?> result = objectMapper.readValue(body, R.class);
            if (result != null && result.getMsg() != null) {
                return result.getMsg();
            }
        } catch (Exception e) {
            log.debug("解析错误响应为R对象失败，使用原始响应体", e);
        }
        
        // 如果解析失败，返回原始响应体（限制长度）
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}