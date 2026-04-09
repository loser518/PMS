package com.project.pms.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @className: CorsConfig
 * @description:  跨域配置
 * @author: loser
 * @createTime: 2026/1/31 21:03
 */
@Configuration
public class CorsConfig implements Filter {
    /**
     * 跨域过滤器
     *
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        // 设置允许的源（Origin）
        String origin = request.getHeader("Origin");
        if (origin != null) {
            // 动态设置允许的来源（根据请求的来源决定）
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        // 效果：允许任何来源访问，但比通配符"*"更安全

        // 设置允许的请求头
        String headers = request.getHeader("Access-Control-Request-Headers");
        if (headers != null) {
            // 允许客户端发送的请求头
            response.setHeader("Access-Control-Allow-Headers", headers);
            // 允许客户端访问的响应头
            response.setHeader("Access-Control-Expose-Headers", headers);
        }
        // 效果：动态允许客户端需要的所有请求头

        // 设置允许的HTTP方法
        response.setHeader("Access-Control-Allow-Methods", "*");
        // 效果：允许所有 HTTP 方法（GET, POST, PUT, DELETE, OPTIONS 等）

        // 设置预检请求缓存时间
        response.setHeader("Access-Control-Max-Age", "3600");
        // 效果：预检请求结果缓存 3600 秒（1小时），减少重复预检

        // 设置允许携带凭证
        response.setHeader("Access-Control-Allow-Credentials", "true");
        // 效果：允许发送 Cookie、Authorization 头等凭证信息

        filterChain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }


    @Override
    public void destroy() {
    }
}
