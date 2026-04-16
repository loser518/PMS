package com.project.pms.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.project.pms.entity.po.Comment;
import com.project.pms.entity.po.CommentTree;
import com.project.pms.entity.vo.Result;
import com.project.pms.service.ICommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * @className: CommentController
 * @description: 评论控制器
 * @author: loser
 * @createTime: 2026/2/27 15:21
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/comment")
public class CommentController {

    private final ICommentService commentService;

    /**
     * 获取指定目标的评论列表（树状展示）
     *
     * @param tid 目标ID
     */
    @GetMapping("/tree/{tid}")
    public List<CommentTree> getCommentTree(@PathVariable Integer tid) {
        return commentService.getCommentTreeByTid(tid);
    }

    /**
     * 发表评论/回复
     *
     * @param comment
     * @return
     */
    @PostMapping("/add")
    public Result addComment(@RequestBody Comment comment) {
        return commentService.addComment(comment);
    }

    /**
     * 删除评论（逻辑删除）
     *
     * @param id
     * @return
     */
    @PostMapping("/{id}")
    public Result deleteComment(@PathVariable Integer id) {
        LambdaUpdateWrapper<Comment> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Comment::getId, id);
        updateWrapper.set(Comment::getIsDeleted, 1);
        commentService.update(updateWrapper);
        return Result.success("删除评论成功!");
    }

}
