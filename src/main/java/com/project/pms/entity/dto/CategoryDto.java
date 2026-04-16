package com.project.pms.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @className: CategoryDto
 * @description: 分类信息
 * @author: loser
 * @createTime: 2026/2/5 20:54
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    /**
     * 分类名称
     */
    private String name;

    /**
     * 显示颜色 (success, warning, danger, info)
     */
    private String colorType;

    /**
     * 是否启用
     */
    private String isActive;

    /**
     * 创建时间
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
}
