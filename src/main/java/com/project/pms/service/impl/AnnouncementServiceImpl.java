package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.pms.entity.constants.Constants;
import com.project.pms.entity.po.Announcement;
import com.project.pms.entity.query.AnnouncementQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.enums.ResponseCodeEnum;
import com.project.pms.enums.announcement.AnnouncementPriorityEnum;
import com.project.pms.enums.announcement.AnnouncementTargetRoleEnum;
import com.project.pms.enums.im.NotificationEnum;
import com.project.pms.mapper.AnnouncementMapper;
import com.project.pms.mapper.UserMapper;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.IAnnouncementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.pms.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 公告信息表 服务实现类
 * </p>
 *
 * @author loser
 * @since 2026-02-04
 */
@RequiredArgsConstructor
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements IAnnouncementService {
    private final CurrentUser currentUser;
    private final AnnouncementMapper announcementMapper;
    private final INotificationService notificationService;
    private final UserMapper userMapper;

    /**
     * 添加公告信息
     *
     * @param announcement 公告信息
     * @return 添加结果
     */
    @Override
    public Result addAnnouncement(Announcement announcement) {
        Integer id = currentUser.getUserId();

        if (announcement == null) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "公告信息不能为空！");
        }
        if (announcement.getTitle() == null || announcement.getTitle().isEmpty()) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "公告标题不能为空！");
        }
        if (announcement.getContent() == null || announcement.getContent().isEmpty()) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "公告内容不能为空！");
        }
        if (announcement.getStatus() == null) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "公告状态不能为空！");
        }
        if (announcement.getCategoryId() == null) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "公告分类不能为空！");
        }

        if (announcement.getTargetRole() == null || announcement.getTargetRole().isEmpty()) {
            announcement.setTargetRole(AnnouncementTargetRoleEnum.ALL.getCode());
        }
        // 修复：只在前端未传 priority 时才设默认值，不强制覆盖
        if (announcement.getPriority() == null) {
            announcement.setPriority(AnnouncementPriorityEnum.NORMAL.getCode());
        }
        announcement.setAuthorId(id);
        announcement.setViewCount(Constants.NUMBER_ZERO);
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.insert(announcement);

        // 根据目标角色，查询对应用户并批量发送公告通知
        String notifyContent = "您收到了新的公告：" + announcement.getTitle();
        Integer notifyType = NotificationEnum.ANNOUNCEMENT_PUBLISHED.getType();
        String notifyTitle = NotificationEnum.ANNOUNCEMENT_PUBLISHED.getDesc();
        Integer authorId = announcement.getAuthorId();
        Integer refId = announcement.getId();

        if (announcement.getTargetRole().equals(AnnouncementTargetRoleEnum.ALL.getCode())) {
            // 发给所有学生和教师（role=0 学生，role=1 教师）
            List<Integer> studentIds = userMapper.selectIdsByRole(0);
            List<Integer> teacherIds = userMapper.selectIdsByRole(1);
            List<Integer> allIds = new java.util.ArrayList<>(studentIds);
            allIds.addAll(teacherIds);
            notificationService.batchSend(allIds, authorId, notifyType, notifyTitle, notifyContent, refId);
        } else if (announcement.getTargetRole().equals(AnnouncementTargetRoleEnum.STUDENT.getCode())) {
            // 只发给学生（role=0）
            List<Integer> studentIds = userMapper.selectIdsByRole(0);
            notificationService.batchSend(studentIds, authorId, notifyType, notifyTitle, notifyContent, refId);
        } else if (announcement.getTargetRole().equals(AnnouncementTargetRoleEnum.TEACHER.getCode())) {
            // 只发给教师（role=1）
            List<Integer> teacherIds = userMapper.selectIdsByRole(1);
            notificationService.batchSend(teacherIds, authorId, notifyType, notifyTitle, notifyContent, refId);
        }


        return Result.success("添加公告信息成功！");
    }

    /**
     * 获取公告信息列表
     *
     * @param query 查询参数
     * @return 公告信息列表
     */
    @Override
    public PageResult<Announcement> getAnnouncement(AnnouncementQuery query) {
        // 根据用户角色过滤可见公告（管理员role=2可看所有，学生role=0和教师role=1按角色过滤）
        Integer role = currentUser.getRole();
        if (role != 2) {  // 非管理员
            java.util.List<String> visibleRoles = new java.util.ArrayList<>();
            visibleRoles.add("ALL");  // 全部可见
            if (role == 0) {
                visibleRoles.add("STUDENT");  // 学生额外可见学生公告
            } else if (role == 1) {
                visibleRoles.add("TEACHER");  // 教师额外可见教师公告
            }
            query.setTargetRoles(visibleRoles);
        }

        Page<Announcement> page = Page.of(query.getPageNo(), query.getPageSize());
        announcementMapper.selectAnnouncementList(page, query);
        List<Announcement> list = page.getRecords();
        return PageResult.success(list, (int) page.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * 阅读公告（viewCount +1）
     *
     * @param id 公告ID
     */
    @Override
    public void incrementViewCount(Integer id) {
        announcementMapper.update(null,
                new LambdaUpdateWrapper<Announcement>()
                        .eq(Announcement::getId, id)
                        .setSql("view_count = view_count + 1")
        );
    }

}
