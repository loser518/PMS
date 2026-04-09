package com.project.pms.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @className: LoginUserDto
 * @description:  返回登录用户信息
 * @author: loser
 * @createTime: 2026/1/31 21:15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Integer id;
    private String username;
    private String nickname;
    private String avatar;
    private String background;
    // 性别，0女性 1男性 2无性别，默认2
    private Integer gender;
    private String description;
    // 0 正常，1 封禁中，2 已注销
    private Integer status;
    // 0 学生，1 指导老师，2 管理员
    private Integer role;
    private String phone;
    private String email;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}