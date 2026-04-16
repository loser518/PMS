package com.project.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.pms.entity.po.FriendRelation;
import com.project.pms.entity.vo.FriendVO;
import com.project.pms.exception.BusinessException;

import java.util.List;
import java.util.Set;

/**
 * 好友服务接口
 */
public interface IFriendService extends IService<FriendRelation> {

    /**
     * 发起好友申请
     *
     * @param toUid   被申请人ID
     * @param message 申请附言（可为空）
     */
    void applyFriend(Integer toUid, String message) throws BusinessException;

    /**
     * 接受好友申请
     *
     * @param relationId 关系ID
     */
    void acceptFriend(Integer relationId) throws BusinessException;

    /**
     * 拒绝好友申请
     *
     * @param relationId 关系ID
     */
    void rejectFriend(Integer relationId);

    /**
     * 删除好友
     *
     * @param friendId 好友用户ID
     */
    void removeFriend(Integer friendId);

    /**
     * 获取当前用户的好友列表
     *
     * @return 好友VO列表
     */
    List<FriendVO> listFriends();

    /**
     * 获取收到的好友申请列表（待处理）
     *
     * @return 申请VO列表
     */
    List<FriendVO> listPendingRequests();

    /**
     * 获取指定用户的所有好友ID集合（status=1）
     *
     * @param uid 用户ID
     * @return 好友ID集合
     */
    Set<Integer> getFriendIds(Integer uid);
}
