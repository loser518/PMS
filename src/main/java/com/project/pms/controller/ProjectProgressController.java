package com.project.pms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.pms.config.MinioConfig;
import com.project.pms.entity.po.ProjectInfo;
import com.project.pms.entity.po.ProjectProgress;
import com.project.pms.entity.query.ProjectProgressQuery;
import com.project.pms.entity.vo.PageResult;
import com.project.pms.entity.vo.Result;
import com.project.pms.mapper.ProjectInfoMapper;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.IProjectProgressService;
import com.project.pms.utils.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @className: ProjectProgressController
 * @description: 课题进度管理表控制类
 * @author: loser
 * @createTime: 2026/2/9 16:14
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/projectProgress")
public class ProjectProgressController {
    private final IProjectProgressService projectProgressService;
    private final MinioUtil minioUtil;
    private final MinioConfig minioConfig;
    private final CurrentUser currentUser;
    private final ProjectInfoMapper projectInfoMapper;

    /**
     * 添加课题进度管理
     * 学生只能向自己的课题提交进度，防止伪造 pId 越权操作
     *
     * @param projectProgress 课题进度管理
     * @return 添加结果
     */
    @PostMapping
    public Result addProjectProgress(@RequestBody ProjectProgress projectProgress) {
        if (projectProgress.getPId() == null) {
            return Result.error("pId不能为空");
        }
        // 学生角色：校验 pId 对应的课题是否属于自己
        if (currentUser.isStudent()) {
            Integer userId = currentUser.getUserId();
            long count = projectInfoMapper.selectCount(
                    new LambdaQueryWrapper<ProjectInfo>()
                            .eq(ProjectInfo::getId, projectProgress.getPId())
                            .eq(ProjectInfo::getSid, userId)
            );
            if (count == 0) {
                return Result.error("无权向该课题提交进度");
            }
        }
        projectProgressService.save(projectProgress);
        return Result.success("添加课题进度管理成功！");
    }

    /**
     * 查询课题进度列表（后端真分页 + 学生数据隔离）
     * 若当前用户是学生（role=0），强制注入 sid 过滤，防止越权查看他人进度
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping
    public PageResult<ProjectProgress> queryProjectProgress(ProjectProgressQuery query) {
        // 学生只能查自己课题的进度，强制注入 sid 防止越权
        if (currentUser.isStudent()) {
            query.setSid(currentUser.getUserId());
        }
        return projectProgressService.selectProjectProgressList(query);
    }

    /**
     * 修改课题进度管理
     *
     * @param projectProgress 课题进度管理
     * @return 修改结果
     */
    @PostMapping("/update")
    public Result updateProjectProgress(@RequestBody ProjectProgress projectProgress) {
        projectProgressService.updateById(projectProgress);
        return Result.success("修改课题进度管理成功！");
    }

    /**
     * 删除课题进度管理
     *
     * @param ids 主键ID
     * @return 删除结果
     */
    @DeleteMapping
    public Result deleteProjectProgress(@RequestBody List<Integer> ids) {
        projectProgressService.removeByIds(ids);
        return Result.success("删除课题进度管理成功！");
    }

    /**
     * 上传进度附件
     *
     * @param file 附件文件
     * @return 包含文件访问URL和原始文件名的结果
     */
    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) {
        // 上传到 MinIO
        String objectName = minioUtil.uploadFile(file);
        // 获取文件的公开访问URL
        String fileUrl = minioUtil.getPublicUrl(minioConfig.getBucketName(), objectName);

        Map<String, String> data = new HashMap<>();
        data.put("objectName", objectName);
        data.put("fileName", file.getOriginalFilename());
        data.put("fileUrl", fileUrl);

        return Result.success(data, "文件上传成功");
    }

    /**
     * 下载进度附件
     * 使用 MinioUtil.getFile 方法获取文件流，返回给前端
     *
     * @param objectName MinIO存储的文件名
     * @param fileUrl 文件的公开URL
     * @param fileName 原始文件名
     * @return 文件流
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(@RequestParam(value = "objectName", required = false) String objectName,
                                                        @RequestParam(value = "fileUrl", required = false) String fileUrl,
                                                        @RequestParam(value = "fileName", required = false) String fileName) {
        try {
            // 解析 objectName：如果没传，尝试从 fileUrl 中提取
            if (objectName == null || objectName.isEmpty()) {
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    objectName = minioUtil.getFilename(fileUrl);
                } else {
                    return ResponseEntity.badRequest().build();
                }
            }

            // 使用 MinioUtil 获取文件流
            InputStream inputStream = minioUtil.getFile(objectName);

            // 获取文件信息，用于设置 Content-Type
            var fileInfo = minioUtil.getFileInfo(objectName);
            String contentType = fileInfo != null ? fileInfo.contentType() : "application/octet-stream";

            // 设置下载文件名
            String downloadFileName = fileName != null && !fileName.isEmpty() ? fileName : objectName;

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            // 使用 ISO-8859-1 编码避免中文文件名问题
            String encodedFileName = new String(downloadFileName.getBytes("UTF-8"), "ISO-8859-1");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("文件下载失败: objectName={}, fileUrl={}", objectName, fileUrl, e);
            return ResponseEntity.notFound().build();
        }
    }
}
