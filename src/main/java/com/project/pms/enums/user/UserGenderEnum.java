package com.project.pms.enums.user;

/**
 * @className: UserGenderEnum
 * @description: 用户性别枚举类
 * @author: loser
 * @createTime: 2026/2/3 10:46
 */
public enum UserGenderEnum {
    MALE(1, "男"),
    FEMALE(0, "女"),
    UNKNOWN(2, "未知");
    private Integer code;
    private String desc;

    UserGenderEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
