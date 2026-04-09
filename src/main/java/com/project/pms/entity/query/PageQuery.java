package com.project.pms.entity.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * @className: PageQuery
 * @description: 分页查询参数
 * @author: loser
 * @createTime: 2026/2/3 10:39
 */
@Data
public class PageQuery {
    private Integer pageNo; // 页码
    private Integer pageSize; // 每页数量
    private String sortBy; // 排序字段
    private Boolean isAsc; // 排序方式

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTimeStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTimeEnd;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTimeStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTimeEnd;

    /**
     * 获取偏移量
     */
    public Integer getOffset() {
        return (pageNo - 1) * pageSize;
    }

    /**
     * 获取排序SQL
     * @param defaultSortBy 默认排序字段
     * @param defaultAsc 默认排序方式
     * @return 排序SQL片段
     */
    public String getOrderByClause(String defaultSortBy, boolean defaultAsc) {
        String orderField = StringUtils.hasText(sortBy) ? sortBy : defaultSortBy;
        String orderDirection = (isAsc != null ? isAsc : defaultAsc) ? "ASC" : "DESC";

        return orderField + " " + orderDirection;
    }
}
