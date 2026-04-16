package com.project.pms.enums.im;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @className: NotificationEnum
 * @description: 通知类型枚举
 * @author: loser
 * @createTime: 2026/3/17 21:08
 */
@Getter
@AllArgsConstructor
public enum NotificationEnum {
    //1-课题审核通过 2-课题审核驳回 3-课题需修改
    //4-进度已阅 5-进度退回 6-公告发布 7-好友申请 8-好友通过 9-系统消息

    PROJECT_APPROVED(1, "PROJECT_APPROVED", "您的课题申报已通过"),
    PROJECT_REJECTED(2, "PROJECT_REJECTED", "您的课题申报被驳回"),
    PROJECT_NEED_MODIFY(3, "PROJECT_NEED_MODIFY", "您的课题申报需要修改"),
    PROGRESS_REVIEWED(4, "PROGRESS_REVIEWED", "您的进度已阅"),
    PROGRESS_RETURNED(5, "PROGRESS_RETURNED", "您的进度退回"),
    ANNOUNCEMENT_PUBLISHED(6, "ANNOUNCEMENT_PUBLISHED", "您有新的公告"),
    FRIEND_REQUEST(7, "FRIEND_REQUEST", "您有新的好友申请"),
    FRIEND_APPROVED(8, "FRIEND_APPROVED", "您的好友申请已通过"),
    SYSTEM_MESSAGE(9, "SYSTEM_MESSAGE", "您有新的系统消息");

    private Integer type;
    private String initMessage;
    private String desc;

    public static MessageTypeEnum getByType(Integer type) {
        for (MessageTypeEnum e : MessageTypeEnum.values()) {
            if (e.getType().equals(type)) {
                return e;
            }
        }
        return null;
    }
}
