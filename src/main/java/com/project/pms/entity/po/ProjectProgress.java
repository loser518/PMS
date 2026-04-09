package com.project.pms.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * 课题进度管理表
 * </p>
 *
 * @author loser
 * @since 2026-02-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("project_progress")
public class ProjectProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 进度记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 关联课题ID
     */
    @JsonProperty("pId")
    private Integer pId;

    /**
     * 阶段名称(如中期检查/结题)
     */
    private String title;

    /**
     * 工作内容更新
     */
    private String content;

    /**
     * 进度附件存储路径
     */
    private String fileUrl;

    /**
     * 附件原始文件名
     */
    private String fileName;

    /**
     * 审核状态(0-待审核, 1-导师已阅, 2- 驳回)
     */
    private Integer status;

    /**
     * 导师指导意见
     */
    private String opinion;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;


}
