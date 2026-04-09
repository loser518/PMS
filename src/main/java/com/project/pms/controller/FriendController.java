package com.project.pms.controller;

import com.project.pms.entity.vo.FriendVO;
import com.project.pms.entity.vo.Result;
import com.project.pms.exception.BusinessException;
import com.project.pms.service.IFriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友控制器
 *
 * @author loser
 * @since 2026-03-14
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/friend")
public class FriendController {

    private final IFriendService friendService;

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result listFriends() {
        List<FriendVO> list = friendService.listFriends();
        return Result.success(list, "获取好友列表成功");
    }

    /**
     * 获取收到的好友申请（待处理）
     */
    @GetMapping("/requests")
    public Result listPendingRequests() {
        List<FriendVO> list = friendService.listPendingRequests();
        return Result.success(list, "获取好友申请成功");
    }

    /**
     * 发送好友申请
     *
     * @param toUid   目标用户ID
     * @param message 申请附言（可为空）
     */
    @PostMapping("/apply")
    public Result applyFriend(@RequestParam Integer toUid,
                              @RequestParam(required = false) String message) throws BusinessException {
        friendService.applyFriend(toUid, message);
        return Result.success("好友申请已发送");
    }

    /**
     * 接受好友申请
     *
     * @param id 关系ID
     */
    @PostMapping("/accept/{id}")
    public Result acceptFriend(@PathVariable Integer id) throws BusinessException {
        friendService.acceptFriend(id);
        return Result.success("已接受好友申请");
    }

    /**
     * 拒绝好友申请
     *
     * @param id 关系ID
     */
    @PostMapping("/reject/{id}")
    public Result rejectFriend(@PathVariable Integer id) {
        friendService.rejectFriend(id);
        return Result.success("已拒绝好友申请");
    }

    /**
     * 删除好友
     *
     * @param friendId 好友用户ID
     */
    @DeleteMapping("/{friendId}")
    public Result removeFriend(@PathVariable Integer friendId) {
        friendService.removeFriend(friendId);
        return Result.success("已删除好友");
    }
}
