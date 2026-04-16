package com.project.pms.entity.vo;

import com.project.pms.entity.dto.UserDto;
import com.project.pms.entity.po.StudentProfile;
import com.project.pms.entity.po.TeacherProfile;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @className: UserInfoVO
 * @description: 响应用户信息
 * @author: loser
 * @createTime: 2026/1/31 21:16
 */
@Data
@Accessors(chain = true) // 允许链式调用
public class UserInfoVO {

    // 基础用户信息
    private UserDto user;

    // 根据角色返回不同的档案信息
    private Object profile;


    public static UserInfoVO forStudent(UserDto user, StudentProfile profile) {
        return new UserInfoVO()
                .setUser(user)
                .setProfile(profile);
    }

    public static UserInfoVO forTeacher(UserDto user, TeacherProfile profile) {
        return new UserInfoVO()
                .setUser(user)
                .setProfile(profile);
    }

    public static UserInfoVO forAdmin(UserDto user) {
        return new UserInfoVO()
                .setUser(user);
    }
//
//    public static <T> UserInfoVO<T> create(UserDto user, T profile) {
//        UserInfoVO<T> vo = new UserInfoVO<>();
//        vo.setUser(user);
//        vo.setProfile(profile);
//        return vo;
//    }
//
//
//
//    public static UserInfoVO<StudentProfile> forStudent(UserDto user, StudentProfile profile) {
//        return create(user, profile);
//    }
//
//    public static UserInfoVO<TeacherProfile> forTeacher(UserDto user, TeacherProfile profile) {
//        return create(user, profile);
//    }
//    private Integer uid;
//    private String username;
//    private String nickname;
//    // 性别，0女性 1男性 2无性别，默认2
//    private Integer gender;
//    private String description;
//    // 0 正常，1 封禁中，2 已注销
//    private Integer status;
//    // 0 普通用户，1 普通管理员，2 超级管理员
//    private Integer role;
//    private String token;
//
//    public UserInfoVO() {
//    }
//
//    public UserInfoVO(Integer uid, String username, String nickname, Integer gender, String description, Integer status, Integer role, String token) {
//        this.uid = uid;
//        this.username = username;
//        this.nickname = nickname;
//        this.gender = gender;
//        this.description = description;
//        this.status = status;
//        this.role = role;
//        this.token = token;
//    }
//
//    /**
//     * 获取
//     * @return uid
//     */
//    public Integer getUid() {
//        return uid;
//    }
//
//    /**
//     * 设置
//     * @param uid
//     */
//    public void setUid(Integer uid) {
//        this.uid = uid;
//    }
//
//    /**
//     * 获取
//     * @return username
//     */
//    public String getUsername() {
//        return username;
//    }
//
//    /**
//     * 设置
//     * @param username
//     */
//    public void setUsername(String username) {
//        this.username = username;
//    }
//
//    /**
//     * 获取
//     * @return nickname
//     */
//    public String getNickname() {
//        return nickname;
//    }
//
//    /**
//     * 设置
//     * @param nickname
//     */
//    public void setNickname(String nickname) {
//        this.nickname = nickname;
//    }
//
//    /**
//     * 获取
//     * @return gender
//     */
//    public Integer getGender() {
//        return gender;
//    }
//
//    /**
//     * 设置
//     * @param gender
//     */
//    public void setGender(Integer gender) {
//        this.gender = gender;
//    }
//
//    /**
//     * 获取
//     * @return description
//     */
//    public String getDescription() {
//        return description;
//    }
//
//    /**
//     * 设置
//     * @param description
//     */
//    public void setDescription(String description) {
//        this.description = description;
//    }
//
//    /**
//     * 获取
//     * @return status
//     */
//    public Integer getState() {
//        return status;
//    }
//
//    /**
//     * 设置
//     * @param status
//     */
//    public void setState(Integer status) {
//        this.status = status;
//    }
//
//    /**
//     * 获取
//     * @return role
//     */
//    public Integer getRole() {
//        return role;
//    }
//
//    /**
//     * 设置
//     * @param role
//     */
//    public void setRole(Integer role) {
//        this.role = role;
//    }
//
//    /**
//     * 获取
//     * @return token
//     */
//    public String getToken() {
//        return token;
//    }
//
//    /**
//     * 设置
//     * @param token
//     */
//    public void setToken(String token) {
//        this.token = token;
//    }
}
