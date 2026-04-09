package com.project.pms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @className: WebSocketConfig
 * @description: WebSocket配置类
 * @author: loser
 * @createTime: 2026/2/9 20:02
 */
@Configuration
public class WebSocketConfig {

    /**
     * 注册一个ServerEndpointExporter，该Bean会自动注册使用@ServerEndpoint注解声明的websocket endpoint
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
