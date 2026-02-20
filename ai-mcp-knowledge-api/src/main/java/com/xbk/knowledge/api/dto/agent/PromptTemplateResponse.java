package com.xbk.knowledge.api.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PromptTemplate 响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateResponse {

    private Long id;

    private String templateCode;

    private String templateName;

    private Integer versionNo;

    private String state;

    private String content;

    private String variableSpecJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
