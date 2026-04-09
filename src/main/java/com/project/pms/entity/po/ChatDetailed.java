package com.project.pms.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 聊天消息详情表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_detailed")
public class ChatDetailed {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;     // 发送者uid
    private Integer anotherId;  // 接收者uid
    private String content;     // 消息内容
    private Integer userDel;    // 发送者是否删除 0否 1是
    private Integer anotherDel; // 接收者是否删除 0否 1是
    private Integer withdraw;   // 是否撤回 0否 1是
    private Date time;          // 发送时间
}
