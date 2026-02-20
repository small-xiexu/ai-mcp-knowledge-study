package com.xbk.knowledge.application.service.mcp;

import com.xbk.knowledge.application.model.dto.McpToolInfo;

import java.util.List;

/**
 * MCP 工具目录服务
 * 提供工具清单文本，用于注入到模型上下文
 *
 * @author sxie
 */
public interface McpToolCatalogService {

    /**
     * 构建工具清单提示词
     *
     * @return 工具清单提示词（无工具则返回空字符串）
     */
    String buildToolPrompt();

    /**
     * 获取工具列表
     *
     * @return 工具列表
     */
    List<McpToolInfo> listTools();
}
