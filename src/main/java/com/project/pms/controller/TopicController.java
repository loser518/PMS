package com.project.pms.controller;

import com.project.pms.entity.po.Topic;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.entity.vo.TopicVO;
import com.project.pms.exception.BusinessException;
import com.project.pms.service.ITopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 选题市场控制器
 *
 * @author loser
 * @since 2026-03-14
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/topic")
public class TopicController {

    private final ITopicService topicService;

    /**
     * 查询选题市场（学生/所有人可见）
     */
    @GetMapping
    public PageResult<TopicVO> listTopics(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer typeId,
            @RequestParam(defaultValue = "false") boolean onlyMine) {
        return topicService.listTopics(pageNo, pageSize, keyword, typeId, onlyMine);
    }

    /**
     * 教师发布课题
     */
    @PostMapping
    public Result publishTopic(@RequestBody Topic topic) throws BusinessException {
        topicService.publishTopic(topic);
        return Result.success("课题发布成功");
    }

    /**
     * 更新课题
     */
    @PostMapping("/update")
    public Result updateTopic(@RequestBody Topic topic) throws BusinessException {
        topicService.updateTopic(topic);
        return Result.success("课题更新成功");
    }

    /**
     * 删除课题
     */
    @DeleteMapping("/{id}")
    public Result removeTopic(@PathVariable Integer id) throws BusinessException {
        topicService.removeTopic(id);
        return Result.success("课题删除成功");
    }

    /**
     * 学生选题（自动生成课题申请）
     */
    @PostMapping("/select/{id}")
    public Result selectTopic(@PathVariable Integer id) throws BusinessException {
        topicService.selectTopic(id);
        return Result.success("选题成功，课题申请已自动提交");
    }
}
