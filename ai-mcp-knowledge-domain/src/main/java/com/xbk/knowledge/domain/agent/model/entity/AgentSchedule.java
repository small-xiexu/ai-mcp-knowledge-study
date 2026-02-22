package com.xbk.knowledge.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 调度配置。
 *
 * 职责：表达调度配置的领域对象，用于与 XXL-Job 联动管理。
 *
 * 说明：调度执行时不绑定固定版本，运行时必须取 Agent.current_published_version_id。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long agentId;
    /**
     * Agent 对外编码（查询/展示字段，不一定落库）。
     */
    private String agentCode;

    /**
     * 调度名称（同一个 Agent 下唯一）。
     */
    private String scheduleName;

    /**
     * 调度描述（可选）。
     */
    private String description;

    /**
     * Cron 表达式（由 XXL-Job 解释）。
     */
    private String cron;

    /**
     * 是否启用（与 xxl-job triggerStatus 关联）。
     */
    private Boolean enabled;

    /**
     * 关联的 XXL 任务 ID。
     */
    private Long xxlJobId;

    /**
     * 调度入参模板（JSON）。
     *
     * 约定（P2 最小集）：
     * - content: string（必填）
     * - ragTagsJson: string（可选，JSON 数组字符串）
     */
    private String payloadTemplateJson;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
