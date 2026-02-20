package com.xbk.knowledge.api.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AgentSchedule 响应模型。
 
  * @author xiexu
  */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentScheduleResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long agentId;
    private String agentCode;

    private String cron;
    private Boolean enabled;
    private Long xxlJobId;
    private String payloadTemplateJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
