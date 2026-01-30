package com.xbk.knowledge.test;

import com.xbk.knowledge.Application;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 快速检查工具注册状态
 */
@Slf4j
@Tag("integration")
@SpringBootTest(classes = Application.class)
@ImportAutoConfiguration(exclude = {
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class
})
public class QuickToolCheckTest {

    private final ToolCallbackProvider toolCallbackProvider;

    /**
     * 对外暴露 QuickToolCheckTest 作为调用入口，便于上层复用。
     */
    @Autowired
    public QuickToolCheckTest(ToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    /**
     * 对外暴露 checkToolsRegistered 作为调用入口，便于上层复用。
     */
    @Test
    public void checkToolsRegistered() {
        log.info("=== 快速检查工具注册状态 ===");

        assertNotNull(toolCallbackProvider, "❌ ToolCallbackProvider 未注入！");
        log.info("✅ ToolCallbackProvider 已注入");

        ToolCallback[] tools = toolCallbackProvider.getToolCallbacks();
        log.info(">>> 已注册工具数量: {}", tools.length);

        boolean hasTools = tools.length > 0;
        assertTrue(hasTools, "❌ 没有注册任何工具！");

        for (int i = 0; i < tools.length; i++) {
            int displayIndex = i + 1;
            ToolCallback tool = tools[i];
            String toolName = tool
                    .getClass()
                    .getSimpleName();
            log.info(">>> 工具 {}: {}", displayIndex, toolName);
        }

        log.info("✅ 工具注册正常，共 {} 个工具", tools.length);
    }
}
