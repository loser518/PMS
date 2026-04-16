package com.project.pms.controller;

import com.project.pms.entity.constants.Constants;
import com.project.pms.entity.vo.Result;
import com.project.pms.enums.ResponseCodeEnum;
import com.project.pms.exception.BusinessException;
import com.project.pms.service.UserAccountService;
import com.project.pms.utils.RedisUtil;
import com.wf.captcha.ArithmeticCaptcha;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @className: UserAccountController
 * @description: 用户账户控制层
 * @author: loser
 * @createTime: 2026/1/31 21:15
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserAccountController {

    private final RedisUtil redisUtil;
    private final UserAccountService userAccountService;

    /**
     * 获取验证码
     *
     * @return
     */
    @GetMapping("/checkCode")
    public Result checkCode() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 43);
        String code = captcha.text();
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtil.setExValue(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey, code, Constants.TIME_1MIN * 5);
        String checkCodeBase64 = captcha.toBase64();
        Map<String, String> result = new HashMap<>();
        result.put("checkCode", checkCodeBase64);
        result.put("checkCodeKey", checkCodeKey);
        return Result.success(result, "获取验证码成功");
    }

    /**
     * 注册
     *
     * @param checkCodeKey
     * @param username
     * @param password
     * @param checkCode
     * @return
     * @throws BusinessException
     */
    @PostMapping("/register")
    public Result register(@NotEmpty String checkCodeKey,
                           @NotEmpty String username,
                           @Nonnull Integer role,
                           @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String password,
                           @NotEmpty String checkCode) throws BusinessException {
        try {
            if (!checkCode.equalsIgnoreCase((String) redisUtil.getValue(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey))) {
                throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "图片验证码不正确！");
            }
            userAccountService.register(username, role, password);
            return Result.success("注册成功！欢迎您！");
        } finally {
            redisUtil.delValue(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
        }
    }

    /**
     * 登录
     *
     * @param checkCodeKey
     * @param username
     * @param password
     * @param checkCode
     * @return
     * @throws BusinessException
     */
    @PostMapping("/login")
    public Result login(@NotEmpty String checkCodeKey,
                        @NotEmpty String username,
                        @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String password,
                        @NotEmpty String checkCode) throws BusinessException {
        try {
//            logger.info((String) redisUtil.getValue(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey));
            if (!checkCode.equalsIgnoreCase((String) redisUtil.getValue(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey))) {
                throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "图片验证码不正确！");
            }
            return userAccountService.login(username, password);
        } finally {
            redisUtil.delValue(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
        }
    }


    /**
     * 登出
     *
     * @return
     */
    @GetMapping("/logout")
    public Result logout() {
        userAccountService.logout();
        return Result.success("注销成功！");
    }

    /**
     * 修改密码
     *
     * @param pwd
     * @param npwd
     * @return
     */
    @PostMapping("/updatePassword")
    public Result updatePassword(@RequestParam("pwd") String pwd, @RequestParam("npwd") @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String npwd) throws BusinessException {
        return userAccountService.updatePassword(pwd, npwd);
    }
}
