package com.project.pms.entity.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户管理 Excel 导入/导出 DTO
 * <p>
 * 导出时将数据库字段映射为可读中文列名；
 * 导入时将 Excel 表格数据映射回实体字段。
 * </p>
 *
 * @author loser
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@HeadRowHeight(20)
@ColumnWidth(18)
@HeadFontStyle(bold = com.alibaba.excel.enums.BooleanEnum.TRUE)
public class UserExcelDTO {

    /** 用户名（学号/工号），导入必填 */
    @ExcelProperty("用户名(学号/工号)")
    @ColumnWidth(20)
    private String username;

    /** 昵称/姓名 */
    @ExcelProperty("姓名")
    private String nickname;

    /**
     * 角色：0-学生，1-教师，2-管理员
     * 导出时显示中文，导入时也接受数字字符串
     */
    @ExcelProperty("角色(0学生/1教师/2管理员)")
    @ColumnWidth(24)
    private String role;

    /** 手机号 */
    @ExcelProperty("手机号")
    @ColumnWidth(16)
    private String phone;

    /** 邮箱 */
    @ExcelProperty("邮箱")
    @ColumnWidth(24)
    private String email;

    /**
     * 账号状态：0-正常，1-封禁，2-注销
     * 仅导出时使用，中文可读
     */
    @ExcelProperty("状态(0正常/1封禁)")
    @ColumnWidth(18)
    private String status;

    /** 注册时间，仅导出 */
    @ExcelProperty("注册时间")
    @ColumnWidth(22)
    private String createTime;

    // ----- 学生附加字段（导入时可选填，角色为0时有效）------

    /** 年级 */
    @ExcelProperty("年级")
    private String grade;

    /** 专业 */
    @ExcelProperty("专业")
    @ColumnWidth(20)
    private String major;

    /** 学院 */
    @ExcelProperty("学院")
    @ColumnWidth(20)
    private String college;

    /** 班级 */
    @ExcelProperty("班级")
    @ColumnWidth(16)
    private String className;

    /** 入学年份（学生专用） */
    @ExcelProperty("入学年份")
    @ColumnWidth(14)
    private String enrollmentYear;

    /**
     * 导入时的默认密码列（导出时忽略）
     * 若留空则使用系统默认密码 123456
     */
    @ExcelProperty("初始密码(留空用123456)")
    @ColumnWidth(24)
    private String password;
}
