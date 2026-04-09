package com.project.pms.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指导教师下拉选项 VO
 * 用于课题申报时前端展示可选教师列表（含名额信息）
 *
 * @author loser
 * @since 2026-03-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherOptionVO {

    /**
     * 教师用户ID（即 user.id，对应 project_info.tid）
     */
    private Integer tid;

    /**
     * 教师昵称
     */
    private String nickname;

    /**
     * 职称
     */
    private String title;

    /**
     * 研究方向
     */
    private String researchField;

    /**
     * 学院
     */
    private String college;

    /**
     * 最大指导学生数（0 表示未设置，不限制）
     */
    private Integer maxStudentCount;

    /**
     * 当前指导学生数
     */
    private Integer currentStudentCount;

    /**
     * 是否已满额（currentStudentCount >= maxStudentCount，且 maxStudentCount > 0）
     */
    public boolean isFull() {
        if (maxStudentCount == null || maxStudentCount <= 0) {
            return false;
        }
        return currentStudentCount != null && currentStudentCount >= maxStudentCount;
    }
}
