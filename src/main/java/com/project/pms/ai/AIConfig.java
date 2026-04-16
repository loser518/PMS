package com.project.pms.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * AI 配置类
 */
@Configuration
public class AIConfig {

    /**
     * 通用 ChatClient（辅助写作等单次调用场景）
     */
    @Bean
    @Qualifier("chatClient")
    public ChatClient chatClient(DeepSeekChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 智能客服专用会话记忆（JDBC 持久化，存入 MySQL）
     * 保留最近 20 条消息作为上下文窗口
     */
    @Bean
    public ChatMemory customerServiceChatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    /**
     * 智能客服专用 ChatClient
     * - 固定系统人设提示词
     * - 绑定 JDBC 持久化会话记忆
     * - 注册业务工具（Function Calling）
     */
    @Bean
    @Qualifier("customerServiceChatClient")
    public ChatClient customerServiceChatClient(
            DeepSeekChatModel chatModel,
            ChatMemory customerServiceChatMemory,
            CustomerServiceTools customerServiceTools) {

        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是PMS项目管理系统的智能客服助手，名叫"小P"。
                        你的职责是帮助用户解答系统使用问题，以及查询他们的课题申报、进度提交、系统公告等信息。
                        
                        当前登录用户ID：{userId}
                        调用需要查询当前用户数据的工具时（如 getMyProjects），请将 userId={userId} 作为参数传入。
                       
                        
                        回复要求：
                        1. 语气亲切、专业，不要过于正式
                        2. 回复简洁明了，重点突出
                        3. 遇到需要查询数据的问题，优先调用工具获取真实数据再回答
                        4. 如果用户询问与系统无关的问题，礼貌地引导回系统相关话题
                        5. 使用中文回答
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(customerServiceChatMemory).build()
                )
                .defaultTools(customerServiceTools)
                .build();
    }

    /**
     * 同步请求客户端
     */
    @Bean
    public RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * 异步客户端（支持流式响应）
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)
                )
                .build();
    }
}