package com.project.pms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.pms.entity.po.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 分页查询某用户的通知列表（未读优先，按时间倒序）
     */
    @Select("SELECT * FROM notification WHERE receiver_id = #{receiverId} ORDER BY is_read ASC, create_time DESC")
    Page<Notification> selectByReceiver(Page<Notification> page, @Param("receiverId") Integer receiverId);

    /**
     * 查询某用户未读通知数量
     */
    @Select("SELECT COUNT(*) FROM notification WHERE receiver_id = #{receiverId} AND is_read = 0")
    int countUnread(@Param("receiverId") Integer receiverId);

    /**
     * 将某用户所有通知标为已读
     */
    @Update("UPDATE notification SET is_read = 1 WHERE receiver_id = #{receiverId} AND is_read = 0")
    void markAllRead(@Param("receiverId") Integer receiverId);
}
