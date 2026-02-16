package com.xbk.knowledge.api.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AgentVersion 响应 DTO。
 
  * @author xiexu
  */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersionResponse {

    private Long id;

    private Long orgId;

    private Long agentId;

    private Integer versionNo;

    private String state;

    private String changeSummary;

    private Long promptTemplateId;

    private Integer promptTemplateVersionNo;

    private String templateParamsJson;

    private String systemPromptSnapshot;

    private Long workflowVersionId;

    private String outputContractVersion;

    private String outputContractOptionsJson;

    private String modelStrategyType;

    private String taskTypeCode;

    private Long fixedModelId;

    private String ragMode;

    private String defaultRagTagsJson;

    private String allowedRagTagsJson;

    private String toolPolicyMode;

    private String allowedToolKeysJson;

    private Integer timeoutMs;

    private Integer maxTurns;

    private Double temperature;

    private Integer repairRetryTimes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
