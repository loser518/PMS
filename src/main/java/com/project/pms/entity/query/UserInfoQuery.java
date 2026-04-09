package com.project.pms.entity.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @className: UserInfoQuery
 * @description: 用户信息查询参数
 * @author: loser
 * @createTime: 2026/2/3 10:42
 */
@Data
public class UserInfoQuery extends PageQuery {
    private Integer id;
    private String username;
    private String nickname;
    private Integer gender;
    private Integer status;
    private Integer role;
    private String phone;
    private String email;

    //    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    private Date createTime;
//    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    private Date updateTime;



    /**
     * 获取排序SQL
     */
    public String getOrderByClause() {
        return super.getOrderByClause("id", true); // 默认按id升序
    }
}
