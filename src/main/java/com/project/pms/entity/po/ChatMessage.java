package com.project.pms.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息表
 *
 * @author loser
 * @since 2026-03-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 发送者ID */
    private Integer senderId;

    /** 接收者ID */
    private Integer receiverId;

    /** 消息内容 */
    private String content;

    /**
     * 消息类型：1-文本 2-图片 3-文件
     */
    private Integer msgType;

    /** 是否已读：0-未读 1-已读 */
    private Integer isRead;

    /** 是否已撤回：0-否 1-是 */
    private Integer isWithdrawn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
