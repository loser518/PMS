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
 * 教师发布的课题（选题市场）
 * 学生可以从这里"选题"并直接生成 ProjectInfo 申请
 *
 * status: 0-草稿 1-已发布 2-已关闭
 * 当 selectedSid != null 时表示已被学生选中
 *
 * @author loser
 * @since 2026-03-14
 */
@Data
@TableName("topic")
public class Topic implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 发布教师ID */
    private Integer tid;

    /** 课题名称 */
    private String title;

    /** 课题类型 */
    private Integer typeId;

    /** 课题描述/研究背景 */
    private String description;

    /** 对学生的要求 */
    private String requirement;

    /**
     * 状态：0-草稿 1-已发布 2-已关闭
     */
    private Integer status;

    /** 已选学生ID（null=未被选中，非null=已被某学生选中） */
    private Integer selectedSid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
