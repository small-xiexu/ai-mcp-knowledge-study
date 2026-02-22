package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * AgentSchedule 创建请求。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentScheduleCreateRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 目标 Agent（对外标识）。
     */
    @NotBlank(message = "agentCode 不能为空")
    private String agentCode;

    /**
     * 调度名称（同一个 Agent 下唯一）。
     */
    @NotBlank(message = "scheduleName 不能为空")
    private String scheduleName;

    /**
     * 调度描述（可选）。
     */
    private String description;

    /**
     * Cron 表达式。
     */
    @NotBlank(message = "cron 不能为空")
    private String cron;

    /**
     * 是否启用（缺省 true）。
     */
    private Boolean enabled;

    /**
     * 调度入参模板 JSON。
     */
    private String payloadTemplateJson;
}
