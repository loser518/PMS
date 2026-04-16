package com.project.pms.ai;

import com.project.pms.entity.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 辅助写作控制器
 * 使用 AI 模型辅助学生填写课题申报信息
 * 支持用户自定义 API Key（通过 X-API-Key 请求头传入）
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AiApiCaller aiApiCaller;

    /**
     * 获取用户自定义 API Key（前端 localStorage 传入）
     * 若无，则使用系统默认 Key
     */
    private String getUserApiKey(HttpServletRequest request) {
        String header = request.getHeader("X-API-Key");
        return (header != null && !header.isBlank()) ? header.trim() : null;
    }

    /**
     * AI 生成课题描述
     */
    @PostMapping("/generate/description")
    public Result generateDescription(
            @RequestParam String title,
            @RequestParam(required = false) String type,
            HttpServletRequest request) {
        String userKey = getUserApiKey(request);
        String prompt = buildDescriptionPrompt(title, type);
        String result = aiApiCaller.syncCall(userKey, prompt);
        return Result.success(result, "生成成功");
    }

    /**
     * AI 润色课题描述
     */
    @PostMapping("/polish")
    public Result polish(@RequestParam String content, HttpServletRequest request) {
        String userKey = getUserApiKey(request);
        String prompt = "请对以下学术课题描述进行专业润色，保持原意不变，使语言更加严谨、学术化：\n\n" + content;
        String result = aiApiCaller.syncCall(userKey, prompt);
        return Result.success(result, "润色成功");
    }

    /**
     * AI 扩展课题描述
     */
    @PostMapping("/expand")
    public Result expand(@RequestParam String keywords, HttpServletRequest request) {
        String userKey = getUserApiKey(request);
        String prompt = "基于以下研究关键词，为本科生课题申报撰写一段100-200字的课题研究内容描述。\n\n关键词：" + keywords;
        String result = aiApiCaller.syncCall(userKey, prompt);
        return Result.success(result, "生成成功");
    }

    /**
     * 构建描述 Prompt
     */
    private String buildDescriptionPrompt(String title, String type) {
        return """
                请为以下本科毕业设计生成一段100-150字的研究描述。

                要求：
                1. 语言学术化
                2. 包含研究背景
                3. 描述研究目标
                4. 说明预期成果
                5. 只输出正文

                课题名称：%s
                课题类型：%s
                """.formatted(title, type == null ? "未指定" : type);
    }

    /**
     * 验证 API Key 是否有效
     */
    @PostMapping("/test-key")
    public Result testKey(@RequestBody Map<String, String> body) {
        String apiKey = body.get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            return Result.error("API Key 不能为空");
        }
        boolean valid = aiApiCaller.validateKey(apiKey.trim());
        if (valid) {
            return Result.success(true, "API Key 有效");
        } else {
            return Result.error("API Key 无效或已过期");
        }
    }
}