package com.project.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.pms.entity.po.ChatMessage;
import com.project.pms.entity.vo.PageResult;

/**
 * 聊天消息服务接口（保留简单历史查询，新功能已由 IChatService + IChatDetailedService 承担）
 */
public interface IChatMessageService extends IService<ChatMessage> {

    /**
     * 查询两用户之间的消息历史（分页，按发送时间倒序）
     *
     * @param targetUid 对方用户ID
     * @param pageNo    页码
     * @param pageSize  每页条数
     * @return 分页结果
     */
    PageResult<ChatMessage> getHistory(Integer targetUid, int pageNo, int pageSize);
}
