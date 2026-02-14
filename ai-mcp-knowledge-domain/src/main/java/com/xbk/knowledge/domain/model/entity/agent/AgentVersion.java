package com.xbk.knowledge.domain.model.entity.agent;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * 对应表：agent_version
 */
@TableName("agent_version")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersion {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long agentId;

    private Integer versionNo;

    /**
     * DRAFT/PUBLISHED/ARCHIVED。
     */
    private String state;

    private String changeSummary;

    private Long promptTemplateId;

    private Integer promptTemplateVersionNo;

    private String templateParamsJson;

    private String systemPromptSnapshot;

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

    private String toolRiskPolicyJson;

    private Integer timeoutMs;

    private Integer maxTurns;

    private BigDecimal temperature;

    private Integer repairRetryTimes;

    private Long createdBy;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

