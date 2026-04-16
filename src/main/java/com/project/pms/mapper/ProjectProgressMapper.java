package com.project.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.pms.entity.po.ProjectProgress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.pms.entity.query.ProjectProgressQuery;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 课题进度管理表 Mapper 接口
 * </p>
 *
 * @author loser
 * @since 2026-02-09
 */
public interface ProjectProgressMapper extends BaseMapper<ProjectProgress> {

    /**
     * 查询课题进度列表
     *
     * @param page
     * @param query
     * @return
     */
    IPage<ProjectProgress> selectProjectProgressList(IPage<ProjectProgress> page,@RequestParam("query") ProjectProgressQuery query);
}
