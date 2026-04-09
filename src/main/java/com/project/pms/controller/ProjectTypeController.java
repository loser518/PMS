package com.project.pms.controller;

import com.project.pms.entity.po.ProjectType;
import com.project.pms.entity.query.ProjectTypeQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.exception.BusinessException;
import com.project.pms.service.IProjectTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课题类型管理 Controller
 *
 * @author loser
 * @since 2026-03-16
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/projectType")
public class ProjectTypeController {

    private final IProjectTypeService projectTypeService;

    /**
     * 分页查询课题类型列表
     */
    @GetMapping
    public PageResult<ProjectType> list(ProjectTypeQuery query) {
        return projectTypeService.selectProjectTypeList(query);
    }

    /**
     * 查询所有启用的课题类型（用于申报表单下拉框）
     */
    @GetMapping("/enabled")
    public Result<List<ProjectType>> listEnabled() {
        return Result.success(projectTypeService.listEnabled(), "查询成功");
    }

    /**
     * 新增课题类型
     */
    @PostMapping
    public Result add(@RequestBody ProjectType projectType) throws BusinessException {
        if (projectType.getName() == null || projectType.getName().isBlank()) {
            return Result.error("类型名称不能为空");
        }
        // 默认启用、默认排序为0
        if (projectType.getStatus() == null) projectType.setStatus(1);
        if (projectType.getSort() == null) projectType.setSort(0);
        projectTypeService.saveProjectType(projectType);
        return Result.success("添加课题类型成功！");
    }

    /**
     * 更新课题类型
     */
    @PostMapping("/update")
    public Result update(@RequestBody ProjectType projectType) throws BusinessException {
        projectTypeService.updateProjectType(projectType);
        return Result.success("更新课题类型成功！");
    }

    /**
     * 删除课题类型（批量）
     */
    @DeleteMapping
    public Result remove(@RequestBody List<Integer> ids) {
        projectTypeService.removeByIds(ids);
        return Result.success("删除课题类型成功！");
    }
}
