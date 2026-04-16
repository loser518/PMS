package com.project.pms.service;

import com.project.pms.entity.dto.CategoryDto;
import com.project.pms.entity.po.AnnouncementCategory;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.pms.entity.query.CategoryQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;

/**
 * <p>
 * 公告分类表 服务类
 * </p>
 *
 * @author loser
 * @since 2026-02-04
 */
public interface IAnnouncementCategoryService extends IService<AnnouncementCategory> {

    /**
     * 添加公告分类
     * @param category
     * @return
     */
    Result addCategory(CategoryDto category);

    /**
     * 获取分类列表
     * @param query
     * @return
     */
    PageResult<AnnouncementCategory> getPageList(CategoryQuery  query);

    Result updateCategory(AnnouncementCategory category);
}
