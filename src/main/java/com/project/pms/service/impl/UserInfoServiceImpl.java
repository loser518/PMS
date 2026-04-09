package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.pms.config.MinioConfig;
import com.project.pms.entity.constants.Constants;
import com.project.pms.entity.dto.UserDto;
import com.project.pms.entity.excel.UserExcelDTO;
import com.project.pms.entity.po.StudentProfile;
import com.project.pms.entity.po.TeacherProfile;
import com.project.pms.entity.po.User;
import com.project.pms.entity.query.UserInfoQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.entity.vo.TeacherOptionVO;
import com.project.pms.entity.vo.UserInfoVO;
import com.project.pms.enums.ResponseCodeEnum;
import com.project.pms.exception.BusinessException;
import com.project.pms.mapper.StudentProfileMapper;
import com.project.pms.mapper.TeacherProfileMapper;
import com.project.pms.mapper.UserMapper;
import com.project.pms.service.UserInfoService;
import com.project.pms.utils.CopyUtil;
import com.project.pms.utils.MinioUtil;
import com.project.pms.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @className: UserInfoServiceImpl
 * @description: 用户信息服务实现类
 * @author: loser
 * @createTime: 2026/2/3 11:07
 */
@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl implements UserInfoService {
    private static final Logger log = LoggerFactory.getLogger(UserInfoServiceImpl.class);
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final MinioUtil minioUtil;
    private final MinioConfig minioConfig;
    private final Executor taskExecutor;
    private final StudentProfileMapper studentProfileMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final ObjectMapper objectMapper;

    /**
     * 获取用户信息列表
     *
     * @param query
     * @return
     */
    @Override
    public PageResult<UserInfoVO> getUserInfoList(UserInfoQuery query) {
        Page<User> page = new Page<>(query.getPageNo(), query.getPageSize());
        //先从redis中获取数据
//        List<UserInfoVO> redisResult = redisUtil.getAllList(Constants.REDIS_KEY_USER_LIST, UserInfoVO.class);
//
//        if (!redisResult.isEmpty()) {
//            return PageResult.success(redisResult,
//                    redisResult.size(),
//                    pageNo,
//                    pageSize);
//        }

        // 执行分页查询
        userMapper.selectUserInfoList(page, query);

        List<UserInfoVO> userInfoList = page.getRecords().stream()
                .map(this::convertToUserInfoVO)
                .collect(Collectors.toList());

        // 缓存数据到redis
//        redisUtil.setAllList(Constants.REDIS_KEY_USER_LIST, userInfoList);

        // 构建返回结果
        return PageResult.success(
                userInfoList,
                (int) page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize()
        );
    }


    /**
     * 获取单个用户信息
     *
     * @param id
     * @return
     */
    @Override
    public UserInfoVO getOneUserInfo(Integer id) {
        User user = redisUtil.getObject(Constants.REDIS_KEY_USER + id, User.class);

        // 如果redis中没有user数据，就从mysql中获取并更新到redis
        if (user == null) {
            user = userMapper.selectById(id);
            if (user == null) {
                return null;
            }
            User finalUser = user;
            CompletableFuture.runAsync(() -> {
                redisUtil.setExObjectValue(Constants.REDIS_KEY_USER + finalUser.getId(), finalUser);
            }, taskExecutor);
        }

        UserDto userDto = CopyUtil.copy(user, UserDto.class);
        Integer role = userDto.getRole();

        if (role == 0) {
            StudentProfile profile = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>()
                            .eq(StudentProfile::getSid, userDto.getId())
            );
            return UserInfoVO.forStudent(userDto, profile);
        } else if (role == 1) {
            TeacherProfile profile = teacherProfileMapper.selectOne(
                    new LambdaQueryWrapper<TeacherProfile>()
                            .eq(TeacherProfile::getTid, userDto.getId())
            );
            return UserInfoVO.forTeacher(userDto, profile);
        } else {
            return UserInfoVO.forAdmin(userDto);
        }
    }

    /**
     * 更新用户头像
     *
     * @param file
     * @param id
     * @return
     */
    @Override
    public Result updateAvatar(MultipartFile file, Integer id) {
        if (file == null) {
            return Result.error(ResponseCodeEnum.CODE_602.getCode(), "文件为空，请选择文件！");
        }
        String fileName = minioUtil.uploadFile(file, minioConfig.getBucketName());

        String avatarUrl = minioUtil.getPublicUrl(minioConfig.getBucketName(), fileName);

        User user = userMapper.selectById(id);
        LambdaUpdateWrapper<User> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(User::getId, id).set(User::getAvatar, fileName);
        userMapper.update(null, lambdaUpdateWrapper);

        String oldAvatar = minioUtil.getFilename(user.getAvatar());
        minioUtil.removeFile(oldAvatar);

        redisUtil.removeCache(Constants.REDIS_KEY_USER + id);
        return Result.success(avatarUrl, "头像更新成功！");
    }

    /**
     * 更新用户信息
     *
     * @param userInfoVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateUserInfo(UserInfoVO userInfoVO) throws BusinessException {
        Integer id = userInfoVO.getUser().getId();
        String username = userInfoVO.getUser().getUsername();
        String nickname = userInfoVO.getUser().getNickname();
        Integer role = userInfoVO.getUser().getRole();
        UserDto userDto = userInfoVO.getUser();
        Object profileObj = userInfoVO.getProfile();
        StudentProfile studentProfile = null;
        TeacherProfile teacherProfile = null;

        if (username == null || username.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "用户名不能为空！");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).ne(User::getId, id)) != null) {
            throw new BusinessException(ResponseCodeEnum.CODE_601.getCode(), "用户名已存在！");
        }
        if (nickname == null || nickname.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "昵称不能为空！");
        }
        if (role == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "角色不能为空！");
        }

        User user = CopyUtil.copy(userDto, User.class);
        user.setUpdateTime(new Date());
        userMapper.update(user, new LambdaQueryWrapper<User>().eq(User::getId, id));

        if (profileObj instanceof Map) {
            // 如果是 Map（从 JSON 反序列化而来）
            if (role == 0) {
                studentProfile = objectMapper.convertValue(profileObj, StudentProfile.class);
            } else {
                teacherProfile = objectMapper.convertValue(profileObj, TeacherProfile.class);
            }
        } else if (profileObj instanceof StudentProfile) {
            studentProfile = (StudentProfile) profileObj;
        } else if (profileObj instanceof TeacherProfile) {
            teacherProfile = (TeacherProfile) profileObj;
        } else if (profileObj == null) {
            redisUtil.removeCache(Constants.REDIS_KEY_USER + id);
            return Result.success("更新管理员信息成功！");
        }
        if (role == 0) {
            if (studentProfile != null) {
                studentProfile.setUpdateTime(new Date());
            }
            studentProfileMapper.update(studentProfile, new LambdaUpdateWrapper<StudentProfile>().eq(StudentProfile::getSid, id));
        } else if (role == 1) {
            if (teacherProfile != null) {
                teacherProfile.setUpdateTime(new Date());
            }
            teacherProfileMapper.update(teacherProfile, new LambdaUpdateWrapper<TeacherProfile>().eq(TeacherProfile::getTid, id));
        } else {
            redisUtil.removeCache(Constants.REDIS_KEY_USER + id);
            return Result.success("更新管理员信息成功！");
        }
        redisUtil.removeCache(Constants.REDIS_KEY_USER + id);
        return Result.success("更新用户信息成功！");
    }

    /**
     * 删除用户信息
     *
     * @param ids
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteUserInfoByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success("删除用户信息成功！");
        }

        for (Integer id : ids) {
            User user = userMapper.selectById(id);
            if (user == null) {
                continue;
            }

            // 删除关联的学生或教师档案
            if (user.getRole() == 0) {
                studentProfileMapper.delete(
                        new LambdaQueryWrapper<StudentProfile>()
                                .eq(StudentProfile::getSid, id)
                );
            } else if (user.getRole() == 1) {
                teacherProfileMapper.delete(
                        new LambdaQueryWrapper<TeacherProfile>()
                                .eq(TeacherProfile::getTid, id)
                );
            }

            userMapper.deleteById(id);
            redisUtil.removeCache(Constants.REDIS_KEY_USER + id);
        }

        return Result.success("删除用户信息成功！");
    }

    /**
     * 获取指导教师下拉选项（含名额信息）
     * 查询所有 role=1 的教师用户及其档案信息
     *
     * @return 教师选项列表
     */
    @Override
    public List<TeacherOptionVO> getTeacherOptions() {
        // 查询所有角色为教师的用户
        List<User> teachers = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, 1)
        );
        return teachers.stream().map(user -> {
            TeacherProfile profile = teacherProfileMapper.selectOne(
                    new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getTid, user.getId())
            );
            TeacherOptionVO vo = new TeacherOptionVO();
            vo.setTid(user.getId());
            vo.setNickname(user.getNickname());
            if (profile != null) {
                vo.setTitle(profile.getTitle());
                vo.setResearchField(profile.getResearchField());
                vo.setCollege(profile.getCollege());
                vo.setMaxStudentCount(profile.getMaxStudentCount());
                vo.setCurrentStudentCount(profile.getCurrentStudentCount());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 搜索用户（用于添加好友），按昵称或用户名模糊匹配，排除自身
     */
    @Override
    public List<UserInfoVO> searchUsers(String keyword, Integer selfId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String kw = "%" + keyword.trim() + "%";
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .ne(User::getId, selfId)
                        .and(w -> w.like(User::getNickname, keyword.trim())
                                   .or()
                                   .like(User::getUsername, keyword.trim()))
                        .last("LIMIT 20")
        );
        return users.stream().map(this::convertToUserInfoVO).collect(Collectors.toList());
    }

    /**
     * 转换User为UserInfoVO
     */
    private UserInfoVO convertToUserInfoVO(User user) {
        if (user == null) {
            return null;
        }

        UserDto userDto = CopyUtil.copy(user, UserDto.class);
        Integer role = user.getRole();

        if (role == 0) { // 学生
            StudentProfile profile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getSid, userDto.getId()));
            return UserInfoVO.forStudent(userDto, profile);
        } else if (role == 1) { // 指导老师
            TeacherProfile profile = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getTid, userDto.getId()));
            return UserInfoVO.forTeacher(userDto, profile);
        } else { // 管理员
            return UserInfoVO.forAdmin(userDto);
        }
    }

    // ==================== Excel 导入导出 ====================

    /**
     * 导出用户列表为 Excel 行数据
     * 不分页，将筛选条件匹配的所有用户全量导出
     */
    @Override
    public List<UserExcelDTO> exportUserExcel(UserInfoQuery query) {
        // 不分页：pageNo=1, pageSize=Integer.MAX_VALUE
        Page<User> page = new Page<>(1, Integer.MAX_VALUE);
        userMapper.selectUserInfoList(page, query);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return page.getRecords().stream().map(user -> {
            UserExcelDTO dto = new UserExcelDTO();
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setRole(roleToText(user.getRole()));
            dto.setPhone(user.getPhone());
            dto.setEmail(user.getEmail());
            dto.setStatus(statusToText(user.getStatus()));
            dto.setCreateTime(user.getCreateTime() != null ? sdf.format(user.getCreateTime()) : "");
            // 学生额外字段
            if (user.getRole() == 0) {
                StudentProfile sp = studentProfileMapper.selectOne(
                        new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getSid, user.getId()));
                if (sp != null) {
                    dto.setGrade(sp.getGrade());
                    dto.setMajor(sp.getMajor());
                    dto.setCollege(sp.getCollege());
                    dto.setClassName(sp.getClassName());
                    dto.setEnrollmentYear(sp.getEnrollmentYear() != null ? sp.getEnrollmentYear().toString() : "");
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 批量导入用户（Excel）
     * 规则：用户名已存在则跳过；默认密码 lsy123456；学生自动创建 student_profile
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result importUserExcel(MultipartFile file) {
        List<UserExcelDTO> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(UserExcelDTO.class)
                    .sheet()
                    .headRowNumber(1)
                    .doReadSync();
        } catch (Exception e) {
            log.error("解析Excel失败", e);
            return Result.error("Excel 文件解析失败，请检查文件格式");
        }

        if (rows == null || rows.isEmpty()) {
            return Result.error("Excel 内容为空");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        int successCount = 0;
        List<String> failList = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            UserExcelDTO row = rows.get(i);
            int rowNum = i + 2; // Excel 第几行（含表头偏移）
            try {
                String username = row.getUsername();
                if (StringUtils.isBlank(username)) {
                    failList.add("第" + rowNum + "行：用户名不能为空");
                    continue;
                }
                // 用户名已存在则跳过
                if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) != null) {
                    failList.add("第" + rowNum + "行：用户名 [" + username + "] 已存在，已跳过");
                    continue;
                }
                // 解析角色
                int role = textToRole(row.getRole());

                // 构建用户
                User user = new User();
                user.setUsername(username);
                user.setNickname(StringUtils.isBlank(row.getNickname()) ? username : row.getNickname());
                user.setPassword(encoder.encode(
                        StringUtils.isBlank(row.getPassword()) ? "lsy123456" : row.getPassword()
                ));
                user.setRole(role);
                user.setPhone(row.getPhone());
                user.setEmail(row.getEmail());
                user.setAvatar(Constants.AVATAR_URL);
                user.setBackground(Constants.BG_URL);
                user.setStatus(0);
                user.setGender(2);
                user.setCreateTime(new Date());
                user.setUpdateTime(new Date());
                userMapper.insert(user);

                // 学生自动创建 student_profile
                if (role == 0) {
                    StudentProfile sp = new StudentProfile();
                    sp.setSid(user.getId());
                    sp.setGrade(row.getGrade());
                    sp.setMajor(row.getMajor());
                    sp.setCollege(row.getCollege());
                    sp.setClassName(row.getClassName());
                    if (StringUtils.isNotBlank(row.getEnrollmentYear())) {
                        try { sp.setEnrollmentYear(Integer.parseInt(row.getEnrollmentYear().trim())); } catch (NumberFormatException ignore) {}
                    }
                    sp.setCreateTime(new Date());
                    sp.setUpdateTime(new Date());
                    studentProfileMapper.insert(sp);
                }

                successCount++;
            } catch (Exception e) {
                log.error("导入第{}行失败", rowNum, e);
                failList.add("第" + rowNum + "行：导入异常 - " + e.getMessage());
            }
        }

        String msg = "导入完成：成功 " + successCount + " 条";
        if (!failList.isEmpty()) {
            msg += "，失败/跳过 " + failList.size() + " 条：" + String.join("；", failList.subList(0, Math.min(failList.size(), 5)));
        }
        return Result.success(Map.of("successCount", successCount, "failList", failList), msg);
    }

    // -------- 工具方法 --------

    private String roleToText(Integer role) {
        if (role == null) return "";
        return switch (role) {
            case 0 -> "0-学生";
            case 1 -> "1-教师";
            case 2 -> "2-管理员";
            default -> role.toString();
        };
    }

    private int textToRole(String text) {
        if (StringUtils.isBlank(text)) return 0;
        String t = text.trim();
        if (t.startsWith("0")) return 0;
        if (t.startsWith("1")) return 1;
        if (t.startsWith("2")) return 2;
        return 0;
    }

    private String statusToText(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "正常";
            case 1 -> "封禁";
            case 2 -> "注销";
            default -> status.toString();
        };
    }
}
