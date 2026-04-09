package com.project.pms.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * 课题项目申报信息表
 * </p>
 *
 * @author loser
 * @since 2026-02-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("project_info")
public class ProjectInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 课题名称
     */
    private String title;

    /**
     * 课题类型ID（关联 project_type.id）
     */
    private Integer typeId;

    /**
     * 课题描述/研究内容
     */
    private String description;

    /**
     * 申报学生ID
     */
    private Integer sid;

    /**
     * 指导教师ID
     */
    private Integer tid;

    /**
     * 审核状态(0-待审核, 1-审核通过, 2-驳回, 3-需修改)
     */
    private Integer status;

    /**
     * 管理员或教师的最终审核意见
     */
    private String opinion;

    /**
     * 申报时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 申报学生昵称（联表查询，非数据库字段）
     */
    @TableField(exist = false)
    private String studentName;

}
