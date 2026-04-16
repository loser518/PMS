package com.project.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.pms.entity.po.Notification;
import com.project.pms.entity.vo.PageResult;

import java.util.List;

/**
 * 通知服务接口
 */
public interface INotificationService extends IService<Notification> {

    /**
     * 发送通知给指定用户
     *
     * @param receiverId 接收者ID
     * @param senderId   发送者ID（为null表示系统消息）
     * @param type       通知类型（见 Notification.type 注释）
     * @param title      通知标题
     * @param content    通知正文
     * @param refId      关联业务ID（可为null）
     */
    void send(Integer receiverId, Integer senderId, Integer type,
              String title, String content, Integer refId);

    /**
     * 批量发送通知给多个用户
     *
     * @param receiverIds 接收者ID列表
     * @param senderId    发送者ID（为null表示系统消息）
     * @param type        通知类型
     * @param title       通知标题
     * @param content     通知正文
     * @param refId       关联业务ID（可为null）
     */
    void batchSend(List<Integer> receiverIds, Integer senderId, Integer type,
                   String title, String content, Integer refId);

    /**
     * 查询当前登录用户的通知列表（分页，未读优先）
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<Notification> listMyNotifications(int pageNo, int pageSize);

    /**
     * 查询当前登录用户未读通知数量
     *
     * @return 未读数量
     */
    int countUnread();

    /**
     * 将某条通知标为已读
     *
     * @param id 通知ID
     */
    void markRead(Integer id);

    /**
     * 将当前用户所有通知标为已读
     */
    void markAllRead();

    /**
     * 清空当前用户的所有通知
     */
    void clearAll();
}
