package com.project.pms.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * AI API 调用器
 * 支持自定义 API Key（用户个人配额）和系统默认 Key（公共配额）
 */
@Slf4j
@Component
public class AiApiCaller {

    private static final String BASE_URL = "https://api.deepseek.com";
    private static final String MODEL = "deepseek-chat";

    private final WebClient webClient;

    @Value("${spring.ai.deepseek.api-key:}")
    private String systemApiKey;

    public AiApiCaller(WebClient.Builder builder) {
        this.webClient = builder.baseUrl(BASE_URL).build();
    }

    /**
     * 同步调用（用于 AI 辅助写作等单次返回场景）
     *
     * @param apiKey 用户个人 Key（可为空，则用系统默认 Key）
     * @param prompt 用户输入
     * @return AI 回复文本
     */
    public String syncCall(String apiKey, String prompt) {
        String effectiveKey = (apiKey != null && !apiKey.isBlank()) ? apiKey : systemApiKey;

        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 1000
            );

            String result = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + effectiveKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 简单解析：提取 choices[0].message.content
            return parseContentFromResponse(result);
        } catch (Exception e) {
            log.error("AI syncCall 失败 apiKey={} prompt={}", effectiveKey, prompt, e);
            return "AI 服务暂时不可用，请稍后再试。";
        }
    }

    /**
     * 验证 API Key 是否有效
     */
    public boolean validateKey(String apiKey) {
        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "messages", List.of(Map.of("role", "user", "content", "Hi")),
                    "max_tokens", 5
            );

            webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            return true;
        } catch (Exception e) {
            log.warn("API Key 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 简单 JSON 解析（不引入 Jackson 额外依赖，用字符串提取）
     */
    private String parseContentFromResponse(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            int contentStart = json.indexOf("\"content\":\"");
            if (contentStart == -1) return "";
            int start = contentStart + 10; // length of "\"content\":\""
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            String content = json.substring(start, end);
            // unescape \n \" \\
            return content
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        } catch (Exception e) {
            log.warn("解析 AI 响应失败: {}", e.getMessage());
            return "";
        }
    }
}
