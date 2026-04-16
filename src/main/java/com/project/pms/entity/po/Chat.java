package com.project.pms.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 聊天会话表（每对用户之间有两条记录，各自维护自己的未读数和删除状态）
 * user_id -> another_id 表示 another_id 是发消息方（对方），user_id 是收消息方（自己）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat")
public class Chat {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;     // 收消息方（自己）
    private Integer anotherId;  // 发消息方（对方）
    private Integer isDeleted;  // 是否移除聊天 0否 1是
    private Integer unread;     // 未读消息数
    private Date latestTime;    // 最近消息时间
}
