package com.xbk.knowledge.domain.client.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Client Profile 步骤实体。
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfileStep {

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
