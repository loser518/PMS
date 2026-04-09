package com.project.pms.enums.im;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @className: CommandTypeEnum
 * @description: 命令类型枚举
 * @author: loser
 * @createTime: 2026/2/10 16:31
 */
@Getter
@AllArgsConstructor
public enum CommandTypeEnum {
    /**
     * 建立连接
     */
    CONNECTION(100),

    /**
     * 聊天功能 发送
     */
    CHAT_SEND(101),

    /**
     * 聊天功能 撤回
     */
    CHAT_WITHDRAW(102),

    ERROR(-1),
    ;

    private final Integer code;

    public static CommandTypeEnum getByCode(Integer code) {
        for (CommandTypeEnum value : CommandTypeEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return ERROR;
    }
}
