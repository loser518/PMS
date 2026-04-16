package com.project.pms.controller;

import com.project.pms.entity.dto.CategoryDto;
import com.project.pms.entity.po.AnnouncementCategory;
import com.project.pms.entity.query.CategoryQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.service.IAnnouncementCategoryService;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Delete;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @className: CategoryController
 * @description: 公告分类控制类
 * @author: loser
 * @createTime: 2026/2/4 21:56
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/category")
public class CategoryController {
    private final IAnnouncementCategoryService categoryService;

    /**
     * 添加公告分类
     *
     * @param category
     * @return
     */
    @PostMapping
    public Result addCategory(@RequestBody CategoryDto category) {
        return categoryService.addCategory(category);
    }

    /**
     * 获取公告分类列表
     *
     * @param query
     * @return
     */
    @GetMapping
    public PageResult<AnnouncementCategory> getPageList(CategoryQuery query) {
        return categoryService.getPageList(query);
    }

    /**
     * 修改公告分类
     *
     * @param category
     * @return
     */
    @PostMapping("/update")
    public Result updateCategory(@RequestBody AnnouncementCategory category) {
        return categoryService.updateCategory(category);
    }


    /**
     * 删除公告分类
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result deleteCategory(@RequestBody List<Integer> ids) {
        categoryService.removeBatchByIds(ids);
        return Result.success("删除成功");
    }

}
