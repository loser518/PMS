package com.project.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.pms.entity.po.Topic;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.TopicVO;
import com.project.pms.exception.BusinessException;

import java.util.List;

/**
 * 选题市场服务接口
 */
public interface ITopicService extends IService<Topic> {

    /**
     * 教师发布课题
     */
    void publishTopic(Topic topic) throws BusinessException;

    /**
     * 更新课题（仅发布者可操作）
     */
    void updateTopic(Topic topic) throws BusinessException;

    /**
     * 删除课题（仅发布者可操作，未被选题时才允许删）
     */
    void removeTopic(Integer id) throws BusinessException;

    /**
     * 查询选题市场（分页，可过滤）
     *
     * @param pageNo    页码
     * @param pageSize  每页条数
     * @param keyword   关键词（搜课题名）
     * @param typeId      课题类型
     * @param onlyMine  仅查看我发布的（教师用）
     * @return 分页结果
     */
    PageResult<TopicVO> listTopics(int pageNo, int pageSize, String keyword, Integer typeId, boolean onlyMine);

    /**
     * 学生选题：选择某个课题，自动生成 ProjectInfo 申请
     *
     * @param topicId 课题ID
     */
    void selectTopic(Integer topicId) throws BusinessException;
}
