package com.rui.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 客户端工具类
 *
 * @author rui
 */
public class ServletUtils {

    /**
     * 获取String参数
     */
    public static String getParameter(String name) {
        return getRequest().getParameter(name);
    }

    /**
     * 获取String参数
     */
    public static String getParameter(String name, String defaultValue) {
        return convert(getRequest().getParameter(name), defaultValue);
    }

    /**
     * 获取Integer参数
     */
    public static Integer getParameterToInt(String name) {
        return convert(getRequest().getParameter(name), (Integer) null);
    }

    /**
     * 获取Integer参数
     */
    public static Integer getParameterToInt(String name, Integer defaultValue) {
        return convert(getRequest().getParameter(name), defaultValue);
    }

    /**
     * 获取Boolean参数
     */
    public static Boolean getParameterToBool(String name) {
        return convert(getRequest().getParameter(name), (Boolean) null);
    }

    /**
     * 获取Boolean参数
     */
    public static Boolean getParameterToBool(String name, Boolean defaultValue) {
        return convert(getRequest().getParameter(name), defaultValue);
    }

    /**
     * 获得所有请求参数
     */
    public static String getParameters() {
        return getRequest().getQueryString();
    }

    /**
     * 获取request
     */
    public static HttpServletRequest getRequest() {
        return getRequestAttributes().getRequest();
    }

    /**
     * 获取response
     */
    public static HttpServletResponse getResponse() {
        return getRequestAttributes().getResponse();
    }

    /**
     * 获取session
     */
    public static jakarta.servlet.http.HttpSession getSession() {
        return getRequest().getSession();
    }

    public static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return (ServletRequestAttributes) attributes;
    }

    /**
     * 将字符串渲染到客户端
     *
     * @param response 渲染对象
     * @param string   待渲染的字符串
     */
    public static void renderString(HttpServletResponse response, String string) {
        try {
            response.setStatus(200);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是否是Ajax异步请求
     *
     * @param request
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        String accept = request.getHeader("accept");
        if (accept != null && accept.contains("application/json")) {
            return true;
        }

        String xRequestedWith = request.getHeader("X-Requested-With");
        if (xRequestedWith != null && xRequestedWith.contains("XMLHttpRequest")) {
            return true;
        }

        String uri = request.getRequestURI();
        if (StringUtils.inStringIgnoreCase(uri, ".json", ".xml")) {
            return true;
        }

        String ajax = request.getParameter("__ajax");
        return StringUtils.inStringIgnoreCase(ajax, "json", "xml");
    }

    /**
     * 获取客户端IP
     */
    public static String getClientIP() {
        return getClientIP(getRequest());
    }

    /**
     * 获取客户端IP
     *
     * @param request 请求对象
     */
    public static String getClientIP(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : getMultistageReverseProxyIp(ip);
    }

    /**
     * 检查是否为内部IP地址
     *
     * @param ip IP地址
     * @return 结果
     */
    public static boolean internalIp(String ip) {
        byte[] addr = textToNumericFormatV4(ip);
        return internalIp(addr) || "127.0.0.1".equals(ip);
    }

    /**
     * 检查是否为内部IP地址
     *
     * @param addr byte地址
     * @return 结果
     */
    private static boolean internalIp(byte[] addr) {
        if (StringUtils.isNull(addr) || addr.length < 2) {
            return true;
        }
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        // 10.x.x.x/8
        final byte SECTION_1 = 0x0A;
        // 172.16.x.x/12
        final byte SECTION_2 = (byte) 0xAC;
        final byte SECTION_3 = (byte) 0x10;
        final byte SECTION_4 = (byte) 0x1F;
        // 192.168.x.x/16
        final byte SECTION_5 = (byte) 0xC0;
        final byte SECTION_6 = (byte) 0xA8;
        switch (b0) {
            case SECTION_1:
                return true;
            case SECTION_2:
                if (b1 >= SECTION_3 && b1 <= SECTION_4) {
                    return true;
                }
            case SECTION_5:
                switch (b1) {
                    case SECTION_6:
                        return true;
                }
        }
        return false;
    }

    /**
     * 将IPv4地址转换成字节
     *
     * @param text IPv4地址
     * @return byte 字节
     */
    public static byte[] textToNumericFormatV4(String text) {
        if (text.length() == 0) {
            return null;
        }

        byte[] bytes = new byte[4];
        String[] elements = text.split("\\.", -1);
        try {
            long l;
            int i;
            switch (elements.length) {
                case 1:
                    l = Long.parseLong(elements[0]);
                    if ((l < 0L) || (l > 4294967295L)) {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l >> 24 & 0xFF);
                    bytes[1] = (byte) (int) ((l & 0xFFFFFF) >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 2:
                    l = Integer.parseInt(elements[0]);
                    if ((l < 0L) || (l > 255L)) {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l & 0xFF);
                    l = Integer.parseInt(elements[1]);
                    if ((l < 0L) || (l > 16777215L)) {
                        return null;
                    }
                    bytes[1] = (byte) (int) (l >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 3:
                    for (i = 0; i < 2; ++i) {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L)) {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    l = Integer.parseInt(elements[2]);
                    if ((l < 0L) || (l > 65535L)) {
                        return null;
                    }
                    bytes[2] = (byte) (int) (l >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 4:
                    for (i = 0; i < 4; ++i) {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L)) {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    break;
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return bytes;
    }

    /**
     * 获取多级反向代理IP
     *
     * @param ip 获取的IP地址
     * @return 多级反向代理IP
     */
    public static String getMultistageReverseProxyIp(String ip) {
        // 多级反向代理检测
        if (ip != null && ip.indexOf(",") > 0) {
            final String[] ips = ip.trim().split(",");
            for (String subIp : ips) {
                if (false == isUnknown(subIp)) {
                    ip = subIp;
                    break;
                }
            }
        }
        return StringUtils.substring(ip, 0, 255);
    }

    /**
     * 检测给定字符串是否为未知，多用于检测HTTP请求相关
     *
     * @param checkString 被检测的字符串
     * @return 是否未知
     */
    public static boolean isUnknown(String checkString) {
        return StringUtils.isBlank(checkString) || "unknown".equalsIgnoreCase(checkString);
    }

    /**
     * 内容编码
     *
     * @param str 内容
     * @return 编码后的内容
     */
    public static String urlEncode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    /**
     * 内容解码
     *
     * @param str 内容
     * @return 解码后的内容
     */
    public static String urlDecode(String str) {
        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }

    /**
     * 转换为字符串<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 结果
     */
    public static String convert(Object value, String defaultValue) {
        if (null == value) {
            return defaultValue;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }

    /**
     * 转换为Integer类型<br>
     * 如果给定的值为空，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 结果
     */
    public static Integer convert(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        final String valueStr = convert(value, (String) null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(valueStr.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为Boolean类型<br>
     * String支持的值为：true、false、yes、ok、no，1,0 如果给定的值为空，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 结果
     */
    public static Boolean convert(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String valueStr = convert(value, (String) null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }
        valueStr = valueStr.trim().toLowerCase();
        switch (valueStr) {
            case "true":
            case "yes":
            case "ok":
            case "1":
                return true;
            case "false":
            case "no":
            case "0":
                return false;
            default:
                return defaultValue;
        }
    }
}