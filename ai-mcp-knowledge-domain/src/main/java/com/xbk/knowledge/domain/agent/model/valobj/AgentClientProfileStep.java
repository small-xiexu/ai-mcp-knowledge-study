package com.xbk.knowledge.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Agent 客户端串联步骤配置。
 *
 * 用途：
 * - 对齐 ai-agent-station 的“Agent -> Client 顺序执行”使用形态
 * - 作为 AgentVersion.clientChainJson 的结构化对象
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentClientProfileStep {

    /**
     * 执行顺序（越小越先执行）。
     */
    private Integer sequence;

    /**
     * 步骤名称（展示用）。
     */
    private String stepName;

    /**
     * 本步骤使用的模型 ID。
     */
    private Long modelId;

    /**
     * 本步骤系统提示词（为空时回退到版本快照）。
     */
    private String systemPrompt;

    /**
     * 是否启用工具（默认 true）。
     */
    private Boolean enableTools;

    /**
     * 本步骤允许的工具 key 集合（为空时回退到版本级 allowedToolKeysJson）。
     */
    private List<String> allowedToolKeys;
}
