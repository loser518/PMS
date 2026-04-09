package com.project.pms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.pms.entity.po.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 查询两个用户之间的消息历史（分页，时间倒序）
     */
    @Select("SELECT * FROM chat_message WHERE is_withdrawn = 0 AND " +
            "((sender_id = #{uid1} AND receiver_id = #{uid2}) OR " +
            "(sender_id = #{uid2} AND receiver_id = #{uid1})) " +
            "ORDER BY create_time DESC")
    Page<ChatMessage> selectHistory(Page<ChatMessage> page,
                                    @Param("uid1") Integer uid1,
                                    @Param("uid2") Integer uid2);

    /**
     * 将指定对话的未读消息全部标记为已读
     */
    @Update("UPDATE chat_message SET is_read = 1 WHERE receiver_id = #{receiverId} AND sender_id = #{senderId} AND is_read = 0")
    void markRead(@Param("receiverId") Integer receiverId, @Param("senderId") Integer senderId);

    /**
     * 统计指定用户的未读消息数
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE receiver_id = #{uid} AND is_read = 0 AND is_withdrawn = 0")
    int countUnread(@Param("uid") Integer uid);
}
