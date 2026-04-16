package com.project.pms.entity.vo;

import lombok.Data;

/**
 * 好友信息VO（含用户基本信息）
 */
@Data
public class FriendVO {
    /** 关系ID */
    private Integer relationId;
    /** 好友用户ID */
    private Integer friendId;
    /** 好友昵称 */
    private String nickname;
    /** 好友头像 */
    private String avatar;
    /** 好友角色 */
    private Integer role;
    /** 关系状态：1-已通过 */
    private Integer status;
    /** 申请附言（好友申请列表用） */
    private String message;
    /** 申请时间 */
    private String createTime;
    /** 申请人ID（好友申请列表用） */
    private Integer applicantId;
}
