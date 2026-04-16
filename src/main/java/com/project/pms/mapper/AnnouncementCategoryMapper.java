package com.project.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.project.pms.entity.po.AnnouncementCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.pms.entity.po.User;
import com.project.pms.entity.query.CategoryQuery;
import com.project.pms.entity.query.UserInfoQuery;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 公告分类表 Mapper 接口
 * </p>
 *
 * @author loser
 * @since 2026-02-04
 */
public interface AnnouncementCategoryMapper extends BaseMapper<AnnouncementCategory> {
    /**
     * 查询公告分类列表
     *
     * @param page
     * @param query
     * @return
     */
    IPage<AnnouncementCategory> selectAnnouncementCategoryInfoList(IPage<AnnouncementCategory> page, @Param("query") CategoryQuery query);
}
