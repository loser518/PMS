package com.project.pms.entity.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课题管理 Excel 导入/导出 DTO
 *
 * @author loser
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@HeadRowHeight(20)
@ColumnWidth(20)
@HeadFontStyle(bold = com.alibaba.excel.enums.BooleanEnum.TRUE)
public class ProjectInfoExcelDTO {

    /** 课题名称，导入必填 */
    @ExcelProperty("课题名称")
    @ColumnWidth(30)
    private String title;

    /** 课题类型名称（导入时按名称匹配，导出时显示类型名） */
    @ExcelProperty("课题类型")
    @ColumnWidth(16)
    private String typeName;

    /** 课题描述 */
    @ExcelProperty("课题描述")
    @ColumnWidth(40)
    private String description;

    /** 申报学生姓名（导出时显示，导入时用于查找学生） */
    @ExcelProperty("申报学生")
    @ColumnWidth(16)
    private String studentName;

    /** 申报学生学号（导入时用于精准匹配学生） */
    @ExcelProperty("学生学号")
    @ColumnWidth(18)
    private String studentUsername;

    /** 指导教师姓名（导出时显示，导入时用于查找教师） */
    @ExcelProperty("指导教师")
    @ColumnWidth(16)
    private String teacherName;

    /** 指导教师工号（导入时用于精准匹配教师） */
    @ExcelProperty("教师工号")
    @ColumnWidth(18)
    private String teacherUsername;

    /**
     * 审核状态：0-待审核，1-通过，2-驳回，3-需修改
     * 导出时显示中文，导入时接受数字字符串
     */
    @ExcelProperty("审核状态")
    @ColumnWidth(16)
    private String status;

    /** 管理员/教师审核意见（导出用） */
    @ExcelProperty("审核意见")
    @ColumnWidth(30)
    private String opinion;

    /** 申报时间（导出只读） */
    @ExcelProperty("申报时间")
    @ColumnWidth(22)
    private String createTime;
}
