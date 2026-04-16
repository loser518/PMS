package com.project.pms.im;

import com.project.pms.im.handler.TokenValidationHandler;
import com.project.pms.im.handler.WebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @className: IMServer
 * @description: IM服务
 * @author: loser
 * @createTime: 2026/2/10 10:21
 */
@Component
public class IMServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(IMServer.class);

    // 存储每个用户的全部连接
    public static final Map<Integer, Set<Channel>> USER_CHANNEL = new ConcurrentHashMap<>();

    // 存储群组
    public static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // EventLoopGroup：线程池 + 事件循环     NIO（非阻塞）
    // bossGroup：负责处理客户端连接，accept事件（不处理具体业务）
    private static EventLoopGroup bossGroup = new NioEventLoopGroup();
    // workerGroup：负责处理客户端业务，read/write事件
    private static EventLoopGroup workerGroup = new NioEventLoopGroup();

    @Value("${ws.port}")
    private int port;

    @Resource
    private WebSocketHandler webSocketHandler;

    @Resource
    private TokenValidationHandler tokenValidationHandler;

    @PreDestroy
    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void run() {
        // 绑定端口
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline(); //流水线
                        pipeline.addLast(new HttpServerCodec())
                                .addLast(new ChunkedWriteHandler())
                                .addLast(new HttpObjectAggregator(1024 * 64))
                                // WS 升级（HTTP -> WebSocket）
                                .addLast(new WebSocketServerProtocolHandler("/im"))
                                // 第一帧做 Token 验证，验证通过后将自身从 pipeline 移除
                                .addLast(tokenValidationHandler)
                                // 后续帧由业务处理器处理
                                .addLast(webSocketHandler);
                    }
                });
        try {
            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("启动netty成功，端口:{}", port);
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("启动netty失败:", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
