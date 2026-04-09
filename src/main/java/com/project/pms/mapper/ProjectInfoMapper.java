package com.project.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.pms.entity.po.ProjectInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.pms.entity.query.ProjectInfoQuery;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 课题项目申报信息表 Mapper 接口
 * </p>
 *
 * @author loser
 * @since 2026-02-09
 */
public interface ProjectInfoMapper extends BaseMapper<ProjectInfo> {
    /**
     * 查询课题项目申报信息列表
     *
     * @param page
     * @param query
     * @return
     */
    IPage<ProjectInfo> selectProjectInfoList(IPage<ProjectInfo> page,@RequestParam("query") ProjectInfoQuery query);
}
