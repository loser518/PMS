package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.pms.entity.po.Comment;
import com.project.pms.entity.po.CommentTree;
import com.project.pms.entity.vo.Result;
import com.project.pms.entity.vo.UserInfoVO;
import com.project.pms.mapper.CommentMapper;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.ICommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.pms.service.UserInfoService;
import com.project.pms.utils.CopyUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 评论表 服务实现类
 * </p>
 *
 * @author loser
 * @since 2026-02-27
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {

    private final CurrentUser currentUser;
    private final UserInfoService userInfoService;


    /**
     * 添加或回复评论
     *
     * @param comment
     * @return
     */
    @Override
    public Result addComment(Comment comment) {
        // 基础非空校验
        if (comment.getTid() == null) {
            return Result.error("参数错误！");
        }
        if (StringUtils.isBlank(comment.getContent())) {
            return Result.error("评论不能为空！");
        }
        comment.setUid(currentUser.getUserId());
        comment.setCreateTime(new Date());

        // 3. 处理回复逻辑
        if (comment.getParentId() != null && comment.getParentId() != 0) {
            // 说明这是一条回复，不是根评论
            Comment parentComment = this.getById(comment.getParentId());
            if (parentComment == null) {
                return Result.error("回复的父评论不存在！");
            }

            // A. 自动填充被回复人 ID
            comment.setToUserId(parentComment.getUid());

            // B. 维护 rootId 逻辑
            // 如果父评论的 rootId 为 0，说明父评论是“一楼”，那么新评论的 root 就是这个“一楼”
            // 如果父评论的 rootId 不为 0，说明父评论也是个“回复”，那么新评论就跟它共用同一个 root
            if (parentComment.getRootId() == null || parentComment.getRootId() == 0) {
                comment.setRootId(parentComment.getId());
            } else {
                comment.setRootId(parentComment.getRootId());
            }
            this.save(comment);
            return Result.success("回复成功！");
        } else {
            // 说明这是根评论
            comment.setRootId(0);
            comment.setParentId(0);
            comment.setToUserId(null);
            this.save(comment);
            return Result.success("评论成功！");
        }
    }

    /**
     * 获取评论树
     *
     * @param tid
     * @return
     */
    @Override
    public List<CommentTree> getCommentTreeByTid(Integer tid) {
        // 1. 查询该目标下的所有未删除评论
        List<Comment> allComments = this.list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTid, tid)
                .eq(Comment::getIsDeleted, 0)
                .orderByDesc(Comment::getIsTop) // 置顶优先
                .orderByDesc(Comment::getCreateTime));

        if (allComments.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 将 PO 转换为 Tree 节点（此时还未嵌套）
        List<CommentTree> treeNodes = allComments.stream().map(this::convertToTreeDTO).collect(Collectors.toList());

        // 3. 构造 Map 方便通过 ID 快速查找节点
        Map<Integer, CommentTree> map = treeNodes.stream().collect(Collectors.toMap(CommentTree::getId, node -> node));

        List<CommentTree> rootComments = new ArrayList<>();

        // 4. 组装树形结构
        for (CommentTree node : treeNodes) {
            if (node.getRootId() == null || node.getRootId() == 0) {
                // 如果没有 rootId，说明自己就是根评论
                rootComments.add(node);
            } else {
                // 如果有 rootId，找到它的根评论节点，放入其 replies 集合中
                CommentTree root = map.get(node.getRootId());
                if (root != null) {
                    if (root.getReplies() == null) {
                        root.setReplies(new ArrayList<>());
                    }
                    root.getReplies().add(node);
                }
            }
        }

        // 5. 更新每条根评论下的回复总数
        rootComments.forEach(root -> {
            if (root.getReplies() != null) {
                root.setCount((long) root.getReplies().size());
            } else {
                root.setCount(0L);
            }
        });

        return rootComments;
    }

    /**
     * 将 PO 转为 DTO 并填充用户信息
     */
    private CommentTree convertToTreeDTO(Comment comment) {
        CommentTree commentTree = CopyUtil.copy(comment, CommentTree.class);

//         填充评论者用户信息 (这里需要根据实际的 UserService 实现)
        commentTree.setUser(userInfoService.getOneUserInfo(comment.getUid()).getUser());

        // 如果是回复某人，填充被回复者用户信息
        if (comment.getToUserId() != null) {
            commentTree.setToUser(userInfoService.getOneUserInfo(comment.getToUserId()).getUser());
        }

        return commentTree;
    }


}
