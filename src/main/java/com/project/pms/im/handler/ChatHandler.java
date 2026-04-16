package com.project.pms.im.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.project.pms.entity.po.ChatDetailed;
import com.project.pms.entity.po.Command;
import com.project.pms.entity.po.IMResponse;
import com.project.pms.im.IMServer;
import com.project.pms.mapper.ChatDetailedMapper;
import com.project.pms.service.IChatService;
import com.project.pms.service.UserInfoService;
import com.project.pms.utils.RedisUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @className: ChatHandler
 * @description: 聊天处理器
 * @author: loser
 */
@Slf4j
@Component
public class ChatHandler {

    private static IChatService chatService;
    private static ChatDetailedMapper chatDetailedMapper;
    private static UserInfoService userInfoService;
    private static RedisUtil redisUtil;

    @Autowired
    public void setDependencies(
            @Lazy IChatService chatService,
            ChatDetailedMapper chatDetailedMapper,
            UserInfoService userInfoService,
            RedisUtil redisUtil) {
        ChatHandler.chatService = chatService;
        ChatHandler.chatDetailedMapper = chatDetailedMapper;
        ChatHandler.userInfoService = userInfoService;
        ChatHandler.redisUtil = redisUtil;
    }


    /**
     * 发送消息
     * 前端 WebSocket 帧格式：{"code":101, "content":"{\"anotherId\":对方uid,\"content\":\"消息内容\"}"}
     */
    public static void send(ChannelHandlerContext ctx, TextWebSocketFrame tx) {
        try {
            Command command = JSON.parseObject(tx.text(), Command.class);
            JSONObject body = JSON.parseObject(command.getContent());

            Integer fromUid = (Integer) ctx.channel().attr(AttributeKey.valueOf("userId")).get();
            Integer toUid = body.getInteger("anotherId");
            String content = body.getString("content");

            if (fromUid == null || toUid == null) {
                ctx.channel().writeAndFlush(IMResponse.error("非法请求：用户未认证或目标用户为空"));
                return;
            }

            // 1. 持久化消息
            ChatDetailed detail = new ChatDetailed();
            detail.setUserId(fromUid);
            detail.setAnotherId(toUid);
            detail.setContent(content);
            detail.setUserDel(0);
            detail.setAnotherDel(0);
            detail.setWithdraw(0);
            detail.setTime(new Date());
            chatDetailedMapper.insert(detail);

            // 2. 写入 Redis ZSet（双向，发送方和接收方各一条）
            redisUtil.zset("chat_detailed_zset:" + fromUid + ":" + toUid, detail.getId());
            redisUtil.zset("chat_detailed_zset:" + toUid + ":" + fromUid, detail.getId());

            // 3. 更新会话未读/时间，返回对方是否在窗口
            boolean online = chatService.updateChat(fromUid, toUid);

            // 4. 组装推送数据（并行查询）
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("type", "接收");
            pushData.put("online", online);
            pushData.put("detail", detail);

            CompletableFuture<Void> f1 = CompletableFuture.runAsync(() ->
                    pushData.put("chat", chatService.getChat(fromUid, toUid)));
            CompletableFuture<Void> f2 = CompletableFuture.runAsync(() ->
                    pushData.put("user", userInfoService.getOneUserInfo(fromUid)));
            f1.join();
            f2.join();

            // 5. 推送给发送方和接收方的所有在线 Channel
            pushToUser(fromUid, pushData);
            pushToUser(toUid, pushData);

        } catch (Exception e) {
            log.error("发送聊天消息时出错: {}", e.getMessage(), e);
            ctx.channel().writeAndFlush(IMResponse.error("发送消息失败，请重试"));
        }
    }


    /**
     * 撤回消息（2分钟内可撤回）
     * 前端 WebSocket 帧格式：{"code":102, "content":"{\"id\":消息id}"}
     */
    public static void withdraw(ChannelHandlerContext ctx, TextWebSocketFrame tx) {
        try {
            Command command = JSON.parseObject(tx.text(), Command.class);
            JSONObject body = JSON.parseObject(command.getContent());
            Integer msgId = body.getInteger("id");
            Integer fromUid = (Integer) ctx.channel().attr(AttributeKey.valueOf("userId")).get();

            if (fromUid == null || msgId == null) {
                ctx.channel().writeAndFlush(IMResponse.error("撤回失败：参数不完整"));
                return;
            }

            ChatDetailed detail = chatDetailedMapper.selectById(msgId);
            if (detail == null) {
                ctx.channel().writeAndFlush(IMResponse.error("消息不存在"));
                return;
            }
            if (!Objects.equals(detail.getUserId(), fromUid)) {
                ctx.channel().writeAndFlush(IMResponse.error("无权撤回此消息"));
                return;
            }
            long diff = System.currentTimeMillis() - detail.getTime().getTime();
            if (diff > 120_000L) {
                ctx.channel().writeAndFlush(IMResponse.error("发送时间超过两分钟，无法撤回"));
                return;
            }

            // 标记撤回
            detail.setWithdraw(1);
            chatDetailedMapper.updateById(detail);

            // 推送撤回通知
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("type", "撤回");
            pushData.put("sendId", detail.getUserId());
            pushData.put("acceptId", detail.getAnotherId());
            pushData.put("id", msgId);

            pushToUser(fromUid, pushData);
            pushToUser(detail.getAnotherId(), pushData);
        } catch (Exception e) {
            log.error("撤回消息时出错: {}", e.getMessage(), e);
            ctx.channel().writeAndFlush(IMResponse.error("撤回消息失败，请重试"));
        }
    }


    private static void pushToUser(Integer uid, Map<String, Object> data) {
        Set<Channel> channels = IMServer.USER_CHANNEL.get(uid);
        if (channels == null) return;
        for (Channel ch : channels) {
            if (ch.isActive()) {
                ch.writeAndFlush(IMResponse.success("whisper", data));
            }
        }
    }
}
