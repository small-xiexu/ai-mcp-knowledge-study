package com.xbk.knowledge.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Agent 运行上下文快照实体。
 * 对应数据库表agent_run_context
 *
 * 职责：在审批“方式B”续跑场景中保存可恢复的运行输入快照。
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunContext {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 运行 ID。
     */
    private String runId;

    /**
     * SAVED/RESUMED/EXPIRED。
     */
    private String status;

    /**
     * 运行快照 JSON。
     */
    private String snapshotJson;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
