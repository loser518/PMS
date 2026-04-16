package com.project.pms.entity.query;

import lombok.Data;

/**
 * @className: AnnouncementQuery
 * @description: 公告信息查询参数
 * @author: loser
 * @createTime: 2026/2/4 21:54
 */
@Data
public class AnnouncementQuery extends PageQuery {
    private Integer id;
    private String title;
    private String content;
    private String targetRole;
    private java.util.List<String> targetRoles;  // 用于IN查询
    private Integer priority;
    private Integer status;
    private Integer authorId;
    private Integer categoryId;

    /**
     * 获取排序SQL
     */
    public String getOrderByClause() {
        return super.getOrderByClause("id", true); // 默认按id升序
    }
}
