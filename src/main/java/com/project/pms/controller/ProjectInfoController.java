package com.project.pms.controller;

import com.alibaba.excel.EasyExcel;
import com.project.pms.entity.dto.ProjectInfoDTO;
import com.project.pms.entity.excel.ProjectInfoExcelDTO;
import com.project.pms.entity.po.ProjectInfo;
import com.project.pms.entity.query.ProjectInfoQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.exception.BusinessException;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.IProjectInfoService;
import com.project.pms.utils.CopyUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @className: ProjectInfoController
 * @description: 课题项目申报信息表控制类
 * @author: loser
 * @createTime: 2026/2/9 15:23
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/projectInfo")
@Validated
public class ProjectInfoController {
    private final IProjectInfoService projectInfoService;
    private final CurrentUser currentUser;

    /**
     * 添加课题项目申报信息
     * 学生提交时，sid 由后端自动注入，若指定了指导教师则校验名额
     *
     * @param projectInfoDTO 课题项目申报信息
     * @return 添加结果
     */
    @PostMapping
    public Result addProjectInfo(@RequestBody ProjectInfoDTO projectInfoDTO) throws BusinessException {
        if (projectInfoDTO.getTitle() == null || projectInfoDTO.getTitle().isBlank()) {
            return Result.error("课题名称不能为空");
        }
        if (projectInfoDTO.getTypeId() == null) {
            return Result.error("课题类型不能为空");
        }
        ProjectInfo projectInfo = CopyUtil.copy(projectInfoDTO, ProjectInfo.class);
        projectInfo.setSid(currentUser.getUserId());
        // 使用带名额校验的 save 方法
        projectInfoService.saveWithAdvisorCheck(projectInfo);
        return Result.success("添加课题项目申报信息成功！");
    }

    /**
     * 查询所有课题项目申报信息
     *
     * @return 查询结果
     */
    @GetMapping
    public PageResult<ProjectInfo> queryProjectInfo(ProjectInfoQuery query) {
        return projectInfoService.selectProjectInfoList(query);
    }


    /**
     * 修改课题项目申报信息（含审核通过/驳回时自动维护教师指导人数）
     *
     * @param projectInfoDTO 课题项目申报信息
     * @return 修改结果
     */
    @PostMapping("/update")
    public Result updateProjectInfo(@RequestBody ProjectInfoDTO projectInfoDTO) throws BusinessException {
        ProjectInfo projectInfo = CopyUtil.copy(projectInfoDTO, ProjectInfo.class);
        // 使用带计数维护的 update 方法
        projectInfoService.updateWithAdvisorCount(projectInfo);
        return Result.success("修改课题项目申报信息成功！");
    }


    /**
     * 删除课题项目申报信息
     * 若课题已审核通过，同步维护教师指导人数 -1
     *
     * @param ids 主键ID
     * @return 删除结果
     */
    @DeleteMapping
    public Result deleteProjectInfo(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            projectInfoService.removeWithAdvisorCount(id);
        }
        return Result.success("删除课题项目申报信息成功！");
    }

    /**
     * 导出课题列表为 Excel
     * GET /projectInfo/export?status=1&keyword=xxx
     */
    @GetMapping("/export")
    public void exportExcel(ProjectInfoQuery query, HttpServletResponse response) throws Exception {
        List<ProjectInfoExcelDTO> data = projectInfoService.exportProjectExcel(query);
        String fileName = URLEncoder.encode("课题列表", StandardCharsets.UTF_8) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
        EasyExcel.write(response.getOutputStream(), ProjectInfoExcelDTO.class)
                .sheet("课题列表")
                .doWrite(data);
    }

    /**
     * 导入课题（Excel）
     * POST /projectInfo/import  multipart/form-data  file=xxx.xlsx
     */
    @PostMapping("/import")
    public Result importExcel(@RequestParam("file") MultipartFile file) {
        return projectInfoService.importProjectExcel(file);
    }
}
