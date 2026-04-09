package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.pms.entity.po.FriendRelation;
import com.project.pms.entity.po.User;
import com.project.pms.entity.vo.FriendVO;
import com.project.pms.enums.ResponseCodeEnum;
import com.project.pms.enums.im.NotificationEnum;
import com.project.pms.exception.BusinessException;
import com.project.pms.mapper.FriendRelationMapper;
import com.project.pms.mapper.UserMapper;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.IFriendService;
import com.project.pms.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 好友服务实现类
 *
 * @author loser
 * @since 2026-03-14
 */
@Service
@RequiredArgsConstructor
public class FriendServiceImpl extends ServiceImpl<FriendRelationMapper, FriendRelation>
        implements IFriendService {

    private final FriendRelationMapper friendRelationMapper;
    private final UserMapper userMapper;
    private final CurrentUser currentUser;
    @Lazy
    private final INotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyFriend(Integer toUid, String message) throws BusinessException {
        Integer myId = currentUser.getUserId();
        if (myId.equals(toUid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "不能添加自己为好友");
        }
        if (userMapper.selectById(toUid) == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "目标用户不存在");
        }
        if (friendRelationMapper.existsRelation(myId, toUid) > 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "已发送申请或已是好友");
        }
        FriendRelation relation = new FriendRelation();
        relation.setApplicantId(myId);
        relation.setRecipientId(toUid);
        relation.setStatus(0);
        relation.setMessage(message);
        relation.setCreateTime(LocalDateTime.now());
        relation.setUpdateTime(LocalDateTime.now());
        friendRelationMapper.insert(relation);

        // 发送通知给被申请人
        User me = userMapper.selectById(myId);
        String myName = me != null ? me.getNickname() : "某人";
        notificationService.send(toUid, myId, NotificationEnum.FRIEND_REQUEST.getType(), NotificationEnum.FRIEND_REQUEST.getDesc(),
                myName + " 想添加你为好友" + (message != null && !message.isEmpty() ? "：" + message : ""),
                relation.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptFriend(Integer relationId) throws BusinessException {
        Integer myId = currentUser.getUserId();
        FriendRelation relation = friendRelationMapper.selectById(relationId);
        if (relation == null || !relation.getRecipientId().equals(myId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "申请不存在或无权操作");
        }
        friendRelationMapper.update(null, new LambdaUpdateWrapper<FriendRelation>()
                .eq(FriendRelation::getId, relationId)
                .set(FriendRelation::getStatus, 1)
                .set(FriendRelation::getUpdateTime, LocalDateTime.now())
        );
        // 通知申请人
        User me = userMapper.selectById(myId);
        String myName = me != null ? me.getNickname() : "对方";
        notificationService.send(relation.getApplicantId(), myId, NotificationEnum.FRIEND_APPROVED.getType(),
                NotificationEnum.FRIEND_APPROVED.getDesc(), myName + " 已接受你的好友申请，现在可以开始聊天了！", null);
    }

    @Override
    public void rejectFriend(Integer relationId) {
        Integer myId = currentUser.getUserId();
        friendRelationMapper.update(null, new LambdaUpdateWrapper<FriendRelation>()
                .eq(FriendRelation::getId, relationId)
                .eq(FriendRelation::getRecipientId, myId)
                .set(FriendRelation::getStatus, 2)
                .set(FriendRelation::getUpdateTime, LocalDateTime.now())
        );
    }

    @Override
    public void removeFriend(Integer friendId) {
        Integer myId = currentUser.getUserId();
        friendRelationMapper.delete(new LambdaQueryWrapper<FriendRelation>()
                .and(w -> w
                        .eq(FriendRelation::getApplicantId, myId).eq(FriendRelation::getRecipientId, friendId)
                        .or()
                        .eq(FriendRelation::getApplicantId, friendId).eq(FriendRelation::getRecipientId, myId)
                )
                .eq(FriendRelation::getStatus, 1)
        );
    }

    @Override
    public List<FriendVO> listFriends() {
        Integer myId = currentUser.getUserId();
        List<FriendRelation> relations = friendRelationMapper.selectFriends(myId);
        return relations.stream().map(r -> toFriendVO(r, myId)).collect(Collectors.toList());
    }

    @Override
    public List<FriendVO> listPendingRequests() {
        Integer myId = currentUser.getUserId();
        List<FriendRelation> relations = friendRelationMapper.selectPendingRequests(myId);
        return relations.stream().map(r -> {
            FriendVO vo = new FriendVO();
            vo.setRelationId(r.getId());
            vo.setApplicantId(r.getApplicantId());
            vo.setStatus(r.getStatus());
            vo.setMessage(r.getMessage());
            vo.setCreateTime(r.getCreateTime() != null ? r.getCreateTime().toString() : "");
            User applicant = userMapper.selectById(r.getApplicantId());
            if (applicant != null) {
                vo.setFriendId(applicant.getId());
                vo.setNickname(applicant.getNickname());
                vo.setAvatar(applicant.getAvatar());
                vo.setRole(applicant.getRole());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private FriendVO toFriendVO(FriendRelation r, Integer myId) {
        Integer friendId = r.getApplicantId().equals(myId) ? r.getRecipientId() : r.getApplicantId();
        FriendVO vo = new FriendVO();
        vo.setRelationId(r.getId());
        vo.setFriendId(friendId);
        vo.setStatus(r.getStatus());
        vo.setMessage(r.getMessage());
        vo.setCreateTime(r.getCreateTime() != null ? r.getCreateTime().toString() : "");
        User friend = userMapper.selectById(friendId);
        if (friend != null) {
            vo.setNickname(friend.getNickname());
            vo.setAvatar(friend.getAvatar());
            vo.setRole(friend.getRole());
        }
        return vo;
    }

    @Override
    public Set<Integer> getFriendIds(Integer uid) {
        List<FriendRelation> relations = friendRelationMapper.selectFriends(uid);
        Set<Integer> ids = new HashSet<>();
        for (FriendRelation r : relations) {
            ids.add(r.getApplicantId().equals(uid) ? r.getRecipientId() : r.getApplicantId());
        }
        return ids;
    }
}
