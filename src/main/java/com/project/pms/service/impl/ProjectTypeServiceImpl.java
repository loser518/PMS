package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.pms.entity.po.ProjectType;
import com.project.pms.entity.query.ProjectTypeQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.exception.BusinessException;
import com.project.pms.mapper.ProjectTypeMapper;
import com.project.pms.service.IProjectTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 课题类型 Service 实现类
 *
 * @author loser
 * @since 2026-03-16
 */
@Service
@RequiredArgsConstructor
public class ProjectTypeServiceImpl extends ServiceImpl<ProjectTypeMapper, ProjectType> implements IProjectTypeService {

    @Override
    public PageResult<ProjectType> selectProjectTypeList(ProjectTypeQuery query) {
        Page<ProjectType> page = Page.of(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<ProjectType> wrapper = new LambdaQueryWrapper<ProjectType>()
                .like(StringUtils.hasText(query.getName()), ProjectType::getName, query.getName())
                .eq(query.getStatus() != null, ProjectType::getStatus, query.getStatus())
                .orderByAsc(ProjectType::getSort)
                .orderByAsc(ProjectType::getId);
        page(page, wrapper);
        return PageResult.success(page.getRecords(), (int) page.getTotal(), query.getPageNo(), query.getPageSize());
    }

    @Override
    public List<ProjectType> listEnabled() {
        return list(new LambdaQueryWrapper<ProjectType>()
                .eq(ProjectType::getStatus, 1)
                .orderByAsc(ProjectType::getSort)
                .orderByAsc(ProjectType::getId));
    }

    @Override
    public void saveProjectType(ProjectType projectType) throws BusinessException {
        checkNameUnique(projectType.getName(), null);
        save(projectType);
    }

    @Override
    public void updateProjectType(ProjectType projectType) throws BusinessException {
        if (projectType.getId() == null) {
            throw new BusinessException("id不能为空");
        }
        if (StringUtils.hasText(projectType.getName())) {
            checkNameUnique(projectType.getName(), projectType.getId());
        }
        updateById(projectType);
    }

    // ---------- 私有辅助 ----------

    /**
     * 校验类型名称唯一性
     *
     * @param name     名称
     * @param excludeId 更新时排除自身，传null表示新增
     */
    private void checkNameUnique(String name, Integer excludeId) throws BusinessException {
        LambdaQueryWrapper<ProjectType> wrapper = new LambdaQueryWrapper<ProjectType>()
                .eq(ProjectType::getName, name);
        if (excludeId != null) {
            wrapper.ne(ProjectType::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("课题类型名称「" + name + "」已存在，请更换！");
        }
    }
}
