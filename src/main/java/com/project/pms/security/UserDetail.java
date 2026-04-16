package com.project.pms.security;

import com.project.pms.entity.po.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * @className: UserDetail
 * @description: 自定义Spring Security用户详情实现类
 *  * 用于封装用户信息，实现Spring Security的UserDetails接口
 *  * 提供用户认证和授权所需的基本信息
 * @author: loser
 * @createTime: 2026/1/31 21:18
 */
@NoArgsConstructor
@Data
@AllArgsConstructor
public class UserDetail implements UserDetails {

    private User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

