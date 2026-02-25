package com.xbk.knowledge.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AgentSchedule 持久化对象。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSchedulePO implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * Agent ID。
     */
    private Long agentId;

    /**
     * Agent 编码。
     */
    private String agentCode;

    /**
     * 调度名称。
     */
    private String scheduleName;

    /**
     * 调度描述。
     */
    private String description;

    /**
     * Cron 表达式。
     */
    private String cron;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * XXL 任务 ID。
     */
    private Long xxlJobId;

    /**
     * 调度负载模板 JSON。
     */
    private String payloadTemplateJson;

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
