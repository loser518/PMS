package com.project.pms.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 课题项目申报数据传输对象
 * 用于接收前端提交的数据，包含验证逻辑
 *
 * @author loser
 * @since 2026-03-13
 */
@Data
public class ProjectInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（更新时需要）
     */
    private Integer id;

    /**
     * 课题名称（新增时必填，更新时可选）
     */
    @Size(max = 200, message = "课题名称长度不能超过200个字符")
    private String title;

    /**
     * 课题类型ID（关联 project_type.id，新增时必填）
     */
    private Integer typeId;

    /**
     * 课题描述/研究内容
     */
    @Size(max = 2000, message = "课题描述长度不能超过2000个字符")
    private String description;

    /**
     * 申报学生ID（由后端自动填充）
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
    @Size(max = 500, message = "审核意见长度不能超过500个字符")
    private String opinion;
}
