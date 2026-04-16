package com.project.pms.controller;

import com.project.pms.entity.vo.Result;
import com.project.pms.security.CurrentUser;
import com.project.pms.service.IChatDetailedService;
import com.project.pms.utils.MinioUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天消息详情 HTTP 接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/msg/chat-detailed")
public class ChatDetailedController {

    private final IChatDetailedService chatDetailedService;
    private final CurrentUser currentUser;
    private final MinioUtil minioUtil;

    /**
     * 获取更多历史消息（分页，每次 20 条）
     * @param uid    聊天对象的UID
     * @param offset 偏移量（已加载了几条）
     */
    @GetMapping("/get-more")
    public Result getMoreChatDetails(@RequestParam Integer uid,
                                     @RequestParam(defaultValue = "0") Long offset) {
        Integer myId = currentUser.getUserId();
        return Result.success(chatDetailedService.getDetails(uid, myId, offset), "获取成功");
    }

    /**
     * 删除单条消息（仅对自己生效，对方仍可看到）
     * @param id 消息ID
     */
    @DeleteMapping("/delete")
    public Result delDetail(@RequestParam Integer id) {
        Integer myId = currentUser.getUserId();
        if (!chatDetailedService.deleteDetail(id, myId)) {
            return Result.error("删除失败，消息不存在或无权操作");
        }
        return Result.success("删除成功");
    }

    /**
     * 上传聊天图片/文件到 MinIO
     * 返回：{ url: "http://...", fileName: "原始文件名" }
     * @param file 上传的文件
     */
    @PostMapping("/upload")
    public Result uploadChatFile(@RequestParam("file") MultipartFile file) {
        String objectName = minioUtil.uploadFile(file);
        // 返回公开访问 URL（pms-bucket 桶已设为公开可读）
        String url = minioUtil.getPublicUrl(objectName);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("fileName", file.getOriginalFilename());
        return Result.success(data, "上传成功");
    }
}
