package com.project.pms.service;

import com.project.pms.entity.po.User;
import com.project.pms.entity.vo.Result;
import com.project.pms.exception.BusinessException;

/**
 * @className: UserAccountService
 * @description: 用户账户服务
 * @author: loser
 * @createTime: 2026/1/31 21:17
 */
public interface UserAccountService {
    void register(String username, Integer role, String password) throws BusinessException;


    Result login(String username, String password) throws BusinessException;

    User selectByUid(Integer uid);

    void logout();

    Result updatePassword(String pwd, String npwd) throws BusinessException;
}

