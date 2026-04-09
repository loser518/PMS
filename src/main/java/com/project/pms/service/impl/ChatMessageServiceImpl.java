package com.project.pms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.pms.entity.po.ChatMessage;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.mapper.ChatMessageMapper;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.IChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天消息服务实现（旧版 chat_message 表的简单历史查询，保留向后兼容）
 */
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements IChatMessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final CurrentUser currentUser;

    @Override
    public PageResult<ChatMessage> getHistory(Integer targetUid, int pageNo, int pageSize) {
        Integer myId = currentUser.getUserId();
        Page<ChatMessage> page = new Page<>(pageNo, pageSize);
        chatMessageMapper.selectHistory(page, myId, targetUid);
        chatMessageMapper.markRead(myId, targetUid);
        List<ChatMessage> list = page.getRecords();
        return PageResult.success(list, (int) page.getTotal(), pageNo, pageSize);
    }
}
