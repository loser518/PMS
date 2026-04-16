package com.project.pms.enums.announcement;

/**
 * @className: AnnouncementTargetRoleEnum
 * @description: 目标角色枚举类
 * @author: loser
 * @createTime: 2026/2/4 21:43
 */
public enum AnnouncementTargetRoleEnum {
    ALL("ALL", "全部"),
    STUDENT("STUDENT", "学生"),
    TEACHER("TEACHER", "教师");
    public String code;
    public String desc;

    AnnouncementTargetRoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(String code) {
        for (AnnouncementTargetRoleEnum value : AnnouncementTargetRoleEnum.values()) {
            if (value.code.equals(code)) {
                return value.desc;
            }
        }
        return null;
    }

    public static String getCodeByDesc(String desc) {
        for (AnnouncementTargetRoleEnum value : AnnouncementTargetRoleEnum.values()) {
            if (value.desc.equals(desc)) {
                return value.code;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
