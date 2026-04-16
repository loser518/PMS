package com.project.pms.service;

import com.project.pms.entity.po.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.pms.entity.po.CommentTree;
import com.project.pms.entity.vo.Result;

import java.util.List;

/**
 * <p>
 * 评论表 服务类
 * </p>
 *
 * @author loser
 * @since 2026-02-27
 */
public interface ICommentService extends IService<Comment> {

    /**
     * 根据tid获取评论树
     *
     * @param tid
     * @return
     */
    List<CommentTree> getCommentTreeByTid(Integer tid);


    /**
     * 添加或者回复评论
     *
     * @param comment
     * @return
     */
    Result addComment(Comment comment);
}
