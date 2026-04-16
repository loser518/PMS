package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.pms.entity.po.ProjectInfo;
import com.project.pms.entity.po.TeacherProfile;
import com.project.pms.entity.po.Topic;
import com.project.pms.entity.po.User;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.TopicVO;
import com.project.pms.enums.ResponseCodeEnum;
import com.project.pms.exception.BusinessException;
import com.project.pms.mapper.ProjectInfoMapper;
import com.project.pms.mapper.TeacherProfileMapper;
import com.project.pms.mapper.TopicMapper;
import com.project.pms.mapper.UserMapper;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.ITopicService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 选题市场服务实现类
 *
 * @author loser
 * @since 2026-03-14
 */
@Service
@RequiredArgsConstructor
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements ITopicService {

    private final TopicMapper topicMapper;
    private final UserMapper userMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final ProjectInfoMapper projectInfoMapper;
    private final CurrentUser currentUser;

    @Override
    public void publishTopic(Topic topic) throws BusinessException {
        Integer tid = currentUser.getUserId();
        if (topic.getTitle() == null || topic.getTitle().isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "课题名称不能为空");
        }
        topic.setTid(tid);
        topic.setSelectedSid(null);
        if (topic.getStatus() == null) topic.setStatus(1);
        topic.setCreateTime(LocalDateTime.now());
        topic.setUpdateTime(LocalDateTime.now());
        topicMapper.insert(topic);
    }

    @Override
    public void updateTopic(Topic topic) throws BusinessException {
        Integer tid = currentUser.getUserId();
        Topic existing = topicMapper.selectById(topic.getId());
        if (existing == null || !existing.getTid().equals(tid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "课题不存在或无权修改");
        }
        topic.setUpdateTime(LocalDateTime.now());
        topicMapper.updateById(topic);
    }

    @Override
    public void removeTopic(Integer id) throws BusinessException {
        Integer tid = currentUser.getUserId();
        Topic existing = topicMapper.selectById(id);
        if (existing == null || !existing.getTid().equals(tid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "课题不存在或无权删除");
        }
        if (existing.getSelectedSid() != null) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "该课题已被学生选择，无法删除");
        }
        topicMapper.deleteById(id);
    }

    @Override
    public PageResult<TopicVO> listTopics(int pageNo, int pageSize, String keyword, Integer typeId, boolean onlyMine) {
        Integer uid = currentUser.getUserId();
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        if (onlyMine) {
            wrapper.eq(Topic::getTid, uid);
        } else {
            wrapper.eq(Topic::getStatus, 1); // 公开只显示已发布的
        }
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(Topic::getTitle, keyword);
        }

        wrapper.orderByDesc(Topic::getCreateTime);
        Page<Topic> page = new Page<>(pageNo, pageSize);
        topicMapper.selectPage(page, wrapper);
        List<TopicVO> vos = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.success(vos, (int) page.getTotal(), pageNo, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selectTopic(Integer topicId) throws BusinessException {
        Integer sid = currentUser.getUserId();
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null || topic.getStatus() != 1) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "课题不存在或已下架");
        }
        if (topic.getSelectedSid() != null) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "该课题已被其他同学选择");
        }

        // 标记课题已被选
        topicMapper.update(null, new LambdaUpdateWrapper<Topic>()
                .eq(Topic::getId, topicId)
                .set(Topic::getSelectedSid, sid)
                .set(Topic::getUpdateTime, LocalDateTime.now())
        );

        // 自动生成 ProjectInfo 申请
        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.setTitle(topic.getTitle());
        projectInfo.setTypeId(topic.getTypeId());
        projectInfo.setDescription(topic.getDescription());
        projectInfo.setSid(sid);
        projectInfo.setTid(topic.getTid());
        projectInfo.setStatus(0); // 待审核
        projectInfo.setCreateTime(new Date());
        projectInfo.setUpdateTime(new Date());
        projectInfoMapper.insert(projectInfo);
    }

    private TopicVO toVO(Topic topic) {
        TopicVO vo = new TopicVO();
        vo.setId(topic.getId());
        vo.setTid(topic.getTid());
        vo.setTitle(topic.getTitle());
        vo.setTypeId(topic.getTypeId());
        vo.setDescription(topic.getDescription());
        vo.setRequirement(topic.getRequirement());
        vo.setStatus(topic.getStatus());
        vo.setSelectedSid(topic.getSelectedSid());
        vo.setCreateTime(topic.getCreateTime() != null ? topic.getCreateTime().toString() : "");

        User teacher = userMapper.selectById(topic.getTid());
        if (teacher != null) {
            vo.setTeacherName(teacher.getNickname());
        }
        TeacherProfile profile = teacherProfileMapper.selectOne(
                new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getTid, topic.getTid())
        );
        if (profile != null) {
            vo.setTeacherTitle(profile.getTitle());
            vo.setTeacherCollege(profile.getCollege());
        }
        return vo;
    }
}
