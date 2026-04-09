package com.project.pms.im.handler;

import com.alibaba.fastjson.JSON;
import com.project.pms.entity.constants.Constants;
import com.project.pms.entity.po.Command;
import com.project.pms.entity.po.IMResponse;
import com.project.pms.entity.po.User;
import com.project.pms.im.IMServer;
import com.project.pms.service.IFriendService;
import com.project.pms.utils.JwtUtil;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @className: TokenValidationHandler
 * @description:  Token 验证处理器
 *    前端建立 WS 连接后，第一个帧发送 {"code":100,"content":"Bearer xxx.xxx.xxx"}
 *     验证通过后将 userId 绑定到 Channel 并存入 USER_CHANNEL，然后移除自身（后续帧直接到 WebSocketHandler）
 * @author: loser
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class TokenValidationHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static JwtUtil jwtUtil;
    private static RedisUtil redisUtil;
    private static IFriendService friendService;

    @Autowired
    public void setDependencies(JwtUtil jwtUtil, RedisUtil redisUtil, @Lazy IFriendService friendService) {
        TokenValidationHandler.jwtUtil = jwtUtil;
        TokenValidationHandler.redisUtil = redisUtil;
        TokenValidationHandler.friendService = friendService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame tx) {
        Command command = JSON.parseObject(tx.text(), Command.class);
        String token = command.getContent();

        Integer uid = isValidToken(token);
        if (uid != null) {
            // 1. 将 userId 绑定到 Channel 属性
            ctx.channel().attr(AttributeKey.valueOf("userId")).set(uid);

            // 2. 存入 USER_CHANNEL
            IMServer.USER_CHANNEL.computeIfAbsent(uid, k -> new HashSet<>()).add(ctx.channel());

            // 3. 写入 Redis 在线成员集合
            redisUtil.addMember(Constants.REDIS_KEY_LOGIN_MEMBER, uid);

//            log.info("用户 {} WS 连接成功，该用户连接数: {}", uid,
//                    IMServer.USER_CHANNEL.get(uid).size());

            // 4. 推送在线好友列表给当前用户（online_list）
            try {
                Set<Integer> friendIds = friendService.getFriendIds(uid);
                List<Integer> onlineFriendIds = new ArrayList<>();
                for (Integer fid : friendIds) {
                    if (IMServer.USER_CHANNEL.containsKey(fid)) {
                        onlineFriendIds.add(fid);
                    }
                }
                ctx.channel().writeAndFlush(IMResponse.success("online_list", onlineFriendIds));

                // 5. 向当前用户的在线好友广播「uid 上线了」
                for (Integer fid : onlineFriendIds) {
                    Set<Channel> fChannels = IMServer.USER_CHANNEL.get(fid);
                    if (fChannels != null) {
                        for (Channel ch : fChannels) {
                            ch.writeAndFlush(IMResponse.success("online", uid));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("推送上线状态异常: {}", e.getMessage());
            }

            // 6. 移除 TokenValidationHandler，后续帧直接交给 WebSocketHandler 处理
            ctx.pipeline().remove(TokenValidationHandler.class);

            // 7. 保持消息引用计数并传递给下一个 Handler
            tx.retain();
            ctx.fireChannelRead(tx);
        } else {
            log.warn("WS Token 验证失败，拒绝连接");
            ctx.channel().writeAndFlush(IMResponse.error("登录已过期，请重新登录"));
            ctx.close();
        }
    }

    /**
     * 验证 JWT Token
     * 支持 "Bearer xxx" 和 "xxx" 两种格式
     */
    private Integer isValidToken(String token) {
        if (!StringUtils.hasText(token)) return null;

        // 去掉 Bearer 前缀
        String rawToken = token;
        if (token.startsWith("Bearer ") || token.startsWith(Constants.TOKEN_PREFIX)) {
            rawToken = token.substring(7);
        }

        try {
            if (!jwtUtil.verifyToken(rawToken)) {
                log.error("Token 已过期或无效");
                return null;
            }
            String userId = JwtUtil.getSubjectFromToken(rawToken);
            String role = JwtUtil.getClaimFromToken(rawToken, "role");
            User user = redisUtil.getObject(Constants.REDIS_KEY_SECURITY + role + ":" + userId, User.class);
            if (user == null) {
                log.error("用户未登录或 Session 已过期");
                return null;
            }
            return user.getId();
        } catch (Exception e) {
            log.error("Token 验证异常: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("TokenValidationHandler 异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
