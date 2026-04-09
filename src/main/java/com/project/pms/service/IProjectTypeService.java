package com.project.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.pms.entity.po.ProjectType;
import com.project.pms.entity.query.ProjectTypeQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.exception.BusinessException;

import java.util.List;

/**
 * 课题类型 Service 接口
 *
 * @author loser
 * @since 2026-03-16
 */
public interface IProjectTypeService extends IService<ProjectType> {

    /**
     * 分页查询课题类型列表
     */
    PageResult<ProjectType> selectProjectTypeList(ProjectTypeQuery query);

    /**
     * 查询所有启用的课题类型（供申报下拉框使用）
     */
    List<ProjectType> listEnabled();

    /**
     * 新增课题类型（名称唯一性校验）
     */
    void saveProjectType(ProjectType projectType) throws BusinessException;

    /**
     * 更新课题类型（名称唯一性校验，排除自身）
     */
    void updateProjectType(ProjectType projectType) throws BusinessException;
}
