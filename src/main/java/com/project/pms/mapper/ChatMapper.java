package com.project.pms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.pms.entity.po.Chat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMapper extends BaseMapper<Chat> {
}
