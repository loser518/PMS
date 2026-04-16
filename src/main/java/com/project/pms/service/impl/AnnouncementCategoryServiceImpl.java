package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.pms.entity.dto.CategoryDto;
import com.project.pms.entity.po.AnnouncementCategory;
import com.project.pms.entity.query.CategoryQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.enums.ResponseCodeEnum;
import com.project.pms.mapper.AnnouncementCategoryMapper;
import com.project.pms.service.IAnnouncementCategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.pms.utils.CopyUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 公告分类表 服务实现类
 * </p>
 *
 * @author loser
 * @since 2026-02-04
 */
@Service
@RequiredArgsConstructor
public class AnnouncementCategoryServiceImpl extends ServiceImpl<AnnouncementCategoryMapper, AnnouncementCategory> implements IAnnouncementCategoryService {
    private final AnnouncementCategoryMapper announcementCategoryMapper;

    /**
     * 添加公告分类
     *
     * @param category
     * @return
     */
    @Override
    public Result addCategory(CategoryDto category) {
        if (category == null) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "category信息不能为空！");
        }
        if (StringUtils.isBlank(category.getName()) || category.getName() == null) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "公告分类名称不能为空！");
        }
        LambdaQueryWrapper<AnnouncementCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AnnouncementCategory::getName, category.getName());
        if (announcementCategoryMapper.selectOne(queryWrapper) != null) {
            return Result.error(ResponseCodeEnum.CODE_600.getCode(), "公告分类名称已存在！");
        }
        AnnouncementCategory announcementCategory = CopyUtil.copy(category, AnnouncementCategory.class);
        announcementCategory.setCreateTime(new Date());
        announcementCategory.setUpdateTime(new Date());

        announcementCategoryMapper.insert(announcementCategory);
        return Result.success("添加公告分类成功！");
    }

    /**
     * 获取公告分类列表
     *
     * @param query
     * @return
     */
    @Override
    public PageResult<AnnouncementCategory> getPageList(CategoryQuery query) {
        Page<AnnouncementCategory> page = Page.of(query.getPageNo(), query.getPageSize());
        announcementCategoryMapper.selectAnnouncementCategoryInfoList(page, query);
        List<AnnouncementCategory> list = page.getRecords();
        return PageResult.success(list, (int) page.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * 修改公告分类
     *
     * @param category
     * @return
     */
    @Override
    public Result updateCategory(AnnouncementCategory category) {
        if (category == null) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "category信息不能为空！");
        }
        if (StringUtils.isBlank(category.getName()) || category.getName() == null) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "公告分类名称不能为空！");
        }
        if (announcementCategoryMapper.selectById(category.getId()) == null) {
            return Result.error(ResponseCodeEnum.CODE_602.getCode(), "公告分类不存在！");
        }
        if (announcementCategoryMapper.selectOne(new LambdaQueryWrapper<AnnouncementCategory>().eq(AnnouncementCategory::getName, category.getName())) != null) {
            return Result.error(ResponseCodeEnum.CODE_600.getCode(), "公告分类名称已存在！");
        }
        AnnouncementCategory announcementCategory = CopyUtil.copy(category, AnnouncementCategory.class);
        announcementCategory.setUpdateTime(new Date());
        announcementCategoryMapper.updateById(announcementCategory);
        return Result.success("修改公告分类成功！");
    }
}
