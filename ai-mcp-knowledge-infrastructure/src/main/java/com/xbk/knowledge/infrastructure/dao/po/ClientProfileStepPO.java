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

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long clientProfileId;

    private Integer sequenceNo;

    private String stepName;

    private Long modelId;

    private String systemPrompt;

    private Boolean enableTools;

    private String allowedToolKeysJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
