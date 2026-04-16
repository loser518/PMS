package com.project.pms.entity.constants;

/**
 * @className: Constants
 * @description: 静态常量类
 * @author: loser
 * @createTime: 2026/1/31 21:07
 */
public class Constants {
    //数字零
    public static final Integer NUMBER_ZERO = 0;
    // 时间单位
    public static final long SECOND = 1L;
    // token前缀
    public static final String TOKEN_PREFIX = "Bearer ";
    // 请求头
    public static final String HEADER = "Authorization";
    // JWT密钥 Base64 标准字符集是：A-Za-z0-9+/=
    public static final String JWT_SECRET_KEY = "SldUU2VjcmV0S2V5Rm9yUHJvamVjdFN5c3RlbUJhY2tlbmQ=";
    // 过期时间
    public static final long TIME_1MIN = 60 * SECOND;
    //token失效时间
    public static final long JWT_TTL = TIME_1MIN * 24 * 60 * 2;
    //redis存储
    public static final String REDIS_KEY_CHECK_CODE = "redis:checkCode:";
    //token 键
    public static final String REDIS_KEY_TOKEN = "redis:token:";
    public static final String REDIS_KEY_USER = "redis:user:";
    public static final String REDIS_KEY_USER_LIST = "redis:user:list:";
    //security 键
    public static final String REDIS_KEY_SECURITY = "redis:security:";
    // 登录用户集合
    public static final String REDIS_KEY_LOGIN_MEMBER = "redis:login_member:";

    //可指导的学生当前人数和最大人数
    public static final Integer CURRENT_STUDENT_COUNT = 0;
    public static final Integer MAX_STUDENT_COUNT = 3;

    //昵称前缀
    public static final String NICKNAME_PREFIX_STUDENT = "学生_";
    public static final String NICKNAME_PREFIX_TEACHER = "指导老师_";
    // 头像以及背景图默认地址
    public static final String AVATAR_URL = "https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png";
    public static final String BG_URL = "https://picx.zhimg.com/v2-c19550b961bc73af99386d8d52e2ab21_r.jpg?source=2c26e567";


    /**
     * 密码正则表达式规则:
     * - 至少包含1个数字 (\\d)
     * - 至少包含1个字母 ([a-zA-Z])
     * - 允许的特殊字符: !@#$%^&*()_+-=
     * - 长度6-16位
     */
    public static final String REGEX_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])[a-zA-Z\\d!@#$%^&*()_+-=]{6,16}$";
}
