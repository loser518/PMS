package com.project.pms.entity.query;

import lombok.Data;

/**
 * @className: ProjectProgressQuery
 * @description: 课程项目进度查询参数
 * @author: loser
 * @createTime: 2026/2/9 16:28
 */
@Data
public class ProjectProgressQuery extends PageQuery{
    private Integer id;
    private Integer pId;
    private String title;
    private String content;
    private Integer status;
    private String opinion;
    /** 按学生ID过滤（学生角色登录时后端自动注入，数据隔离用） */
    private Integer sid;

    /**
     * 获取排序SQL
     */
    public String getOrderByClause() {
        return super.getOrderByClause("id", true); // 默认按id升序
    }
}
