package com.project.pms.ai;

import com.project.pms.entity.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 辅助写作控制器
 * 使用 AI 模型辅助学生填写课题申报信息
 * 当一个接口有多个实现类，或者当同一个类型有多个 Bean 实例时，
 * Spring 就不知道应该注入哪一个，这时候就需要用 @Qualifier 来指定具体的 Bean。
 * @author loser
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AIController {

    /**
     * Spring AI ChatClient
     */
    private final ChatClient chatClient;

    public AIController(@Qualifier("chatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * AI 生成课题描述
     */
    @PostMapping("/generate/description")
    public Result generateDescription(
            @RequestParam String title,
            @RequestParam(required = false) String type) {

        String prompt = buildDescriptionPrompt(title, type);

        String result = callAI(prompt);

        return Result.success(result, "生成成功");
    }

    /**
     * AI 润色课题描述
     */
    @PostMapping("/polish")
    public Result polish(@RequestParam String content) {

        String prompt = "请对以下学术课题描述进行专业润色，保持原意不变，使语言更加严谨、学术化：\n\n" + content;

        String result = callAI(prompt);

        return Result.success(result, "润色成功");
    }

    /**
     * AI 扩展课题描述
     */
    @PostMapping("/expand")
    public Result expand(@RequestParam String keywords) {

        String prompt = "基于以下研究关键词，为本科生课题申报撰写一段100-200字的课题研究内容描述。\n\n关键词：" + keywords;

        String result = callAI(prompt);

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
     * 统一 AI 调用
     */
    private String callAI(String prompt) {

        try {

            return chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

        } catch (Exception e) {

            log.error("AI 调用失败", e);

            return "AI 服务暂时不可用，请稍后再试。";
        }
    }
}