package com.project.pms.entity.vo;

import lombok.Data;

/**
 * 选题市场展示VO（含教师信息）
 */
@Data
public class TopicVO {
    private Integer id;
    private Integer tid;
    private String teacherName;
    private String teacherTitle;
    private String teacherCollege;
    private String title;
    private Integer typeId;
    private String description;
    private String requirement;
    private Integer status;
    private Integer selectedSid;
    private String createTime;
}
