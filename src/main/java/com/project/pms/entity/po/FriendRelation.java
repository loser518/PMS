package com.project.pms.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友关系表
 * status: 0-申请中, 1-已通过, 2-已拒绝, 3-已拉黑
 *
 * @author loser
 * @since 2026-03-14
 */
@Data
@TableName("friend_relation")
public class FriendRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 申请人ID */
    private Integer applicantId;

    /** 被申请人ID */
    private Integer recipientId;

    /**
     * 关系状态：0-申请中 1-已通过 2-已拒绝 3-已拉黑
     */
    private Integer status;

    /** 申请附言 */
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
