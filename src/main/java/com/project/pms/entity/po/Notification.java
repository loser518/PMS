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
 * 站内通知表
 * 用于记录课题审核结果、进度批阅、公告推送等系统通知
 *
 * @author loser
 * @since 2026-03-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通知ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 接收者用户ID */
    private Integer receiverId;

    /** 发送者用户ID（为空表示系统消息） */
    private Integer senderId;

    /**
     * 通知类型
     * 1-课题审核通过 2-课题审核驳回 3-课题需修改
     * 4-进度已阅 5-进度退回 6-公告发布 7-好友申请 8-好友通过 9-系统消息
     */
    private Integer type;

    /** 通知标题 */
    private String title;

    /** 通知正文 */
    private String content;

    /** 关联业务ID（如课题ID、进度ID、公告ID） */
    private Integer refId;

    /** 已读状态：0-未读，1-已读 */
    private Integer isRead;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
