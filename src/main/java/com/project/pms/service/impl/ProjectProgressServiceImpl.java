package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.pms.config.MinioConfig;
import com.project.pms.entity.po.ProjectInfo;
import com.project.pms.entity.po.ProjectProgress;
import com.project.pms.entity.query.ProjectProgressQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.enums.im.NotificationEnum;
import com.project.pms.mapper.ProjectInfoMapper;
import com.project.pms.mapper.ProjectProgressMapper;
import com.project.pms.service.INotificationService;
import com.project.pms.service.IProjectProgressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.pms.utils.MinioUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课题进度管理表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class ProjectProgressServiceImpl extends ServiceImpl<ProjectProgressMapper, ProjectProgress> implements IProjectProgressService {

    private final ProjectProgressMapper projectProgressMapper;
    private final ProjectInfoMapper projectInfoMapper;
    private final MinioUtil minioUtil;
    private final MinioConfig minioConfig;
    @Lazy
    private final INotificationService notificationService;

    @Override
    public PageResult<ProjectProgress> selectProjectProgressList(ProjectProgressQuery query) {
        Page<ProjectProgress> page = Page.of(query.getPageNo(), query.getPageSize());
        projectProgressMapper.selectProjectProgressList(page, query);
        List<ProjectProgress> list = page.getRecords();

        if (list != null) {
            for (ProjectProgress record : list) {
                String fileUrlInDb = record.getFileUrl();
                if (StringUtils.isNotBlank(fileUrlInDb)) {
                    if (fileUrlInDb.startsWith("http")) {
                        continue;
                    }
                    String longTermUrl = minioUtil.getPublicUrl(minioConfig.getBucketName(), fileUrlInDb);
                    record.setFileUrl(longTermUrl);
                }
            }
        }
        return PageResult.success(list, (int) page.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * 更新进度记录，审核状态变更时向学生发送通知
     */
    @Override
    public boolean updateById(ProjectProgress newProgress) {
        if (newProgress.getId() != null && newProgress.getStatus() != null) {
            ProjectProgress old = getById(newProgress.getId());
            boolean changed = old != null && !newProgress.getStatus().equals(old.getStatus());
            boolean result = super.updateById(newProgress);
            if (changed && old.getPId() != null) {
                ProjectInfo project = projectInfoMapper.selectById(old.getPId());
                if (project != null && project.getSid() != null) {
                    if (newProgress.getStatus() == 1) {
                        notificationService.send(project.getSid(), null, NotificationEnum.PROGRESS_REVIEWED.getType(),
                                NotificationEnum.PROGRESS_REVIEWED.getDesc(),
                                "您的进度「" + old.getTitle() + "」已被导师审阅通过",
                                old.getId());
                    } else if (newProgress.getStatus() == 2) {
                        notificationService.send(project.getSid(), null, NotificationEnum.PROGRESS_RETURNED.getType(),
                                NotificationEnum.PROGRESS_RETURNED.getDesc(),
                                "您的进度「" + old.getTitle() + "」被退回，请修改后重新提交" +
                                        (newProgress.getOpinion() != null ? "，意见：" + newProgress.getOpinion() : ""),
                                old.getId());
                    }
                }
            }
            return result;
        }
        return super.updateById(newProgress);
    }
}
