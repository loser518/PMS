package com.project.pms.service;

import com.project.pms.entity.excel.ProjectInfoExcelDTO;
import com.project.pms.entity.po.ProjectInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.pms.entity.query.ProjectInfoQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 课题项目申报信息表 服务类
 * </p>
 *
 * @author loser
 * @since 2026-02-09
 */
public interface IProjectInfoService extends IService<ProjectInfo> {

    PageResult<ProjectInfo> selectProjectInfoList(ProjectInfoQuery query);

    /**
     * 学生提交课题申报（含指导教师名额校验）
     * 若指定了 tid，则校验该教师名额是否已满
     *
     * @param projectInfo 课题信息
     */
    void saveWithAdvisorCheck(ProjectInfo projectInfo) throws BusinessException;

    /**
     * 更新课题信息（含审核通过/驳回时的教师计数维护）
     * - 审核通过（status 从非1 → 1）：教师 currentStudentCount +1
     * - 审核驳回/需修改（status 从1 → 非1）：教师 currentStudentCount -1
     * - tid 变更：旧教师 -1，新教师名额校验 + +1
     *
     * @param newInfo 更新后的课题信息
     */
    void updateWithAdvisorCount(ProjectInfo newInfo) throws BusinessException;

    /**
     * 删除课题信息，若课题已通过审核则同步更新教师计数 -1
     *
     * @param id 课题ID
     */
    void removeWithAdvisorCount(Integer id);

    /**
     * 导出课题列表为 Excel
     *
     * @param query 筛选条件
     * @return Excel 行数据列表
     */
    List<ProjectInfoExcelDTO> exportProjectExcel(ProjectInfoQuery query);

    /**
     * 批量导入课题（Excel）
     *
     * @param file 上传的 Excel 文件
     * @return 导入结果
     */
    Result importProjectExcel(MultipartFile file);
}
