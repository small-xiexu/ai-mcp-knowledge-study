package com.xbk.knowledge.api.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AgentVersion 响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersionResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 状态
     */
    private String state;

    /**
     * 变更说明
     */
    private String changeSummary;

    /**
     * 提示词模板ID
     */
    private Long promptTemplateId;

    /**
     * 提示词模板版本号
     */
    private Integer promptTemplateVersionNo;

    /**
     * 模板参数JSON
     */
    private String templateParamsJson;

    /**
     * 系统提示词快照
     */
    private String systemPromptSnapshot;

    /**
     * Workflow 版本ID
     */
    private Long workflowVersionId;

    /**
     * outputContract版本
     */
    private String outputContractVersion;

    /**
     * outputContractOptions JSON
     */
    private String outputContractOptionsJson;

    /**
     * RAGMode
     */
    private String ragMode;

    /**
     * 默认RAG标签JSON
     */
    private String defaultRagTagsJson;

    /**
     * 允许的RAG标签JSON
     */
    private String allowedRagTagsJson;

    /**
     * 允许的工具Key列表JSON
     */
    private String allowedToolKeysJson;

    /**
     * Client Profile ID
     */
    private Long clientProfileId;

    /**
     * ClientChain JSON
     */
    private String clientChainJson;

    /**
     * planningConfig JSON
     */
    private String planningConfigJson;

    /**
     * 超时时间（毫秒）
     */
    private Integer timeoutMs;

    /**
     * 最大Turns
     */
    private Integer maxTurns;

    /**
     * 温度
     */
    private Double temperature;

    /**
     * repair重试Times
     */
    private Integer repairRetryTimes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
