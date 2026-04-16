package com.project.pms.service.impl;

import com.project.pms.entity.po.StudentProfile;
import com.project.pms.mapper.StudentProfileMapper;
import com.project.pms.service.IStudentProfileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 学生档案表 服务实现类
 * </p>
 *
 * @author loser
 * @since 2026-02-02
 */
@Service
public class StudentProfileServiceImpl extends ServiceImpl<StudentProfileMapper, StudentProfile> implements IStudentProfileService {

}
