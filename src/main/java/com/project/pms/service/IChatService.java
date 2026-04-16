package com.project.pms.service;

import com.project.pms.entity.po.Chat;

import java.util.List;
import java.util.Map;

/**
 * 聊天会话服务
 */
public interface IChatService {

    /**
     * 创建或恢复聊天会话
     * from = 对方(发消息方), to = 自己(收消息方)
     * @return map包含 msg("新创建"/"已存在"/"未知用户") 以及 chat/user/detail 数据
     */
    Map<String, Object> createChat(Integer from, Integer to);

    /**
     * 获取当前用户最近聊天列表（含用户信息和最近消息，每次10条）
     */
    List<Map<String, Object>> getChatListWithData(Integer uid, Long offset);

    /**
     * 获取单条聊天会话
     */
    Chat getChat(Integer from, Integer to);

    /**
     * 移除聊天并清除未读
     * from = 对方, to = 自己
     */
    void delChat(Integer from, Integer to);

    /**
     * 发送消息时更新会话的未读数和时间
     * @return 对方是否在聊天窗口
     */
    boolean updateChat(Integer from, Integer to);

    /**
     * 进入聊天窗口：标记在线，清除该会话未读
     */
    void updateWhisperOnline(Integer from, Integer to);

    /**
     * 离开聊天窗口：标记离线
     */
    void updateWhisperOutline(Integer from, Integer to);
}
