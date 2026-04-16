package com.project.pms.controller;

import com.project.pms.entity.vo.Result;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.IChatService;
import com.project.pms.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @className: ChatController
 * @description:  聊天相关
 * @author: loser
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/msg/chat")
public class ChatController {

    private final IChatService chatService;
    private final CurrentUser currentUser;
    private final RedisUtil redisUtil;

    /**
     * 新建/恢复聊天会话（点击"私聊"时调用）
     * @param uid 对方用户ID
     */
    @GetMapping("/create/{uid}")
    public Result createChat(@PathVariable Integer uid) {
        Integer myId = currentUser.getUserId();
        Map<String, Object> result = chatService.createChat(uid, myId);
        String msg = result.get("msg").toString();
        if (Objects.equals(msg, "未知用户")) {
            return Result.error(404, "用户不存在");
        }
        if (Objects.equals(msg, "新创建")) {
            return Result.success(result, "聊天创建成功");
        }
        // 已存在
        return Result.success(null, "已存在");
    }

    /**
     * 获取最近聊天列表（含用户信息和最近消息，分页 10 条）
     * @param offset 偏移量（已加载了几条）
     */
    @GetMapping("/recent-list")
    public Result getRecentList(@RequestParam(defaultValue = "0") Long offset) {
        Integer myId = currentUser.getUserId();
        Map<String, Object> map = new HashMap<>();
        map.put("list", chatService.getChatListWithData(myId, offset));
        // 是否还有更多
        map.put("more", offset + 10 < redisUtil.zCard("chat_zset:" + myId));
        return Result.success(map, "获取成功");
    }

    /**
     * 移除聊天会话
     * @param uid 对方用户ID
     */
    @DeleteMapping("/delete/{uid}")
    public Result deleteChat(@PathVariable Integer uid) {
        chatService.delChat(uid, currentUser.getUserId());
        return Result.success("已移除");
    }

    /**
     * 进入聊天窗口：设置在线状态 + 清除该会话未读
     * @param from 对方UID
     */
    @GetMapping("/online")
    public Result updateWhisperOnline(@RequestParam Integer from) {
        chatService.updateWhisperOnline(from, currentUser.getUserId());
        return Result.success("已标记在线");
    }

    /**
     * 离开聊天窗口：清除在线状态
     * 注意：此接口不需要鉴权，防止 token 过期导致一直显示在线
     * （需要在 SecurityConfig 中放开此路径）
     * @param from  对方UID
     * @param to    自己UID
     */
    @GetMapping("/outline")
    public void updateWhisperOutline(@RequestParam Integer from, @RequestParam Integer to) {
        chatService.updateWhisperOutline(from, to);
    }
}
