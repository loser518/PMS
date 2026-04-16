package com.project.pms.entity.query;

import lombok.Data;

/**
 * 课题类型查询参数
 *
 * @author loser
 * @since 2026-03-16
 */
@Data
public class ProjectTypeQuery extends PageQuery {

    /** 类型名称（模糊搜索） */
    private String name;

    /** 状态筛选：1-启用，0-禁用，null-全部 */
    private Integer status;
}
