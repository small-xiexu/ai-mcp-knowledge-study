package com.xbk.knowledge.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * PromptTemplate 实体（模板资产）。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {
    private Long id;

    private String templateCode;

    private String templateName;

    /**
     * 版本号（建议：draft=0，published>=1）。
     */
    private Integer versionNo;

    /**
     * DRAFT/PUBLISHED/ARCHIVED。
     */
    private String state;

    private String content;

    private String variableSpecJson;

    private Long createdBy;

    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
