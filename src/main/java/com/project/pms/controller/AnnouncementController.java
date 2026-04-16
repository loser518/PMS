package com.project.pms.controller;

import com.project.pms.entity.po.Announcement;
import com.project.pms.entity.query.AnnouncementQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.service.IAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @className: AnnouncementController
 * @description: 公告信息控制类
 * @author: loser
 * @createTime: 2026/2/4 21:13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/announcement")
public class AnnouncementController {

    private final IAnnouncementService announcementService;

    /**
     * 添加公告信息
     *
     * @param announcement 公告信息
     * @return 添加结果
     */
    @PostMapping
    public Result addAnnouncement(@RequestBody Announcement announcement) {
        return announcementService.addAnnouncement(announcement);
    }

    /**
     * 获取公告信息列表
     *
     * @param query 查询参数
     * @return 公告信息列表
     */
    @GetMapping
    public PageResult<Announcement> getAnnouncement(AnnouncementQuery query) {
        return announcementService.getAnnouncement(query);
    }

    /**
     * 修改公告信息
     *
     * @param announcement 公告信息
     * @return 修改结果
     */
    @PostMapping("/update")
    public Result updateAnnouncement(@RequestBody Announcement announcement) {
        announcementService.updateById(announcement);
        return Result.success("修改公告信息成功！");
    }

    /**
     * 阅读公告（viewCount +1）
     *
     * @param id 公告ID
     * @return 操作结果
     */
    @PostMapping("/view/{id}")
    public Result viewAnnouncement(@PathVariable Integer id) {
        announcementService.incrementViewCount(id);
        return Result.success("已记录阅读");
    }

    /**
     * 删除公告信息
     *
     * @param ids 公告信息ID
     * @return 删除结果
     */
    @DeleteMapping
    public Result deleteAnnouncement(@RequestBody List<Integer> ids) {
        announcementService.removeByIds(ids);
        return Result.success("删除公告信息成功！");
    }
}
