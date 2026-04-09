package com.project.pms.im.handler;

import com.alibaba.fastjson.JSON;
import com.project.pms.entity.constants.Constants;
import com.project.pms.entity.po.Command;
import com.project.pms.entity.po.IMResponse;
import com.project.pms.enums.im.CommandTypeEnum;
import com.project.pms.im.IMServer;
import com.project.pms.service.IFriendService;
import com.project.pms.utils.RedisUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * WebSocket 消息分发处理器
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static RedisUtil redisUtil;
    private static IFriendService friendService;

    @Autowired
    public void setDependencies(RedisUtil redisUtil, @Lazy IFriendService friendService) {
        WebSocketHandler.redisUtil = redisUtil;
        WebSocketHandler.friendService = friendService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame tx) {
        try {
            Command command = JSON.parseObject(tx.text(), Command.class);
            switch (CommandTypeEnum.getByCode(command.getCode())) {
                case CONNECTION:
                    break;
                case CHAT_SEND:
                    ChatHandler.send(ctx, tx);
                    break;
                case CHAT_WITHDRAW:
                    ChatHandler.withdraw(ctx, tx);
                    break;
                default:
                    ctx.channel().writeAndFlush(IMResponse.error("不支持的 CODE: " + command.getCode()));
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 连接断开时：
     * 1. 从 USER_CHANNEL 移除该 Channel
     * 2. 若该用户已无在线 Channel，清理 Redis 在线状态和 whisper 窗口状态
     * 3. 向该用户的在线好友广播 offline 事件
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Integer uid = (Integer) ctx.channel().attr(AttributeKey.valueOf("userId")).get();
        if (uid == null) {
            ctx.fireChannelInactive();
            return;
        }
        Set<Channel> userChannels = IMServer.USER_CHANNEL.get(uid);
        if (userChannels != null) {
            userChannels.remove(ctx.channel());
            if (userChannels.isEmpty()) {
                IMServer.USER_CHANNEL.remove(uid);
                // 清除全部聊天窗口在线标记（whisper:uid:xxx）
                redisUtil.deleteKeysWithPrefix("whisper:" + uid + ":");
                // 从在线成员集合移除
                redisUtil.delMember(Constants.REDIS_KEY_LOGIN_MEMBER, uid);
                log.info("用户 {} 已离线，当前在线人数: {}", uid, IMServer.USER_CHANNEL.size());

                // 向该用户的在线好友广播 offline 事件
                try {
                    Set<Integer> friendIds = friendService.getFriendIds(uid);
                    for (Integer fid : friendIds) {
                        Set<Channel> fChannels = IMServer.USER_CHANNEL.get(fid);
                        if (fChannels != null) {
                            for (Channel ch : fChannels) {
                                ch.writeAndFlush(IMResponse.success("offline", uid));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("广播下线状态异常: {}", e.getMessage());
                }
            }
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocketHandler 异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
