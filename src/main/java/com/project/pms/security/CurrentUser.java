package com.project.pms.security;

import com.project.pms.entity.constants.Constants;
import com.project.pms.entity.po.User;
import com.project.pms.mapper.UserMapper;
import com.project.pms.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @className: CurrentUser
 * @description: 当前用户
 * @author: loser
 * @createTime: 2026/2/3 11:17
 */
@Service
@RequiredArgsConstructor
public class CurrentUser {

    private final RedisUtil redisUtil;
    private final UserMapper userMapper;
    /**
     * 获取当前登录用户的uid，也是JWT认证的一环
     *
     * @return 当前登录用户的uid
     */
    public Integer getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof UsernamePasswordAuthenticationToken authenticationToken)) {
            throw new org.springframework.security.access.AccessDeniedException("未登录或登录已过期，请重新登录");
        }
        UserDetail loginUser = (UserDetail) authenticationToken.getPrincipal();
        User user = loginUser.getUser();
        return user.getId();
    }

    /**
     * 获取当前登录用户的角色：0-学生，1-教师，2-管理员
     *
     * @return 角色值
     */
    public Integer getRole() {
        Integer id = getUserId();
        User user = redisUtil.getObject(Constants.REDIS_KEY_USER + id, User.class);
        if (user == null) {
            user = userMapper.selectById(id);
            redisUtil.setExObjectValue(Constants.REDIS_KEY_USER + user.getId(), user);
        }
        return user.getRole();
    }

    /**
     * 判断当前用户是否是学生（role == 0）
     *
     * @return 是否学生
     */
    public Boolean isStudent() {
        return getRole() == 0;
    }

    /**
     * 判断当前用户是否管理员
     *
     * @return 是否管理员 true/false
     */
    public Boolean isAdmin() {
        Integer role = getRole();
        return role == 1 || role == 2;
    }
}
