package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.project.pms.entity.po.ChatDetailed;
import com.project.pms.mapper.ChatDetailedMapper;
import com.project.pms.service.IChatDetailedService;
import com.project.pms.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 聊天消息详情服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatDetailedServiceImpl implements IChatDetailedService {

    private final ChatDetailedMapper chatDetailedMapper;
    private final RedisUtil redisUtil;

    /**
     * 获取两人聊天记录（20条，倒序分页）
     * Redis ZSet key: "chat_detailed_zset:uid:aid"（score=时间戳，member=消息id）
     */
    @Override
    public Map<String, Object> getDetails(Integer uid, Integer aid, Long offset) {
        String key = "chat_detailed_zset:" + uid + ":" + aid;
        Map<String, Object> map = new HashMap<>();
        Long total = redisUtil.zCard(key);
        map.put("more", offset + 20 < total);

        Set<Object> set = redisUtil.zReverange(key, offset, offset + 19);
        if (set == null || set.isEmpty()) {
            map.put("list", Collections.emptyList());
            return map;
        }
        QueryWrapper<ChatDetailed> qw = new QueryWrapper<>();
        qw.in("id", set).orderByDesc("time");
        map.put("list", chatDetailedMapper.selectList(qw));
        return map;
    }

    /**
     * 逻辑删除单条消息（仅对发起方或接收方自己生效）
     */
    @Override
    public boolean deleteDetail(Integer id, Integer uid) {
        try {
            ChatDetailed detail = chatDetailedMapper.selectById(id);
            if (detail == null) return false;

            UpdateWrapper<ChatDetailed> uw = new UpdateWrapper<>();
            if (Objects.equals(detail.getUserId(), uid)) {
                // 发送者删除
                uw.eq("id", id).setSql("user_del = 1");
                chatDetailedMapper.update(null, uw);
                redisUtil.zsetDelMember("chat_detailed_zset:" + detail.getAnotherId() + ":" + uid, id);
                return true;
            } else if (Objects.equals(detail.getAnotherId(), uid)) {
                // 接收者删除
                uw.eq("id", id).setSql("another_del = 1");
                chatDetailedMapper.update(null, uw);
                redisUtil.zsetDelMember("chat_detailed_zset:" + detail.getUserId() + ":" + uid, id);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("删除消息记录时出错: {}", e.getMessage());
            return false;
        }
    }
}
