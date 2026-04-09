package com.project.pms.entity.query;

import lombok.Data;

/**
 * @className: ProjectInfoQuery
 * @description: 项目信息查询参数
 * @author: loser
 * @createTime: 2026/2/9 15:58
 */
@Data
public class ProjectInfoQuery extends PageQuery{
    private Integer id;
    private String title;
    private String description;
    private Integer sid;
    private Integer tid;
    private Integer status;
    private String opinion;

    /**
     * 获取排序SQL
     */
    public String getOrderByClause() {
        return super.getOrderByClause("id", true); // 默认按id升序
    }
}
