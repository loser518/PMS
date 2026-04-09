package com.project.pms.enums.im;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @className: MessageTypeEnum
 * @description: 消息类型枚举
 * @author: loser
 * @createTime: 2026/2/10 20:56
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum {
    INIT(0, "", "连接ws获取信息"),
    ADD_FRIEND(1, "", "添加好友消息"),
    CHAT(2, "", "普通聊天消息"),
    GROUP_CREATE(3, "群组已创建，可以和好友一起畅聊了", "群创建成功"),
    CONTACT_APPLY(4, "", "好友申请"),
    MEDIA_CHAT(5, "", "媒体文件消息"),
    FILE_UPLOAD(6, "", "文件上传完成"),
    FORCE_OFF_LINE(7, "", "强制下线通知"),
    DISSOLUTION_GROUP(8, "群聊已解散", "解散群聊通知"),
    ADD_GROUP(9, "%s加入了群组", "新成员加入通知"),
    CONTACT_NAME_UPDATE(10, "", "昵称更新"),
    LEAVE_GROUP(11, "%s退出了群聊", "成员退出通知"),
    REMOVE_GROUP(12, "%s被管理员移出群聊", "成员被移除通知"),
    ADD_FRIEND_SELF(13, "", "添加好友消息");

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
