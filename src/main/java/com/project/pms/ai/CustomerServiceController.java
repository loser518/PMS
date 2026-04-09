package com.project.pms.ai;

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
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam String sessionId,
            @RequestParam String message) {

        Integer userId = currentUser.getUserId();
//        log.info("智能客服请求 sessionId={} userId={} message={}", sessionId, userId, message);

        // 自动创建或更新会话记录
        upsertSession(userId, sessionId, message);

        return assistant.chat(sessionId, userId, message);
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
