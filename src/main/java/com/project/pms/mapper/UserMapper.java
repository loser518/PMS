package com.project.pms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.project.pms.entity.po.User;
import com.project.pms.entity.query.UserInfoQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @className: UserMapper
 * @description:  UserMapper
 * @author: loser
 * @createTime: 2026/1/31 21:12
 */
public interface UserMapper extends BaseMapper<User> {
    /**
     * 分页查询用户信息
     * @param page 分页对象，插件会自动识别并进行 count 和 limit 操作
     * @param query 查询条件封装对象
     * @return 分页结果集
     */
    IPage<User> selectUserInfoList(IPage<User> page, @Param("query") UserInfoQuery query);

    /**
     * 根据角色查询用户ID列表
     *
     * @param role 角色（0-学生，1-教师，2-管理员）
     * @return 用户ID列表
     */
    @Select("SELECT id FROM user WHERE role = #{role} AND status = 0")
    List<Integer> selectIdsByRole(@Param("role") Integer role);
}
