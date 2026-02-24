package com.xbk.knowledge.application.service.armory.node;

import com.xbk.knowledge.application.service.armory.factory.AiClientArmoryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 工具节点，负责解析 MCP 工具回调能力。
 * @author sxie
 */
@Slf4j
@Component
public class AiClientToolNode extends AbstractAiClientArmoryNode {

    private final ObjectProvider<ToolCallbackProvider> toolCallbackProvider;

    public AiClientToolNode(ObjectProvider<ToolCallbackProvider> toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    /**
     * 执行节点处理逻辑。
     *
     * @param context 执行上下文。
     */
    @Override
    protected void doHandle(AiClientArmoryContext context) {
        if (!context.isRequestedEnableTools()) {
            context.setResolvedEnableTools(false);
            context.setToolCallbackProvider(null);
            return;
        }
        ToolCallbackProvider provider = toolCallbackProvider.getIfAvailable();
        if (provider == null) {
            context.setResolvedEnableTools(false);
            context.setToolCallbackProvider(null);
            log.warn("MCP 工具未注入：ToolCallbackProvider 不可用，降级为无工具调用");
            return;
        }
        context.setResolvedEnableTools(true);
        context.setToolCallbackProvider(provider);
        log.debug("MCP 工具注入成功：{}", provider.getClass().getName());
    }
}
