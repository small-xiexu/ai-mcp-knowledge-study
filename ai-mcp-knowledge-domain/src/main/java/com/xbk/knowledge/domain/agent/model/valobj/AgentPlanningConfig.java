package com.xbk.knowledge.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Agent Planning 配置。
 *
 * 职责：描述“自动规划 + 人工确认执行”的运行策略。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPlanningConfig implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 是否启用 Planning 模式。
     */
    @Builder.Default
    private Boolean enabled = false;

    /**
     * 是否需要人工确认后执行。
     */
    @Builder.Default
    private Boolean requireHumanConfirm = true;

    /**
     * 规划阶段使用的模型ID（为空则自动选可用模型）。
     */
    private Long plannerModelId;

    /**
     * 最大规划步骤数。
     */
    @Builder.Default
    private Integer maxPlanSteps = 6;

    /**
     * 失败后最大重规划次数。
     */
    @Builder.Default
    private Integer replanMaxTimes = 1;

    /**
     * 单步骤超时时间（毫秒）。
     */
    @Builder.Default
    private Integer stepTimeoutMs = 60000;

    /**
     * 人工确认审批单有效期（分钟）。
     */
    @Builder.Default
    private Integer approvalExpireMinutes = 120;
}
