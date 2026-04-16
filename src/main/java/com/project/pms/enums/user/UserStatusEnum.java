package com.project.pms.enums.user;

/**
 * @className: UserStatusEnum
 * @description: 账号状态枚举
 * @author: loser
 * @createTime: 2026/1/31 21:12
 */
public enum UserStatusEnum {
    ENABLE(0, "启用"),
    DISABLE(1, "禁用"),
    DELETED(2, "注销");

    public Integer status;
    public String desc;

    UserStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static UserStatusEnum getEnum(Integer status) {
        for (UserStatusEnum e : UserStatusEnum.values()) {
            if (e.getStatus().equals(status)) {
                return e;
            }
        }
        return null;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}

