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

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * Agent ID。
     */
    private Long agentId;

    /**
     * 版本号。
     */
    private Integer versionNo;

    /**
     * DRAFT/PUBLISHED/ARCHIVED。
     */
    private String state;

    /**
     * 变更摘要。
     */
    private String changeSummary;

    /**
     * Prompt 模板 ID。
     */
    private Long promptTemplateId;

    /**
     * Prompt 模板版本号。
     */
    private Integer promptTemplateVersionNo;

    /**
     * 模板参数 JSON。
     */
    private String templateParamsJson;

    /**
     * 系统提示词快照。
     */
    private String systemPromptSnapshot;

    /**
     * 绑定的 WorkflowVersion ID（可选）。
     *
     * 说明：
     * - 非空时，AgentRuntime 转发到 WorkflowRuntime 执行
     * - 允许与 prompt_template 并存，但以 workflowVersionId 为优先
     */
    private Long workflowVersionId;

    /**
     * 输出协议版本。
     */
    private String outputContractVersion;

    /**
     * 输出协议选项 JSON。
     */
    private String outputContractOptionsJson;

    /**
     * RAG 模式。
     */
    private String ragMode;

    /**
     * 默认 RAG 标签 JSON。
     */
    private String defaultRagTagsJson;

    /**
     * 允许的 RAG 标签 JSON。
     */
    private String allowedRagTagsJson;

    /**
     * 允许的工具键 JSON。
     */
    private String allowedToolKeysJson;

    /**
     * Client Profile ID（优先于 clientChainJson）。
     */
    private Long clientProfileId;

    /**
     * Client 串联配置（JSON 数组）。
     *
     * 说明：
     * - 非空时，按 sequence 顺序执行多个“客户端步骤”
     * - 用于对齐 ai-agent-station 的 Agent->Client 组装与串联形态
     */
    private String clientChainJson;

    /**
     * Planning 配置（JSON 对象）。
     *
     * 说明：
     * - enabled=true 时，运行时先自动生成执行计划
     * - 可配置是否要求人工确认后再执行
     */
    private String planningConfigJson;

    /**
     * 超时时间（毫秒）。
     */
    private Integer timeoutMs;

    /**
     * 最大轮次。
     */
    private Integer maxTurns;

    /**
     * 温度参数。
     */
    private BigDecimal temperature;

    /**
     * 修复重试次数。
     */
    private Integer repairRetryTimes;

    /**
     * 创建人 ID。
     */
    private Long createdBy;

    /**
     * 更新人 ID。
     */
    private Long updatedBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
