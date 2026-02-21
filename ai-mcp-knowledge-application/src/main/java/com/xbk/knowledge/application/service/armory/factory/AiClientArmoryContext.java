package com.xbk.knowledge.application.service.armory.factory;

import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * AI 客户端装配上下文。
 *
 * 职责：在 armory 节点链路中传递强类型装配状态，避免 Map 结构带来的运行期错误。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiClientArmoryContext {

    /**
     * 本次装配使用的模型配置。
     */
    private ModelConfig modelConfig;

    /**
     * 调用方请求的工具开关。
     */
    private boolean requestedEnableTools;

    /**
     * 节点链路最终解析后的工具开关。
     */
    private boolean resolvedEnableTools;

    /**
     * 调用方传入的额外 Advisor 列表（可为空）。
     */
    private CallAdvisor[] extraAdvisors;

    /**
     * 框架内置 Advisor 与额外 Advisor 合并后的结果。
     */
    private List<CallAdvisor> mergedAdvisors;

    /**
     * 当前模型可用的工具回调提供器。
     */
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 基于模型配置构建出的 ChatModel。
     */
    private ChatModel chatModel;

    /**
     * 节点链路最终产出的 ChatClient。
     */
    private ChatClient chatClient;

    /**
     * 归一化 Advisor 数组，避免下游空指针与 null 元素。
     */
    public void normalizeExtraAdvisors() {
        if (extraAdvisors == null || extraAdvisors.length == 0) {
            extraAdvisors = new CallAdvisor[0];
            return;
        }
        extraAdvisors = Arrays.stream(extraAdvisors)
                .filter(Objects::nonNull)
                .toArray(CallAdvisor[]::new);
    }
}
