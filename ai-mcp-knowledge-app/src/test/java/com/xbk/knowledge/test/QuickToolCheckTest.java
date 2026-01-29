package com.xbk.knowledge.test;

import com.xbk.knowledge.Application;
import lombok.extern.slf4j.Slf4j;
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
@SpringBootTest(classes = Application.class)
@ImportAutoConfiguration(exclude = {
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class
})
public class QuickToolCheckTest {

    @Autowired(required = false)
    private ToolCallbackProvider toolCallbackProvider;

    @Test
    public void checkToolsRegistered() {
        log.info("=== 快速检查工具注册状态 ===");

        assertNotNull(toolCallbackProvider, "❌ ToolCallbackProvider 未注入！");
        log.info("✅ ToolCallbackProvider 已注入");

        ToolCallback[] tools = toolCallbackProvider.getToolCallbacks();
        log.info(">>> 已注册工具数量: {}", tools.length);

        assertTrue(tools.length > 0, "❌ 没有注册任何工具！");

        for (int i = 0; i < tools.length; i++) {
            log.info(">>> 工具 {}: {}", i + 1, tools[i].getClass().getSimpleName());
        }

        log.info("✅ 工具注册正常，共 {} 个工具", tools.length);
    }
}
