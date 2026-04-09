package com.project.pms.service;

import java.util.Map;

/**
 * 聊天消息详情服务
 */
public interface IChatDetailedService {

    /**
     * 获取两人之间的消息（每次20条，按时间倒序）
     * @param uid  对方uid
     * @param aid  自己uid
     * @param offset 偏移量（已查过几条）
     * @return {list: List<ChatDetailed>, more: boolean}
     */
    Map<String, Object> getDetails(Integer uid, Integer aid, Long offset);

    /**
     * 删除单条消息（逻辑删除，只对自己生效）
     */
    boolean deleteDetail(Integer id, Integer uid);
}
