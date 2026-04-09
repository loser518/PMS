package com.project.pms.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.pms.entity.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * @className: CommentTree
 * @description: 评论树
 * @author: loser
 * @createTime: 2026/2/10 21:24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentTree {
    private Integer id;
    private Integer tid;
    private Integer rootId;
    private Integer parentId;
    private String content;
    private UserDto user;
    private UserDto toUser;
    private Integer love;
    private Integer bad;
    private List<CommentTree> replies;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Long count;
}
