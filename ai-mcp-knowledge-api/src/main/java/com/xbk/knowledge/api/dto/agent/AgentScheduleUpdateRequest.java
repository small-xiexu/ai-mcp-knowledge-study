package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * AgentSchedule 更新参数。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentScheduleUpdateRequest extends BaseRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @NotNull(message = "id 不能为空")
    private Long id;

    /**
     * Agent 编码
     */
    @NotBlank(message = "agentCode 不能为空")
    private String agentCode;

    /**
     * 调度名称
     */
    @NotBlank(message = "scheduleName 不能为空")
    private String scheduleName;

    /**
     * 描述
     */
    private String description;

    /**
     * CRON表达式
     */
    @NotBlank(message = "cron 不能为空")
    private String cron;

    /**
     * 载荷模板JSON
     */
    private String payloadTemplateJson;
}
