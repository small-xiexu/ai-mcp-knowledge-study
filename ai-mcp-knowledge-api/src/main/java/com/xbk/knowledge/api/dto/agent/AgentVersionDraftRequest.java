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
 * AgentVersion 草稿创建/更新请求。
 *
 * 说明：本阶段优先保证“可发布/可回滚”的闭环，字段允许逐步扩展。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentVersionDraftRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 草稿版本ID（更新时必填，创建时不填）。
     */
    private Long id;

    @NotBlank(message = "agentCode 不能为空")
    private String agentCode;

    /**
     * 变更摘要（可选）。
     */
    private String changeSummary;

    /**
     * 模板ID（可选，允许先保存空草稿）。
     */
    private Long promptTemplateId;

    /**
     * 模板参数 JSON（对象）。
     */
    private String templateParamsJson;

    /**
     * 模型策略：TASK_TYPE_POLICY/FIXED_MODEL
     */
    @Builder.Default
    private String modelStrategyType = "TASK_TYPE_POLICY";

    private String taskTypeCode;

    private Long fixedModelId;

    /**
     * RAG 模式：DISABLED/OPTIONAL/REQUIRED
     */
    @Builder.Default
    private String ragMode = "OPTIONAL";

    private String defaultRagTagsJson;

    private String allowedRagTagsJson;

    /**
     * 允许工具集合 JSON（数组，元素为 toolKey）。
     */
    private String allowedToolKeysJson;

    @Builder.Default
    private String outputContractVersion = "v1";

    private String outputContractOptionsJson;

    private Integer timeoutMs;

    private Integer maxTurns;

    private Double temperature;

    private Integer repairRetryTimes;

    /**
     * 是否要求立即创建新 versionNo（创建草稿时）。
     * 若为 null，则由后端决定（默认递增）。
     */
    private Integer versionNo;
}

