package com.project.pms.filter;

import com.project.pms.entity.constants.Constants;
import com.project.pms.entity.po.User;
import com.project.pms.security.UserDetail;
import com.project.pms.utils.JwtUtil;
import com.project.pms.utils.RedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @className: JwtAuthenticationTokenFilter
 * @description: 登录认证过滤器
 * @author: loser
 * @createTime: 2026/1/31 21:08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {


    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;


    /**
     * token 认证过滤器，任何请求访问服务器都会先被这里拦截验证token合法性
     *
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException, ServletException {
        // 优先从请求头获取 token，SSE/EventSource 场景下退而从 URL 参数获取
        String token = request.getHeader(Constants.HEADER);

        if (!StringUtils.hasText(token) || !token.startsWith(Constants.TOKEN_PREFIX)) {
            // 尝试从 URL 参数获取（SSE/EventSource 场景，无法设置请求头）
            // 前端只传纯 JWT 字符串，后端统一加前缀
            String paramToken = request.getParameter("token");
            if (StringUtils.hasText(paramToken)) {
                // 去掉可能携带的 "Bearer " 前缀（含编码形式 "Bearer%20"），统一补上标准前缀
                String stripped = paramToken.replaceFirst("(?i)^Bearer[%20\\s]+", "");
                token = Constants.TOKEN_PREFIX + stripped;
            }
        }

        if (!StringUtils.hasText(token) || !token.startsWith(Constants.TOKEN_PREFIX)) {
            // 通过开放接口过滤器，如果没有token，放行
            filterChain.doFilter(request, response);
            return;
        }

        token = token.substring(7);

        // 解析token
        boolean verifyToken = jwtUtil.verifyToken(token);
        if (!verifyToken) {
            response.addHeader("message", "not login"); // 设置响应头信息，给前端判断用
            response.setStatus(403);
            return;
        }
        String userId = JwtUtil.getSubjectFromToken(token);
        String role = JwtUtil.getClaimFromToken(token, "role");
        // 从redis中获取用户信息
        User user = redisUtil.getObject(Constants.REDIS_KEY_SECURITY + role + ":" + userId, User.class);

        if (user == null) {
            response.addHeader("message", "not login"); // 设置响应头信息，给前端判断用
            response.setStatus(403);
            return;
        }

        // 存入SecurityContextHolder
        UserDetail loginUser = new UserDetail(user);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        //放行
        filterChain.doFilter(request, response);
    }
}
