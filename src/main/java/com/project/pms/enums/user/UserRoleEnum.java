package com.project.pms.enums.user;

/**
 * @className: UserRoleEnum
 * @description: 用户角色枚举
 * @author: loser
 * @createTime: 2026/2/2 21:49
 */
public enum UserRoleEnum {
    USER(0, "学生"),
    TEACHER(1, "指导老师"),
    ADMIN(2, "管理员");

    public Integer role;
    public String desc;
    UserRoleEnum(Integer role, String desc) {
        this.role = role;
        this.desc = desc;
    }
    public static UserRoleEnum getEnum(Integer role) {
        for (UserRoleEnum e : UserRoleEnum.values()) {
            if (e.getRole().equals(role)) {
                return e;
            }
        }
        return null;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
    public Integer getRole() {
        return role;
    }
    public String getDesc() {
        return desc;
    }

}
