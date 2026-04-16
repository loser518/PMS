package com.project.pms.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.pms.entity.po.Announcement;
import com.project.pms.entity.po.ProjectInfo;
import com.project.pms.entity.po.ProjectProgress;
import com.project.pms.entity.po.User;
import com.project.pms.enums.user.UserRoleEnum;
import com.project.pms.mapper.AnnouncementMapper;
import com.project.pms.mapper.ProjectInfoMapper;
import com.project.pms.mapper.ProjectProgressMapper;
import com.project.pms.mapper.UserMapper;
import com.project.pms.service.IProjectInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能客服 Function Calling 工具集
 * 注意：Tool 方法由 Spring AI 在 Reactor boundedElastic 线程中调用，
 * 该线程与 Servlet 主线程不同，Spring Security 的 ThreadLocal SecurityContext 不会传递。
 * 因此所有需要"当前用户"的 Tool 均通过参数接收 userId，不使用 CurrentUser/SecurityContextHolder。
 * @author loser
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerServiceTools {

    private final ProjectInfoMapper projectInfoMapper;
    private final ProjectProgressMapper projectProgressMapper;
    private final AnnouncementMapper announcementMapper;
    private final IProjectInfoService projectInfoService;
    private final UserMapper userMapper;

    @Tool(description = "查询当前登录学生的所有课题申报记录，返回课题名称、类型、审核状态和审核意见。" +
            "状态说明：0=待审核，1=审核通过，2=驳回，3=需修改。" +
            "参数 userId 为当前登录用户的ID，由系统自动提供，无需用户输入。")
    public String getMyProjects(Integer userId) {
        try {
            List<ProjectInfo> list = projectInfoMapper.selectList(
                    new LambdaQueryWrapper<ProjectInfo>().eq(ProjectInfo::getSid, userId)
            );
            if (list == null || list.isEmpty()) {
                return "您目前没有任何课题申报记录。";
            }
            return list.stream().map(p -> {
                String statusStr = switch (p.getStatus()) {
                    case 0 -> "待审核";
                    case 1 -> "审核通过";
                    case 2 -> "已驳回";
                    case 3 -> "需修改";
                    default -> "未知状态";
                };
                String opinion = (p.getOpinion() != null && !p.getOpinion().isBlank())
                        ? "，审核意见：" + p.getOpinion() : "";
                return String.format("【%s】类型：%s，状态：%s%s", p.getTitle(), p.getTypeId(), statusStr, opinion);
            }).collect(Collectors.joining("\n"));
        } catch (Exception e) {
//            log.error("getMyProjects 调用失败 userId={}", userId, e);
            return "查询失败，请稍后再试。";
        }
    }

    @Tool(description = "查询当前登录学生指定课题的进度提交记录。" +
            "参数 projectId 为课题ID（整数）。进度状态：0=待审核，1=导师已阅，2=驳回。")
    public String getProjectProgress(Integer projectId) {
        try {
            List<ProjectProgress> list = projectProgressMapper.selectList(
                    new LambdaQueryWrapper<ProjectProgress>()
                            .eq(ProjectProgress::getPId, projectId)
                            .orderByDesc(ProjectProgress::getSubmitTime)
            );
            if (list == null || list.isEmpty()) {
                return "该课题暂无进度提交记录。";
            }
            return list.stream().map(pp -> {
                String statusStr = switch (pp.getStatus()) {
                    case 0 -> "待审核";
                    case 1 -> "导师已阅";
                    case 2 -> "已驳回";
                    default -> "未知";
                };
                String opinion = (pp.getOpinion() != null && !pp.getOpinion().isBlank())
                        ? "，导师意见：" + pp.getOpinion() : "";
                String time = pp.getSubmitTime() != null ? pp.getSubmitTime().toString() : "未知时间";
                return String.format("【%s】%s | 状态：%s%s | 提交时间：%s",
                        pp.getTitle(), pp.getContent(), statusStr, opinion, time);
            }).collect(Collectors.joining("\n"));
        } catch (Exception e) {
//            log.error("getProjectProgress 调用失败 projectId={}", projectId, e);
            return "查询失败，请稍后再试。";
        }
    }

    @Tool(description = "查询最新发布的系统公告（最多5条），包含公告标题、优先级和发布时间。" +
            "优先级：0=普通，1=紧急，2=置顶。只返回已发布（status=1）的公告。")
    public String getLatestAnnouncements() {
        try {
            List<Announcement> list = announcementMapper.selectList(
                    new LambdaQueryWrapper<Announcement>()
                            .eq(Announcement::getStatus, 1)
                            .orderByDesc(Announcement::getPriority)
                            .orderByDesc(Announcement::getId)
                            .last("LIMIT 5")
            );
            if (list == null || list.isEmpty()) {
                return "目前没有已发布的公告。";
            }
            return list.stream().map(a -> {
                String priorityStr = switch (a.getPriority()) {
                    case 1 -> "紧急";
                    case 2 -> "置顶";
                    default -> "普通";
                };
                return String.format("【%s】%s | %s", priorityStr, a.getTitle(), a.getContent());
            }).collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
//            log.error("getLatestAnnouncements 调用失败", e);
            return "查询失败，请稍后再试。";
        }
    }


    @Tool(description = "获取PMS项目管理系统的功能介绍和使用说明，用于回答用户关于系统如何使用的问题。")
    public String getSystemHelp() {
        return """
                PMS（项目管理系统）功能说明：
               
                【课题申报】（学生）
                - 在"我的课题"页面点击"新建申报"，填写课题名称、类型、描述，选择指导教师后提交。
                - 审核结果会通过站内通知告知，状态分为：待审核、审核通过、驳回、需修改。
                - 若被驳回或需修改，可编辑后重新提交。
                                
                【进度提交】（学生）
                - 课题审核通过后，可在"课题进度"页面提交阶段性进度报告。
                - 每次提交需填写阶段名称和工作内容，支持上传附件。
                - 导师审阅后会给出"已阅"或"驳回"的反馈。
                                
                【课题审核】（教师/管理员）
                - 在"我的课题"页面可看到待审核的学生申报。
                - 可填写审核意见并设置状态（通过/驳回/需修改）。
                                
                【公告系统】
                - 管理员可发布面向全体/学生/教师的公告通知。
                - 紧急公告和置顶公告会优先展示。
                                
                【即时通讯】
                - 添加好友后可进行实时聊天。
                - 在线状态实时更新（绿点=在线）。
                                
                【AI客服】
                - 可询问AI客服关于系统的功能。
                - 可以帮助学生或指导老师实现一些简单的任务。
                """;
    }


    /**
     * 帮助当前登录的学生申报课题
     *
     * @param title
     * @param typeId
     * @param description
     * @param sid
     * @param tid
     * @return
     */
    @Tool(description = "帮助当前登录的学生申报课题，用于回答学生申报课题问题，如果没有指导教师ID,直接设置为null，不要设置为0")
    @Transactional(rollbackFor = Exception.class)
    public void insertProject(@ToolParam(description = "课题名称") String title,
                              @ToolParam(description = "课题类型") Integer typeId,
                              @ToolParam(description = "课题描述/研究内容") String description,
                              @ToolParam(description = "申报学生ID") Integer sid,
                              @ToolParam(description = "指导教师ID") Integer tid) {
        try {
            // 参数校验
            if (title == null) {
                log.error("课题名称为空");
                throw new RuntimeException("课题名称为空");
            }

            // 创建新对象
            ProjectInfo projectInfo = new ProjectInfo();
            projectInfo.setTitle(title);
            projectInfo.setDescription(description);
            projectInfo.setTypeId(typeId);
            projectInfo.setSid(sid);
            projectInfo.setTid(tid);

            // 设置时间
            Date now = new Date();
            projectInfo.setCreateTime(now);
            projectInfo.setUpdateTime(now);

            // 设置默认状态
            projectInfo.setStatus(0);

//            log.info("准备插入的数据: {}", projectInfo);

            // 执行插入
            projectInfoService.saveWithAdvisorCheck(projectInfo);

        } catch (Exception e) {
            throw new RuntimeException("课题申报失败: " + e.getMessage());
        }
    }

    @Tool(description = "当学生提及课题进度提交时调用，帮助学生提交相应课题的进度")
    public void submitProjectProgress(@ToolParam(description = "课题ID") Integer pid,
                                      @ToolParam(description = "进度名称") String title,
                                      @ToolParam(description = "进度内容") String content) {
        // 创建进度对象
        ProjectProgress projectProgress = new ProjectProgress();
        projectProgress.setPId(pid);
        projectProgress.setTitle(title);
        projectProgress.setContent(content);
        projectProgress.setSubmitTime(LocalDateTime.now());

        // 执行插入
        projectProgressMapper.insert(projectProgress);
    }

    /**
     * 根据用户名或者昵称查找知道老师Id
     *
     * @param username
     * @param nickname
     * @return
     */
    @Tool(description = "根据用户名或者昵称查找指导老师Id，用于回答用户查找指导老师Id")
    public Integer findTeacher(@ToolParam(description = "用户名") String username,
                               @ToolParam(description = "昵称") String nickname) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username)
                .or()
                .eq(User::getNickname, nickname);
        User user = userMapper.selectOne(queryWrapper);
        return user != null ? user.getId() : null;
    }

    /**
     * 查找指导老师列表
     *
     * @return
     */
    @Tool(description = "获取当前系统中的全部指导老师，用于回答用户询问系统有哪些指导老师的问题")
    public List<User> findTeacherList() {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getRole, UserRoleEnum.TEACHER.getRole());
        List<User> userList = userMapper.selectList(queryWrapper);
//        log.info("查询到的指导老师列表: {}", userList);
        return userList;
    }

    /**
     * 查找指导老师列表
     *
     * @return
     */
    @Tool(description = "获取当前系统中的全部学生，用于回答用户询问系统有哪些学生的问题")
    public List<User> findStudentList() {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getRole, UserRoleEnum.USER.getRole());
        List<User> userList = userMapper.selectList(queryWrapper);
//        log.info("查询到的学生列表: {}", userList);
        return userList;
    }
}

