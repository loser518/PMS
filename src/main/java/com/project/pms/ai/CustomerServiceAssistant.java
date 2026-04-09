package com.project.pms.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 智能客服核心 Service
 * <p>
 * 特性：
 * 1. 会话记忆（InMemory，同一 sessionId 内保持上下文）
 * 2. Function Calling（自动调用业务工具查询真实数据）
 * 3. 流式响应（SSE，逐字输出，体验更好）
 * @author loser
 */
@Slf4j
@Service
public class CustomerServiceAssistant {

    //构造函数注入 + @Qualifier
    private final ChatClient chatClient;

    public CustomerServiceAssistant(
            @Qualifier("customerServiceChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 流式对话入口
     *
     * @param sessionId 会话ID（前端生成，用于隔离不同用户/不同对话的记忆）
     * @param userId    当前登录用户ID（在 Servlet 主线程取好传入，避免 Tool 执行时跨线程丢失 SecurityContext）
     * @param message   用户消息
     * @return 流式文本
     */
    public Flux<String> chat(String sessionId, Integer userId, String message) {
        return chatClient.prompt()
                // 将 userId 注入 system 上下文，Tool 通过参数直接使用，无需再碰 SecurityContext
                .system(s -> s.param("userId", String.valueOf(userId)))
                .user(message)
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        sessionId))
                .stream()
                .content()
                // SSE 协议中换行符会被解析为事件分隔符，需转义为字面量 \n
                .map(chunk -> chunk.replace("\n", "\\n"))
                .doOnError(e -> log.error("智能客服调用失败 sessionId={} userId={}", sessionId, userId, e));
    }
}
