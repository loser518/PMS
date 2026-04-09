package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.pms.entity.po.IMResponse;
import com.project.pms.entity.po.Notification;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.im.IMServer;
import com.project.pms.mapper.NotificationMapper;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.INotificationService;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通知服务实现类
 *
 * @author loser
 * @since 2026-03-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements INotificationService {

    private final NotificationMapper notificationMapper;
    private final CurrentUser currentUser;

    @Override
    public void send(Integer receiverId, Integer senderId, Integer type,
                     String title, String content, Integer refId) {
        Notification notification = Notification.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .type(type)
                .title(title)
                .content(content)
                .refId(refId)
                .isRead(0)
                .createTime(LocalDateTime.now())
                .build();
        notificationMapper.insert(notification);

        // 单条通知也通过 WebSocket 实时推送
        pushNotificationToOnline(List.of(receiverId), type, title, content, refId);
    }

    @Override
    public void batchSend(List<Integer> receiverIds, Integer senderId, Integer type,
                          String title, String content, Integer refId) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Integer receiverId : receiverIds) {
            Notification notification = Notification.builder()
                    .receiverId(receiverId)
                    .senderId(senderId)
                    .type(type)
                    .title(title)
                    .content(content)
                    .refId(refId)
                    .isRead(0)
                    .createTime(now)
                    .build();
            notificationMapper.insert(notification);
        }

        // 通过 WebSocket 实时推送给在线用户
        pushNotificationToOnline(receiverIds, type, title, content, refId);
    }
    public PageResult<Notification> listMyNotifications(int pageNo, int pageSize) {
        Integer uid = currentUser.getUserId();
        Page<Notification> page = new Page<>(pageNo, pageSize);
        notificationMapper.selectByReceiver(page, uid);
        List<Notification> list = page.getRecords();
        return PageResult.success(list, (int) page.getTotal(), pageNo, pageSize);
    }

    @Override
    public int countUnread() {
        return notificationMapper.countUnread(currentUser.getUserId());
    }

    @Override
    public void markRead(Integer id) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getId, id)
                        .eq(Notification::getReceiverId, currentUser.getUserId())
                        .set(Notification::getIsRead, 1)
        );
    }

    @Override
    public void markAllRead() {
        notificationMapper.markAllRead(currentUser.getUserId());
    }

    @Override
    public void clearAll() {
        notificationMapper.delete(
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getReceiverId, currentUser.getUserId())
        );
    }

    /**
     * 通过 WebSocket 实时推送通知给在线用户
     * 推送格式：{ type: "notification", data: { type, title, content, refId } }
     */
    private void pushNotificationToOnline(List<Integer> receiverIds, Integer type,
                                          String title, String content, Integer refId) {
        if (receiverIds == null || receiverIds.isEmpty()) return;

        // 组装推送数据
        Map<String, Object> payload = Map.of(
                "type", type,
                "title", title,
                "content", content,
                "refId", refId != null ? refId : 0
        );

        for (Integer uid : receiverIds) {
            Set<Channel> channels = IMServer.USER_CHANNEL.get(uid);
            if (channels == null) continue;
            for (Channel ch : channels) {
                if (ch.isActive()) {
                    ch.writeAndFlush(IMResponse.success("notification", payload));
                }
            }
        }
    }
}
