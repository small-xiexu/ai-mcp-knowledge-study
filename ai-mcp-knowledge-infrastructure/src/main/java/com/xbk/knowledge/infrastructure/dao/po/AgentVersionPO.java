package com.xbk.knowledge.infrastructure.dao.po;

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
 * AgentVersion 持久化对象。
 *
 * @author sxie
 */
@TableName("agent_version")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersionPO {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Agent ID。
     */
    private Long agentId;

    /**
     * 版本号。
     */
    private Integer versionNo;

    /**
     * 状态。
     */
    private String state;

    /**
     * 变更摘要。
     */
    private String changeSummary;

    /**
     * Prompt 模板 ID。
     */
    private Long promptTemplateId;

    /**
     * Prompt 模板版本号。
     */
    private Integer promptTemplateVersionNo;

    /**
     * 模板参数 JSON。
     */
    private String templateParamsJson;

    /**
     * 系统提示词快照。
     */
    private String systemPromptSnapshot;

    /**
     * Workflow 版本 ID。
     */
    private Long workflowVersionId;

    /**
     * 输出协议版本。
     */
    private String outputContractVersion;

    /**
     * 输出协议选项 JSON。
     */
    private String outputContractOptionsJson;

    /**
     * RAG 模式。
     */
    private String ragMode;

    /**
     * 默认 RAG 标签 JSON。
     */
    private String defaultRagTagsJson;

    /**
     * 允许的 RAG 标签 JSON。
     */
    private String allowedRagTagsJson;

    /**
     * 允许工具键 JSON。
     */
    private String allowedToolKeysJson;

    /**
     * 客户画像 ID。
     */
    private Long clientProfileId;

    /**
     * 客户端链路 JSON。
     */
    private String clientChainJson;

    /**
     * Planning 配置 JSON。
     */
    private String planningConfigJson;

    /**
     * 超时时间（毫秒）。
     */
    private Integer timeoutMs;

    /**
     * 最大轮次。
     */
    private Integer maxTurns;

    /**
     * 温度参数。
     */
    private BigDecimal temperature;

    /**
     * 修复重试次数。
     */
    private Integer repairRetryTimes;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
