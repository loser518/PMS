package com.project.pms.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @className: Comment
 * @description: 评论
 * @author: loser
 * @createTime: 2026/2/10 21:23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer tid;
    private Integer uid;
    private Integer rootId;
    private Integer parentId;
    private Integer toUserId;
    private String content;
    private Integer love;
    private Integer bad;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Integer isTop;
    private Integer isDeleted;
}
