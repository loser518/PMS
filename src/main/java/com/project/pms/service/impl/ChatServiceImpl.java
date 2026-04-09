package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.project.pms.entity.po.Chat;
import com.project.pms.entity.po.IMResponse;
import com.project.pms.im.IMServer;
import com.project.pms.mapper.ChatMapper;
import com.project.pms.mapper.UserMapper;
import com.project.pms.service.IChatDetailedService;
import com.project.pms.service.IChatService;
import com.project.pms.service.UserInfoService;
import com.project.pms.utils.RedisUtil;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 聊天会话服务实现
 *   chat 表中每对用户有两条记录：
 *     (userId=A, anotherId=B)  → A 收到来自 B 的消息的状态（A 的视角）
 *     (userId=B, anotherId=A)  → B 收到来自 A 的消息的状态（B 的视角）
 *
 *   updateChat(from=发送者, to=接收者) 中：
 *     - to -> from 的记录：更新接收者 to 看到来自 from 的会话状态（时间+未读）
 *     - from -> to 的记录：更新发送者 from 看到来自 to 的会话（时间，不加未读）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

    private final ChatMapper chatMapper;
    private final UserMapper userMapper;
    private final UserInfoService userInfoService;
    private final IChatDetailedService chatDetailedService;
    private final RedisUtil redisUtil;

    /**
     * 创建/恢复聊天会话
     * 调用方：我点击"和对方聊天" → from=对方uid, to=我的uid
     */
    @Override
    public Map<String, Object> createChat(Integer from, Integer to) {
        Map<String, Object> map = new HashMap<>();
        QueryWrapper<Chat> qw = new QueryWrapper<Chat>().eq("user_id", to).eq("another_id", from);
        Chat chat = chatMapper.selectOne(qw);

        if (chat != null) {
            if (chat.getIsDeleted() == 1) {
                // 曾经删除过，重新激活
                chat.setIsDeleted(0);
                chat.setLatestTime(new Date());
                chatMapper.updateById(chat);
                redisUtil.zset("chat_zset:" + to, chat.getId());
                // 填充返回数据
                fillChatData(map, chat, from, to);
                map.put("msg", "新创建");
            } else {
                map.put("msg", "已存在");
            }
            return map;
        }

        // 首次创建：先检查对方用户是否存在
        if (userMapper.selectById(from) == null) {
            map.put("msg", "未知用户");
            return map;
        }

        chat = new Chat(null, to, from, 0, 0, new Date());
        chatMapper.insert(chat);
        redisUtil.zset("chat_zset:" + to, chat.getId());

        fillChatData(map, chat, from, to);
        map.put("msg", "新创建");
        return map;
    }

    private void fillChatData(Map<String, Object> map, Chat chat, Integer from, Integer to) {
        map.put("chat", chat);
        Chat finalChat = chat;
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(() ->
                map.put("user", userInfoService.getOneUserInfo(from)));
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(() ->
                map.put("detail", chatDetailedService.getDetails(from, to, 0L)));
        f1.join();
        f2.join();
    }


    @Override
    public List<Map<String, Object>> getChatListWithData(Integer uid, Long offset) {
        Set<Object> set = redisUtil.zReverange("chat_zset:" + uid, offset, offset + 9);
        if (set == null || set.isEmpty()) return Collections.emptyList();

        QueryWrapper<Chat> qw = new QueryWrapper<Chat>()
                .in("id", set).eq("is_deleted", 0).orderByDesc("latest_time");
        List<Chat> chatList = chatMapper.selectList(qw);
        if (chatList == null || chatList.isEmpty()) return Collections.emptyList();

        return chatList.stream().parallel().map(chat -> {
            Map<String, Object> m = new HashMap<>();
            m.put("chat", chat);
            CompletableFuture<Void> f1 = CompletableFuture.runAsync(() ->
                    m.put("user", userInfoService.getOneUserInfo(chat.getAnotherId())));
            CompletableFuture<Void> f2 = CompletableFuture.runAsync(() ->
                    m.put("detail", chatDetailedService.getDetails(chat.getAnotherId(), uid, 0L)));
            f1.join();
            f2.join();
            return m;
        }).collect(Collectors.toList());
    }


    @Override
    public Chat getChat(Integer from, Integer to) {
        return chatMapper.selectOne(new QueryWrapper<Chat>()
                .eq("user_id", to).eq("another_id", from));
    }


    @Override
    public void delChat(Integer from, Integer to) {
        Chat chat = chatMapper.selectOne(new QueryWrapper<Chat>()
                .eq("user_id", to).eq("another_id", from));
        if (chat == null) return;

        // 通知自己所有在线设备移除该会话
        if (IMServer.USER_CHANNEL.containsKey(to)) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "移除");
            msg.put("id", chat.getId());
            msg.put("count", chat.getUnread());
            for (Channel ch : IMServer.USER_CHANNEL.get(to)) {
                if (ch.isActive()) ch.writeAndFlush(IMResponse.success("whisper", msg));
            }
        }

        // 伪删除 + 清未读
        chatMapper.update(null, new UpdateWrapper<Chat>()
                .eq("user_id", to).eq("another_id", from)
                .set("is_deleted", 1).set("unread", 0));

        try {
            redisUtil.zsetDelMember("chat_zset:" + to, chat.getId());
        } catch (Exception e) {
            log.error("Redis 移除聊天失败: {}", e.getMessage());
        }
    }


    /**
     * 发送消息后更新双方会话记录
     * @param from 发消息的人（自己）
     * @param to   收消息的人（对方）
     * @return 对方是否在聊天窗口
     */
    @Override
    public boolean updateChat(Integer from, Integer to) {
        // whisper:接收方:发送方 → 接收方打开了发送方的聊天窗口时存在
        String whisperKey = "whisper:" + to + ":" + from;
        boolean online = redisUtil.isExist(whisperKey);

        try {
            // 更新 from(收消息方视角) → 即 (userId=from, anotherId=to) 的记录：时间+可见
            CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> {
                QueryWrapper<Chat> qw = new QueryWrapper<Chat>()
                        .eq("user_id", from).eq("another_id", to);
                Chat c = chatMapper.selectOne(qw);
                UpdateWrapper<Chat> uw = new UpdateWrapper<Chat>()
                        .eq("user_id", from).eq("another_id", to)
                        .set("is_deleted", 0).set("latest_time", new Date());
                chatMapper.update(null, uw);
                if (c != null) redisUtil.zset("chat_zset:" + from, c.getId());
            });

            // 更新 to(收消息方视角) → 即 (userId=to, anotherId=from) 的记录：时间+未读
            CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> {
                QueryWrapper<Chat> qw = new QueryWrapper<Chat>()
                        .eq("user_id", to).eq("another_id", from);
                Chat c = chatMapper.selectOne(qw);

                if (online) {
                    // 对方在窗口，不加未读
                    if (c == null) {
                        c = new Chat(null, to, from, 0, 0, new Date());
                        chatMapper.insert(c);
                    } else {
                        chatMapper.update(null, new UpdateWrapper<Chat>()
                                .eq("id", c.getId())
                                .set("is_deleted", 0).set("latest_time", new Date()));
                    }
                } else {
                    // 对方不在窗口，未读+1
                    if (c == null) {
                        c = new Chat(null, to, from, 0, 1, new Date());
                        chatMapper.insert(c);
                    } else {
                        chatMapper.update(null, new UpdateWrapper<Chat>()
                                .eq("id", c.getId())
                                .set("is_deleted", 0)
                                .setSql("unread = unread + 1")
                                .set("latest_time", new Date()));
                    }
                }
                Chat finalC = c;
                redisUtil.zset("chat_zset:" + to, finalC.getId());
            });

            f1.join();
            f2.join();
        } catch (Exception e) {
            log.error("updateChat 失败: {}", e.getMessage());
        }
        return online;
    }


    /**
     * 进入聊天窗口：设置在线标记，清除未读，通知自己所有设备
     */
    @Override
    public void updateWhisperOnline(Integer from, Integer to) {
        try {
            // 设置在线标记（key 存在 = 在窗口）
            redisUtil.setValue("whisper:" + to + ":" + from, true);

            // 查询未读数
            Chat chat = chatMapper.selectOne(new QueryWrapper<Chat>()
                    .eq("user_id", to).eq("another_id", from));
            if (chat == null) return;

            if (chat.getUnread() > 0) {
                // 通知自己所有 channel 把该会话未读清零
                Set<Channel> myChannels = IMServer.USER_CHANNEL.get(to);
                if (myChannels != null) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("type", "已读");
                    msg.put("id", chat.getId());
                    msg.put("count", chat.getUnread());
                    for (Channel ch : myChannels) {
                        if (ch.isActive()) ch.writeAndFlush(IMResponse.success("whisper", msg));
                    }
                }
                // 清库中未读
                chatMapper.update(null, new UpdateWrapper<Chat>()
                        .eq("user_id", to).eq("another_id", from).set("unread", 0));
            }
        } catch (Exception e) {
            log.error("updateWhisperOnline 失败: {}", e.getMessage());
        }
    }


    /**
     * 离开聊天窗口：删除在线标记
     */
    @Override
    public void updateWhisperOutline(Integer from, Integer to) {
        try {
            redisUtil.delValue("whisper:" + to + ":" + from);
        } catch (Exception e) {
            log.error("updateWhisperOutline 失败: {}", e.getMessage());
        }
    }
}
