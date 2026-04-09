package com.project.pms.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.pms.entity.excel.ProjectInfoExcelDTO;
import com.project.pms.entity.po.ProjectInfo;
import com.project.pms.entity.po.ProjectType;
import com.project.pms.entity.po.TeacherProfile;
import com.project.pms.entity.po.User;
import com.project.pms.entity.query.ProjectInfoQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.enums.ResponseCodeEnum;
import com.project.pms.enums.im.NotificationEnum;
import com.project.pms.exception.BusinessException;
import com.project.pms.mapper.ProjectInfoMapper;
import com.project.pms.mapper.ProjectTypeMapper;
import com.project.pms.mapper.TeacherProfileMapper;
import com.project.pms.mapper.UserMapper;
import com.project.pms.service.INotificationService;
import com.project.pms.service.IProjectInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 课题项目申报信息表 服务实现类
 * </p>
 *
 * @author loser
 * @since 2026-02-09
 */
@Service
@RequiredArgsConstructor
public class ProjectInfoServiceImpl extends ServiceImpl<ProjectInfoMapper, ProjectInfo> implements IProjectInfoService {
    private static final Logger log = LoggerFactory.getLogger(ProjectInfoServiceImpl.class);
    private final ProjectInfoMapper projectInfoMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final UserMapper userMapper;
    private final ProjectTypeMapper projectTypeMapper;
    @Lazy
    private final INotificationService notificationService;

