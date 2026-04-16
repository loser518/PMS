package com.project.pms.controller;

import com.alibaba.excel.EasyExcel;
import com.project.pms.entity.excel.UserExcelDTO;
import com.project.pms.entity.query.UserInfoQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.entity.vo.TeacherOptionVO;
import com.project.pms.entity.vo.UserInfoVO;
import com.project.pms.enums.ResponseCodeEnum;
import com.project.pms.exception.BusinessException;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.UserInfoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @className: UserInfoController
 * @description: 用户信息控制层
 * @author: loser
 * @createTime: 2026/2/3 10:43
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/userInfo")
public class UserInfoController {
    private final UserInfoService userInfoService;
    private final CurrentUser currentUser;

    /**
     * 获取用户信息列表
     *
     * @param query
     * @return
     */
    @GetMapping
    public PageResult<UserInfoVO> getUserInfoList(UserInfoQuery query) {
        return userInfoService.getUserInfoList(query);
    }

    /**
     * 获取用户信息
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result getOneUserInfo(@PathVariable Integer id) {
        return Result.success(userInfoService.getOneUserInfo(id), "获取用户信息成功！");
    }

    @PostMapping("/update")
    public Result updateUserInfo(@RequestBody UserInfoVO userInfoVO) throws BusinessException {
        return userInfoService.updateUserInfo(userInfoVO);
    }

    /**
     * 更新用户头像
     *
     * @param file
     * @return
     */
    @PostMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam("file") MultipartFile file) {
        Integer id = currentUser.getUserId();
        try {
            return userInfoService.updateAvatar(file, id);
        } catch (Exception e) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "更新用户头像失败！");
        }
    }


    /**
     * 获取教师下拉列表（用于课题申报时选择指导老师）
     *
     * @return 教师选项列表（含名额信息）
     */
    @GetMapping("/teachers")
    public Result getTeacherOptions() {
        List<TeacherOptionVO> list = userInfoService.getTeacherOptions();
        return Result.success(list, "获取教师列表成功！");
    }

    /**
     * 搜索用户（用于添加好友时查找对方）
     * 支持按昵称或用户名模糊搜索，排除自身，返回基础信息
     *
     * @param keyword 搜索关键字（昵称或用户名）
     * @return 用户列表
     */
    @GetMapping("/search")
    public Result searchUsers(@RequestParam String keyword) {
        Integer selfId = currentUser.getUserId();
        List<UserInfoVO> result = userInfoService.searchUsers(keyword, selfId);
        return Result.success(result, "搜索成功");
    }

    /**
     * 删除用户信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result deleteUserInfo(@RequestBody List<Integer> ids) {
        return userInfoService.deleteUserInfoByIds(ids);
    }

    /**
     * 导出用户列表为 Excel
     * GET /userInfo/export?role=0&keyword=xxx
     */
    @GetMapping("/export")
    public void exportExcel(UserInfoQuery query, HttpServletResponse response) throws Exception {
        List<UserExcelDTO> data = userInfoService.exportUserExcel(query);
        String fileName = URLEncoder.encode("用户列表", StandardCharsets.UTF_8) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
        EasyExcel.write(response.getOutputStream(), UserExcelDTO.class)
                .sheet("用户列表")
                .doWrite(data);
    }

    /**
     * 导入用户（Excel）
     * POST /userInfo/import  multipart/form-data  file=xxx.xlsx
     */
    @PostMapping("/import")
    public Result importExcel(@RequestParam("file") MultipartFile file) {
        return userInfoService.importUserExcel(file);
    }

}
