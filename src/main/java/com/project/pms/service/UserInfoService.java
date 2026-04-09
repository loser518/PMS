package com.project.pms.service;

import com.project.pms.entity.excel.UserExcelDTO;
import com.project.pms.entity.query.UserInfoQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.entity.vo.TeacherOptionVO;
import com.project.pms.entity.vo.UserInfoVO;
import com.project.pms.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @className: UserInfoService
 * @description: 用户信息服务接口
 * @author: loser
 * @createTime: 2026/2/3 11:07
 */
public interface UserInfoService {
    /**
     * 获取用户信息列表
     *
     * @param query
     * @return
     */
    PageResult<UserInfoVO> getUserInfoList(UserInfoQuery  query);

    /**
     * 获取单个用户信息
     *
     * @param id
     * @return
     */
    UserInfoVO getOneUserInfo(Integer id);

    /**
     * 更新用户头像
     *
     * @param file
     * @return
     */
    Result updateAvatar(MultipartFile file, Integer id);

    /**
     * 更新用户信息
     *
     * @param userInfoVO
     * @return
     */
    Result updateUserInfo(UserInfoVO userInfoVO) throws BusinessException;

    /**
     * 获取指导教师下拉选项（含名额信息）
     *
     * @return 教师选项列表
     */
    List<TeacherOptionVO> getTeacherOptions();

    /**
     * 搜索用户（添加好友时使用）
     *
     * @param keyword 关键字（昵称或用户名）
     * @param selfId  当前用户ID（搜索结果中排除自身）
     * @return 用户列表
     */
    List<UserInfoVO> searchUsers(String keyword, Integer selfId);

    /**
     * 删除用户信息
     *
     * @param ids
     * @return
     */
    Result deleteUserInfoByIds(List<Integer> ids);

    /**
     * 导出用户列表为 Excel
     *
     * @param query 筛选条件（与列表页相同）
     * @return Excel 行数据列表
     */
    List<UserExcelDTO> exportUserExcel(UserInfoQuery query);

    /**
     * 批量导入用户（Excel）
     *
     * @param file 上传的 Excel 文件
     * @return 导入结果（成功数/失败信息）
     */
    Result importUserExcel(MultipartFile file);
}
