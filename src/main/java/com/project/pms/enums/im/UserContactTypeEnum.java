package com.project.pms.enums.im;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @className: UserContactTypeEnum
 * @description: 用户联系人类型枚举
 * @author: loser
 * @createTime: 2026/2/10 21:09
 */
@Getter
@AllArgsConstructor
public enum UserContactTypeEnum {
    FRIEND(0, "好友"),
    GROUP(1, "群组");
    private Integer code;
    private String desc;

    public static UserContactTypeEnum getByCode(Integer code) {
        for (UserContactTypeEnum value : UserContactTypeEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
