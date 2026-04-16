package com.project.pms.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.project.pms.entity.po.AiChatSession;
import com.project.pms.entity.po.SpringAiChatMemory;
import com.project.pms.entity.vo.Result;
import com.project.pms.mapper.AiChatSessionMapper;
import com.project.pms.mapper.SpringAiChatMemoryMapper;
import com.project.pms.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 智能客服接口
 * @author loser
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/customer")
public class CustomerServiceController {

    private final CustomerServiceAssistant assistant;
    private final CurrentUser currentUser;
    private final ChatMemory customerServiceChatMemory;
    private final AiChatSessionMapper aiChatSessionMapper;
    private final SpringAiChatMemoryMapper springAiChatMemoryMapper;

    /**
     * 流式对话接口（SSE）
     * 首次发消息时自动创建会话记录，后续更新活跃时间
     * 支持 X-API-Key header 或 apiKey query param：若传入用户个人 Key，则用该 Key 调用 DeepSeek
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam String sessionId,
            @RequestParam String message,
            @RequestParam(required = false) String apiKey,
            @RequestHeader(value = "X-API-Key", required = false) String headerApiKey) {

        String userApiKey = null;
        if (headerApiKey != null && !headerApiKey.isBlank()) {
            userApiKey = headerApiKey;
        } else if (apiKey != null && !apiKey.isBlank()) {
            userApiKey = apiKey;
        }

        Integer userId = currentUser.getUserId();

        // 自动创建或更新会话记录
        upsertSession(userId, sessionId, message);

        // 若用户传入了个人 API Key，使用直接 HTTP SSE 流式调用
        if (userApiKey != null && !userApiKey.isBlank()) {
            return streamWithCustomKey(userApiKey.trim(), sessionId, userId, message);
        }

        // 否则使用系统默认 ChatClient（含工具调用能力）
        return assistant.chat(sessionId, userId, message);
    }

    /**
     * 使用用户自定义 API Key 做 SSE 流式输出
     */
    private Flux<String> streamWithCustomKey(String apiKey, String sessionId, Integer userId, String message) {
        // 获取历史上下文（最近 N 条）
        List<Message> history = customerServiceChatMemory.get(sessionId);
        List<Map<String, String>> historyMessages;
        if (history != null && !history.isEmpty()) {
            historyMessages = history.stream()
                    .map(msg -> Map.of(
                            "role", msg.getMessageType().getValue().toLowerCase(),
                            "content", msg.getText()))
                    .collect(Collectors.toList());
        } else {
            historyMessages = List.of();
        }

        // 构建系统 prompt + 历史 + 当前消息
        String systemPrompt = """
                你是PMS项目管理系统的智能客服助手，名叫"小P"。
                你的职责是帮助用户解答系统使用问题，以及查询他们的课题申报、进度提交、系统公告等信息。
                请使用中文回答。
                """;
        List<Map<String, String>> allMessages = new java.util.ArrayList<>();
        allMessages.add(Map.of("role", "system", "content", systemPrompt));
        allMessages.addAll(historyMessages);
        allMessages.add(Map.of("role", "user", "content", message));

        String jsonBody;
        try {
            JSONObject bodyObj = new JSONObject();
            bodyObj.put("model", "deepseek-chat");
            bodyObj.put("messages", allMessages);
            bodyObj.put("stream", true);
            jsonBody = bodyObj.toJSONString();
        } catch (Exception e) {
            log.error("序列化请求体失败", e);
            return Flux.just("data: [错误] 请求序列化失败\n\n");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<java.io.InputStream> response = client.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                return Flux.just("data: [错误] API 返回 " + response.statusCode() + ": " + errBody + "\n\n");
            }

            java.io.InputStream is = response.body();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            AtomicInteger seq = new AtomicInteger(0);
            return Flux.create(sink -> {
                String line;
                try {
                    while ((line = reader.readLine()) != null && !sink.isCancelled()) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                sink.complete();
                                break;
                            }
                            // 解析 SSE data: {"choices":[{"delta":{"content":"xxx"}}]}
                            try {
                                JSONObject resp = JSON.parseObject(data);
                                JSONArray choices = resp.getJSONArray("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                                    if (delta != null && delta.containsKey("content")) {
                                        String content = delta.getString("content")
                                                .replace("\\n", "\n")
                                                .replace("\\\"", "\"")
                                                .replace("\\\\", "\\");
                                        // 换行符在 SSE 中是事件分隔符，需要转义
                                        String safeContent = content.replace("\n", "\\n");
                                        sink.next("data: " + safeContent + "\n\n");
                                    }
                                }
                            } catch (Exception ignored) {
                                // 非 JSON 行忽略
                            }
                        }
                    }
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                } finally {
                    try { reader.close(); } catch (Exception ignored) {}
                }
            });
        } catch (Exception e) {
            log.error("用户自定义 API Key 流式调用失败 apiKey={}", apiKey, e);
            return Flux.just("data: [错误] AI 服务调用失败，请检查 API Key 或网络连接\n\n");
        }
    }

    /**
     * 查询当前用户的会话列表（按最后活跃时间倒序）
     * GET /ai/customer/sessions
     */
    @GetMapping("/sessions")
    public List<Map<String, Object>> sessions() {
        Integer userId = currentUser.getUserId();
        List<AiChatSession> list = aiChatSessionMapper.selectList(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .orderByDesc(AiChatSession::getUpdateTime)
        );
        return list.stream().map(s -> Map.<String, Object>of(
                "sessionId", s.getSessionId(),
                "title", s.getTitle(),
                "createTime", s.getCreateTime(),
                "updateTime", s.getUpdateTime()
        )).collect(Collectors.toList());
    }

    /**
     * 更新会话名称
     * POST /ai/customer/sessions/{sessionId}
     *
     * @param sessionId
     * @param title
     * @return
     */
    @PostMapping("/sessions/{sessionId}")
    public Result updateChatName(
            @PathVariable String sessionId,
            @RequestParam("title") String title) {
        Integer userId = currentUser.getUserId();
//        log.info("更新会话名称 sessionId={} userId={} title={}", sessionId, userId, title);
        aiChatSessionMapper.update(null,
                new LambdaUpdateWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getSessionId, sessionId)
                        .set(AiChatSession::getTitle, title)
                        .set(AiChatSession::getUpdateTime, new Date())
        );
        return Result.success("更新会话名称成功！");
    }

    /**
     * 删除指定会话（删除列表记录并删除 memory）
     * DELETE /ai/customer/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(@PathVariable String sessionId) {
        Integer userId = currentUser.getUserId();
        aiChatSessionMapper.delete(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getSessionId, sessionId)
        );
        springAiChatMemoryMapper.delete(
                new LambdaQueryWrapper<SpringAiChatMemory>()
                        .eq(SpringAiChatMemory::getConversationId, sessionId)
        );
    }

    /**
     * 查询会话历史消息
     * GET /ai/customer/history?sessionId=xxx
     */
    @GetMapping("/history")
    public List<Map<String, String>> history(
            @RequestParam String sessionId) {

        Integer userId = currentUser.getUserId();
        // 校验 sessionId 归属当前用户，防止越权
        long count = aiChatSessionMapper.selectCount(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .eq(AiChatSession::getSessionId, sessionId)
        );
        if (count == 0) {
            // sessionId 不存在或不属于当前用户，返回空（首次建立对话时该记录尚未写入也返回空）
            return List.of();
        }

        List<Message> messages = customerServiceChatMemory.get(sessionId);
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
                .map(msg -> Map.of(
                        "role", msg.getMessageType().getValue().toLowerCase(),
                        "content", msg.getText()
                ))
                .collect(Collectors.toList());
    }


    /**
     * 测试 API Key 是否有效
     * POST /ai/customer/test-key
     * body: { "apiKey": "sk-xxxxx" }
     */
    @PostMapping("/test-key")
    public Result testApiKey(@RequestBody Map<String, String> body) {
        String apiKey = body.get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            return Result.error("API Key 不能为空");
        }
        try {
            // 用 Java 11 内置 HttpClient 发一个极简请求到 DeepSeek API 验证 Key
            String jsonBody = """
                {
                    "model": "deepseek-chat",
                    "messages": [{"role":"user","content":"Hi"}],
                    "max_tokens": 5
                }
                """;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return Result.success("API Key 有效");
            } else {
                // 解析错误信息
                String bodyResp = response.body();
                if (bodyResp.contains("incorrect_api_key")) {
                    return Result.error("API Key 格式或内容错误");
                }
                if (bodyResp.contains("quota")) {
                    return Result.error("API Key 配额不足");
                }
                return Result.error("验证失败，状态码：" + response.statusCode());
            }
        } catch (Exception e) {
            log.warn("测试 API Key 失败: {}", e.getMessage());
            return Result.error("连接失败，请检查网络或 API Key");
        }
    }

    /**
     * 若该 sessionId 不存在则插入，否则更新 update_time
     * title 取消息的前 30 个字符
     */
    private void upsertSession(Integer userId, String sessionId, String firstMessage) {
        try {
            AiChatSession existing = aiChatSessionMapper.selectOne(
                    new LambdaQueryWrapper<AiChatSession>()
                            .eq(AiChatSession::getSessionId, sessionId)
                            .last("LIMIT 1")
            );
            if (existing == null) {
                // 首次：插入新会话，title = 消息前30字
                // TODO 可以再次调用AI，让AI根据用户第一次输入内容和第一次回答生成会话标题
                String title = firstMessage.length() > 30
                        ? firstMessage.substring(0, 30) + "…"
                        : firstMessage;
                AiChatSession session = AiChatSession.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .title(title)
                        .createTime(new Date())
                        .updateTime(new Date())
                        .build();
                aiChatSessionMapper.insert(session);
            } else {
                // 已存在：仅刷新 update_time
                aiChatSessionMapper.update(null,
                        new LambdaUpdateWrapper<AiChatSession>()
                                .eq(AiChatSession::getSessionId, sessionId)
                                .set(AiChatSession::getUpdateTime, new Date())
                );
            }
        } catch (Exception e) {
            // 会话记录写入失败不影响正常对话
            log.warn("upsertSession 失败: sessionId={}, err={}", sessionId, e.getMessage());
        }
    }
}
