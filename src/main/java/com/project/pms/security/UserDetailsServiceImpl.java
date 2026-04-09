package com.project.pms.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.project.pms.entity.po.User;
import com.project.pms.mapper.UserMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @className: UserDetailsServiceImpl
 * @description:
 * @author: loser
 * @createTime: 2026/1/31 21:18
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("加载用户: {}", username);

        // 查询用户信息
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        wrapper.ne(User::getStatus, 2);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            log.error("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        log.info("找到用户: {}, UID: {}, 角色: {}",
                user.getUsername(), user.getId(), user.getRole());

        // 创建UserDetail对象
        UserDetail userDetail = new UserDetail(user);
        log.info("创建的UserDetail: {}", userDetail);

        return userDetail;
    }
}

