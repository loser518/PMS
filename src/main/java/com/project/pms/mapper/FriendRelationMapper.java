package com.project.pms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.pms.entity.po.FriendRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友关系 Mapper
 */
@Mapper
public interface FriendRelationMapper extends BaseMapper<FriendRelation> {

    /**
     * 查询用户的已通过好友列表（含双向关系）
     */
    @Select("SELECT * FROM friend_relation WHERE status = 1 AND (applicant_id = #{uid} OR recipient_id = #{uid})")
    List<FriendRelation> selectFriends(@Param("uid") Integer uid);

    /**
     * 查询收到的好友申请（status=0）
     */
    @Select("SELECT * FROM friend_relation WHERE recipient_id = #{uid} AND status = 0 ORDER BY create_time DESC")
    List<FriendRelation> selectPendingRequests(@Param("uid") Integer uid);

    /**
     * 检查两用户间是否已存在关系（任意方向）
     */
    @Select("SELECT COUNT(*) FROM friend_relation WHERE status IN (0,1) AND " +
            "((applicant_id = #{uid1} AND recipient_id = #{uid2}) OR " +
            "(applicant_id = #{uid2} AND recipient_id = #{uid1}))")
    int existsRelation(@Param("uid1") Integer uid1, @Param("uid2") Integer uid2);
}
