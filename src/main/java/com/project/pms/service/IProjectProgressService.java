package com.project.pms.service;

import com.project.pms.entity.po.ProjectProgress;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.pms.entity.query.ProjectProgressQuery;
import com.project.pms.entity.vo.PageResult;

/**
 * <p>
 * 课题进度管理表 服务类
 * </p>
 *
 * @author loser
 * @since 2026-02-09
 */
public interface IProjectProgressService extends IService<ProjectProgress> {

    PageResult<ProjectProgress> selectProjectProgressList(ProjectProgressQuery query);
}
