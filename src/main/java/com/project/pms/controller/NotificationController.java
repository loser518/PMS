package com.project.pms.controller;

import com.project.pms.entity.po.Notification;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 *
 * @author loser
 * @since 2026-03-14
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final INotificationService notificationService;

    /**
     * 查询当前用户通知列表（分页，未读优先）
     */
    @GetMapping
    public PageResult<Notification> list(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return notificationService.listMyNotifications(pageNo, pageSize);
    }

    /**
     * 查询当前用户未读通知数量
     */
    @GetMapping("/unread")
    public Result countUnread() {
        return Result.success(notificationService.countUnread(), "查询成功");
    }

    /**
     * 将指定通知标为已读
     */
    @PostMapping("/read/{id}")
    public Result markRead(@PathVariable Integer id) {
        notificationService.markRead(id);
        return Result.success("已标记为已读");
    }

    /**
     * 全部标为已读
     */
    @PostMapping("/read/all")
    public Result markAllRead() {
        notificationService.markAllRead();
        return Result.success("全部已读");
    }

    /**
     * 清空所有通知
     */
    @DeleteMapping("/clear")
    public Result clearAll() {
        notificationService.clearAll();
        return Result.success("已清空全部通知");
    }
}
