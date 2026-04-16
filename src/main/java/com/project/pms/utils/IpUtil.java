package com.project.pms.utils;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {
    /**
     * 获取客户端真实IP地址
     * @param request HttpServletRequest
     * @return 真实IP
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = null;
        
        // 1. 按优先级依次尝试各种代理头
        String[] headers = {
            "x-forwarded-for",          // 标准代理头 [citation:7]
            "X-Real-IP",                 // Nginx代理头 [citation:7]
            "Proxy-Client-IP",           // WebLogic等老代理
            "WL-Proxy-Client-IP",        // WebLogic代理
            "HTTP_CLIENT_IP",            // 某些客户端
            "HTTP_X_FORWARDED_FOR"       // 其他代理
        };
        
        for (String header : headers) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }
        
        // 2. 如果以上都没获取到，直接取远程地址
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
            // localhost IPv6转IPv4
            if ("0:0:0:0:0:0:0:1".equals(ip)) {
                ip = "127.0.0.1";
            }
        }
        
        // 3. 处理多级代理的情况（取第一个非unknown的IP）
        if (ip != null && ip.contains(",")) {
            String[] ips = ip.split(",");
            for (String strIp : ips) {
                if (isValidIp(strIp.trim())) {
                    ip = strIp.trim();
                    break;
                }
            }
        }
        return ip;
    }
    
    private static boolean isValidIp(String ip) {
        return ip != null && ip.length() > 0 
            && !"unknown".equalsIgnoreCase(ip);
    }
}