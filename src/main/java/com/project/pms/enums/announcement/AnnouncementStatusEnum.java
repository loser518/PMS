package com.project.pms.enums.announcement;

/**
 * @className: AnnouncementStatusEnum
 * @description: 公告状态枚举类
 * @author: loser
 * @createTime: 2026/2/4 21:49
 */
public enum AnnouncementStatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    DISCARDED(2, "已废弃");
    public Integer code;
    public String desc;
    AnnouncementStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public static String getDescByCode(Integer code) {
        for (AnnouncementStatusEnum value : AnnouncementStatusEnum.values()) {
            if (value.code.equals(code)) {
                return value.desc;
            }
        }
        return null;
    }
    public static Integer getCodeByDesc(String desc) {
        for (AnnouncementStatusEnum value : AnnouncementStatusEnum.values()) {
            if (value.desc.equals(desc)) {
                return value.code;
            }
        }
        return null;
    }

    public String getDesc() {
        return desc;
    }

    public Integer getCode() {
        return code;
    }
}
