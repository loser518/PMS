package com.project.pms.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @className: PageResult
 * @description: 分页结果类
 * @author: loser
 * @createTime: 2026/2/3 10:37
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private Integer total; // 总数
    private Integer pageNo; // 当前页码
    private Integer pageSize; // 每页数量
    private List<T> list; // 数据

    /**
     * 快速创建成功结果
     */
    public static <T> PageResult<T> success(List<T> list, Integer total, Integer pageNo, Integer pageSize) {
        return new PageResult<>(total, pageNo, pageSize, list);
    }
}
