package com.project.pms.enums.announcement;

/**
 * @className: AnnouncementPriorityEnum
 * @description: 公告优先级枚举类
 * @author: loser
 * @createTime: 2026/2/4 21:48
 */
public enum AnnouncementPriorityEnum {
    NORMAL(0, "普通"),
    URGENT(1, "紧急"),
    TOP(2, "置顶");
    public Integer code;
    public String desc;
    AnnouncementPriorityEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public static String getDescByCode(Integer code) {
        for (AnnouncementPriorityEnum value : AnnouncementPriorityEnum.values()) {
            if (value.code.equals(code)) {
                return value.desc;
            }
        }
        return null;
    }
    public static Integer getCodeByDesc(String desc) {
        for (AnnouncementPriorityEnum value : AnnouncementPriorityEnum.values()) {
            if (value.desc.equals(desc)) {
                return value.code;
            }
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
