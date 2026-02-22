package com.xbk.knowledge.api.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Client Profile 响应。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfileResponse {

    private Long id;

    private String clientCode;

    private String clientName;

    private String description;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ClientProfileStepResponse> steps;

    /**
     * Client Profile 步骤响应。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientProfileStepResponse {

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
}
