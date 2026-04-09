package com.project.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.pms.entity.po.Announcement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.pms.entity.po.AnnouncementCategory;
import com.project.pms.entity.query.AnnouncementQuery;
import com.project.pms.entity.query.CategoryQuery;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 公告信息表 Mapper 接口
 * </p>
 *
 * @author loser
 * @since 2026-02-04
 */
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    /**
     * 查询公告信息列表
     *
     * @param page 分页参数
     * @param query 查询参数
     * @return 公告信息列表
     */
    IPage<Announcement> selectAnnouncementList(IPage<Announcement> page, @Param("query") AnnouncementQuery query);

}
