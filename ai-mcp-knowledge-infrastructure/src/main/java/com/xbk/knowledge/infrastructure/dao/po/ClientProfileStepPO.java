package com.xbk.knowledge.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Client Profile 步骤持久化对象。
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_client_profile_step")
public class ClientProfileStepPO {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 客户画像 ID。
     */
    private Long clientProfileId;

    /**
     * 步骤序号。
     */
    private Integer sequenceNo;

    /**
     * 步骤名称。
     */
    private String stepName;

    /**
     * 模型 ID。
     */
    private Long modelId;

    /**
     * 系统提示词。
     */
    private String systemPrompt;

    /**
     * 是否启用工具。
     */
    private Boolean enableTools;

    /**
     * 允许工具键 JSON。
     */
    private String allowedToolKeysJson;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
