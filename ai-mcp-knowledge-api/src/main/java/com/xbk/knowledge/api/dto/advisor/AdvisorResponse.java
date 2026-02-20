package com.xbk.knowledge.api.dto.advisor;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Advisor 响应。
 *
 * @author sxie
 */
@Data
public class AdvisorResponse {

    private Long id;
    private String advisorCode;
    private String advisorName;
    private String advisorType;
    private Integer enabled;
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
