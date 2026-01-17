package com.xbk.knowledge.test;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * MCP 工具调用测试类
 * 演示使用不同大模型进行 Function Calling
 *
 * @author xiexu
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MCPTest {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private ToolCallbackProvider tools;

    /**
     * Gemini 模型（通过 Google GenAI SDK 调用）
     */
    @Autowired
    private GoogleGenAiChatModel geminiChatModel;

    /**
     * 测试 Gemini 模型的工具调用能力
     * 使用 Google AI Gemini 2.0 Flash 模型
     */
    @Test
    public void test_gemini_tool() {
        String userInput = "有哪些工具可以使用";

        // 使用 Gemini 模型创建 ChatClient
        var chatClient = ChatClient.builder(geminiChatModel)
                .defaultToolCallbacks(tools)
                .build();

        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
    }

    @Test
    public void test_tool() {
        String userInput = "有哪些工具可以使用";
        var chatClient = chatClientBuilder
                .defaultToolCallbacks(tools)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-ai/DeepSeek-R1-0528-Qwen3-8B")
                        .build())
                .build();

        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
    }

    @Test
    public void test() {
        String userInput = "获取电脑配置";
//        userInput = "在 /Users/xiexu/Desktop 文件夹下，创建 电脑.txt";
        userInput = "获取电脑配置 在 /Users/xiexu/Desktop 文件夹下，创建 电脑.txt 把电脑配置写入 电脑.txt";

        var chatClient = ChatClient.builder(geminiChatModel)
                .defaultToolCallbacks(tools)
                .build();

        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
    }

}
