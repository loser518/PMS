package com.project.pms.service;

import com.project.pms.entity.po.Announcement;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.pms.entity.query.AnnouncementQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;

/**
 * <p>
 * 公告信息表 服务类
 * </p>
 *
 * @author loser
 * @since 2026-02-04
 */
public interface IAnnouncementService extends IService<Announcement> {

    /**
     * 添加公告信息
     * @param announcement 公告信息
     * @return 添加结果
     */
    Result addAnnouncement(Announcement announcement);

    /**
     * 获取公告信息列表
     * @param query 查询参数
     * @return 公告信息列表
     */
    PageResult<Announcement> getAnnouncement(AnnouncementQuery query);

    /**
     * 阅读公告，浏览次数 +1
     * @param id 公告ID
     */
    void incrementViewCount(Integer id);

}
