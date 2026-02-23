package com.xbk.knowledge.api.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AgentSchedule 响应模型。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentScheduleResponse implements Serializable {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;
    /**
     * Agent ID
     */
    private Long agentId;
    /**
     * Agent 编码
     */
    private String agentCode;

    /**
     * 调度名称
     */
    private String scheduleName;
    /**
     * 描述
     */
    private String description;
    /**
     * CRON表达式
     */
    private String cron;
    /**
     * 启用状态
     */
    private Boolean enabled;
    /**
     * XXL任务ID
     */
    private Long xxlJobId;
    /**
     * 载荷模板JSON
     */
    private String payloadTemplateJson;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
