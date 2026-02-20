package com.xbk.knowledge.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AgentVersion 实体（草稿/发布/历史）。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersion {
    private Long id;

    private Long agentId;

    private Integer versionNo;

    /**
     * DRAFT/PUBLISHED/ARCHIVED。
     */
    private String state;

    private String changeSummary;

    private Long promptTemplateId;

    private Integer promptTemplateVersionNo;

    private String templateParamsJson;

    private String systemPromptSnapshot;

    /**
     * 绑定的 WorkflowVersion ID（可选）。
     *
     * 说明：
     * - 非空时，AgentRuntime 会转发到 WorkflowRuntime 执行
     * - 允许与 prompt_template 并存，但以 workflowVersionId 为优先
     */
    private Long workflowVersionId;

    private String outputContractVersion;

    private String outputContractOptionsJson;

    private String ragMode;

    private String defaultRagTagsJson;

    private String allowedRagTagsJson;

    private String allowedToolKeysJson;

    private Integer timeoutMs;

    private Integer maxTurns;

    private BigDecimal temperature;

    private Integer repairRetryTimes;

    private Long createdBy;

    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