    @Override
    public PageResult<ProjectInfo> selectProjectInfoList(ProjectInfoQuery query) {
        Page<ProjectInfo> page = Page.of(query.getPageNo(), query.getPageSize());
        projectInfoMapper.selectProjectInfoList(page, query);
        List<ProjectInfo> list = page.getRecords();
        return PageResult.success(list, (int) page.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * 学生提交课题申报（含指导教师名额校验）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithAdvisorCheck(ProjectInfo projectInfo) throws BusinessException {
        LambdaQueryWrapper<ProjectInfo> query = new LambdaQueryWrapper<>();
        query.eq(ProjectInfo::getTitle, projectInfo.getTitle());
        ProjectInfo project = getOne(query);
        if (project != null) {
            throw new BusinessException("课题名称已存在，请更换！");
        }

        if (projectInfo.getTid() != null) {
            checkAndGetProfile(projectInfo.getTid(), true);
        }
        save(projectInfo);
    }

    /**
     * 更新课题信息（含审核通过/撤销时的教师指导人数维护）
     * 规则：
     * 1. status 变为 1（通过）→ 教师 +1
     * 2. status 从 1 变为其他 → 教师 -1
     * 3. tid 发生变更（均需结合 status 判断）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWithAdvisorCount(ProjectInfo newInfo) throws BusinessException {
        if (newInfo.getId() == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "课题ID不能为空");
        }
        // 课题名称唯一性校验：排除自身，防止仅修改其他字段时误报重名
        if (newInfo.getTitle() != null) {
            long count = count(new LambdaQueryWrapper<ProjectInfo>()
                    .eq(ProjectInfo::getTitle, newInfo.getTitle())
                    .ne(ProjectInfo::getId, newInfo.getId())
            );
            if (count > 0) {
                throw new BusinessException("课题名称已存在，请更换！");
            }
        }

        // 获取旧记录
        ProjectInfo oldInfo = getById(newInfo.getId());
        if (oldInfo == null) {
            updateById(newInfo);
            return;
        }

        Integer oldStatus = oldInfo.getStatus();
        Integer newStatus = newInfo.getStatus();
        Integer oldTid = oldInfo.getTid();
        Integer newTid = newInfo.getTid();

        boolean tidChanged = newTid != null && !newTid.equals(oldTid);
        boolean approvedNow = newStatus != null && newStatus == 1 && (oldStatus == null || oldStatus != 1);
        boolean revokedNow = newStatus != null && newStatus != 1 && oldStatus != null && oldStatus == 1;

        if (tidChanged) {
            // 教师发生变更
            // 若旧 tid 有效且旧状态是通过，则旧教师 -1
            if (oldTid != null && oldStatus != null && oldStatus == 1) {
                decrementCount(oldTid);
            }
            // 若新状态为通过，则新教师校验名额并 +1
            if (newStatus != null && newStatus == 1) {
                TeacherProfile profile = checkAndGetProfile(newTid, true);
                incrementCount(newTid, profile);
            } else {
                // tid 变更但新状态不是通过，仅校验名额（不增加计数）
                if (newTid != null) {
                    checkAndGetProfile(newTid, true);
                }
            }
        } else {
            // tid 未变更，仅处理 status 变化
            if (approvedNow && newTid != null) {
                // 新通过：+1
                TeacherProfile profile = checkAndGetProfile(newTid, true);
                incrementCount(newTid, profile);
            } else if (revokedNow && oldTid != null) {
                // 撤销通过：-1
                decrementCount(oldTid);
            }
        }

        updateById(newInfo);

        // 审核结果变更时，向学生发送通知
        if (oldInfo.getSid() != null && newStatus != null && !newStatus.equals(oldStatus)) {
            int notifyType;
            String notifyTitle;
            String notifyContent;
            if (newStatus == 1) {
                notifyType = NotificationEnum.PROJECT_APPROVED.getType();
                notifyTitle = NotificationEnum.PROJECT_APPROVED.getDesc();
                notifyContent = "您的课题「" + oldInfo.getTitle() + "」审核已通过，恭喜！";
            } else if (newStatus == 2) {
                notifyType = NotificationEnum.PROJECT_REJECTED.getType();
                notifyTitle = NotificationEnum.PROJECT_REJECTED.getDesc();
                notifyContent = "您的课题「" + oldInfo.getTitle() + "」审核未通过" +
                        (newInfo.getOpinion() != null ? "，意见：" + newInfo.getOpinion() : "");
            } else if (newStatus == 3) {
                notifyType = NotificationEnum.PROJECT_NEED_MODIFY.getType();
                notifyTitle = NotificationEnum.PROJECT_NEED_MODIFY.getDesc();
                notifyContent = "您的课题「" + oldInfo.getTitle() + "」需要修改" +
                        (newInfo.getOpinion() != null ? "，意见：" + newInfo.getOpinion() : "");
            } else {
                notifyType = NotificationEnum.SYSTEM_MESSAGE.getType();
                notifyTitle = "课题状态更新";
                notifyContent = "您的课题「" + oldInfo.getTitle() + "」状态已更新";
            }
            notificationService.send(oldInfo.getSid(), null, notifyType, notifyTitle, notifyContent, oldInfo.getId());
        }
    }

    /**
     * 删除课题信息，若课题已审核通过则同步教师计数 -1
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeWithAdvisorCount(Integer id) {
        ProjectInfo info = getById(id);
        if (info != null && info.getStatus() != null && info.getStatus() == 1 && info.getTid() != null) {
            decrementCount(info.getTid());
        }
        removeById(id);
    }

    // -------- 私有辅助方法 --------

    /**
     * 校验教师名额，返回教师档案
     *
     * @param tid       教师用户ID
     * @param checkFull 是否检查满额
     */
    private TeacherProfile checkAndGetProfile(Integer tid, boolean checkFull) throws BusinessException {
        TeacherProfile profile = teacherProfileMapper.selectOne(
                new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getTid, tid)
        );
        if (profile == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "未找到该教师档案，请确认教师ID是否正确");
        }
        if (checkFull) {
            Integer max = profile.getMaxStudentCount();
            Integer cur = profile.getCurrentStudentCount() == null ? 0 : profile.getCurrentStudentCount();
            if (max != null && max > 0 && cur >= max) {
                throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(),
                        "该教师指导名额已满（" + cur + "/" + max + "），请选择其他教师");
            }
        }
        return profile;
    }

    /**
     * 教师当前指导人数 +1
     */
    private void incrementCount(Integer tid, TeacherProfile profile) {
        int cur = profile.getCurrentStudentCount() == null ? 0 : profile.getCurrentStudentCount();
        teacherProfileMapper.update(null,
                new LambdaUpdateWrapper<TeacherProfile>()
                        .eq(TeacherProfile::getTid, tid)
                        .set(TeacherProfile::getCurrentStudentCount, cur + 1)
        );
    }

    /**
     * 教师当前指导人数 -1（最小为 0）
     */
    private void decrementCount(Integer tid) {
        TeacherProfile profile = teacherProfileMapper.selectOne(
                new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getTid, tid)
        );
        if (profile == null) return;
        int cur = profile.getCurrentStudentCount() == null ? 0 : profile.getCurrentStudentCount();
        if (cur > 0) {
            teacherProfileMapper.update(null,
                    new LambdaUpdateWrapper<TeacherProfile>()
                            .eq(TeacherProfile::getTid, tid)
                            .set(TeacherProfile::getCurrentStudentCount, cur - 1)
            );
        }
    }

    // ==================== Excel 导入导出 ====================

    /**
     * 导出课题列表为 Excel 行数据（不分页，全量导出）
     */
    @Override
    public List<ProjectInfoExcelDTO> exportProjectExcel(ProjectInfoQuery query) {
        // 全量查询
        Page<ProjectInfo> page = Page.of(1, Integer.MAX_VALUE);
        projectInfoMapper.selectProjectInfoList(page, query);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return page.getRecords().stream().map(info -> {
            ProjectInfoExcelDTO dto = new ProjectInfoExcelDTO();
            dto.setTitle(info.getTitle());
            dto.setDescription(info.getDescription());
            dto.setStatus(projectStatusToText(info.getStatus()));
            dto.setOpinion(info.getOpinion());
            dto.setCreateTime(info.getCreateTime() != null ? sdf.format(info.getCreateTime()) : "");

            // 课题类型名称
            if (info.getTypeId() != null) {
                ProjectType type = projectTypeMapper.selectById(info.getTypeId());
                dto.setTypeName(type != null ? type.getName() : "");
            }
            // 学生信息
            if (info.getSid() != null) {
                User student = userMapper.selectById(info.getSid());
                if (student != null) {
                    dto.setStudentName(student.getNickname());
                    dto.setStudentUsername(student.getUsername());
                }
            }
            // 教师信息
            if (info.getTid() != null) {
                User teacher = userMapper.selectById(info.getTid());
                if (teacher != null) {
                    dto.setTeacherName(teacher.getNickname());
                    dto.setTeacherUsername(teacher.getUsername());
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 批量导入课题（Excel）
     * 规则：课题名称已存在则跳过；学生/教师按工号/学号匹配
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result importProjectExcel(MultipartFile file) {
        List<ProjectInfoExcelDTO> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(ProjectInfoExcelDTO.class)
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

        int successCount = 0;
        List<String> failList = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            ProjectInfoExcelDTO row = rows.get(i);
            int rowNum = i + 2;
            try {
                String title = row.getTitle();
                if (StringUtils.isBlank(title)) {
                    failList.add("第" + rowNum + "行：课题名称不能为空");
                    continue;
                }
                // 课题名称已存在则跳过
                if (getOne(new LambdaQueryWrapper<ProjectInfo>().eq(ProjectInfo::getTitle, title)) != null) {
                    failList.add("第" + rowNum + "行：课题「" + title + "」已存在，已跳过");
                    continue;
                }

                ProjectInfo info = new ProjectInfo();
                info.setTitle(title);
                info.setDescription(row.getDescription());
                info.setStatus(0); // 导入默认待审核

                // 解析课题类型（按名称匹配）
                if (StringUtils.isNotBlank(row.getTypeName())) {
                    ProjectType type = projectTypeMapper.selectOne(
                            new LambdaQueryWrapper<ProjectType>().eq(ProjectType::getName, row.getTypeName().trim())
                    );
                    if (type != null) info.setTypeId(type.getId());
                }

                // 解析学生（优先按学号精准匹配，否则按姓名）
                if (StringUtils.isNotBlank(row.getStudentUsername())) {
                    User student = userMapper.selectOne(
                            new LambdaQueryWrapper<User>().eq(User::getUsername, row.getStudentUsername().trim()).eq(User::getRole, 0)
                    );
                    if (student != null) info.setSid(student.getId());
                } else if (StringUtils.isNotBlank(row.getStudentName())) {
                    User student = userMapper.selectOne(
                            new LambdaQueryWrapper<User>().eq(User::getNickname, row.getStudentName().trim()).eq(User::getRole, 0)
                    );
                    if (student != null) info.setSid(student.getId());
                }

                // 解析教师（优先按工号精准匹配，否则按姓名）
                if (StringUtils.isNotBlank(row.getTeacherUsername())) {
                    User teacher = userMapper.selectOne(
                            new LambdaQueryWrapper<User>().eq(User::getUsername, row.getTeacherUsername().trim()).eq(User::getRole, 1)
                    );
                    if (teacher != null) info.setTid(teacher.getId());
                } else if (StringUtils.isNotBlank(row.getTeacherName())) {
                    User teacher = userMapper.selectOne(
                            new LambdaQueryWrapper<User>().eq(User::getNickname, row.getTeacherName().trim()).eq(User::getRole, 1)
                    );
                    if (teacher != null) info.setTid(teacher.getId());
                }

                info.setCreateTime(new Date());
                info.setUpdateTime(new Date());
                save(info);
                successCount++;
            } catch (Exception e) {
                log.error("导入课题第{}行失败", rowNum, e);
                failList.add("第" + rowNum + "行：导入异常 - " + e.getMessage());
            }
        }

        String msg = "导入完成：成功 " + successCount + " 条";
        if (!failList.isEmpty()) {
            msg += "，失败/跳过 " + failList.size() + " 条：" + String.join("；", failList.subList(0, Math.min(failList.size(), 5)));
        }
        return Result.success(Map.of("successCount", successCount, "failList", failList), msg);
    }

    private String projectStatusToText(Integer status) {
        if (status == null) return "待审核";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "审核通过";
            case 2 -> "驳回";
            case 3 -> "需修改";
            default -> status.toString();
        };
    }
}
