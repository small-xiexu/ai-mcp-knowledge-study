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

    /**
     * 主键ID
     */
    private Long id;

    /**
     * Client 编码
     */
    private String clientCode;

    /**
     * Client 名称
     */
    private String clientName;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * steps
     */
    private List<ClientProfileStepResponse> steps;

    /**
     * Client Profile 步骤响应。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientProfileStepResponse {

        /**
         * 主键ID
         */
        private Long id;

        /**
         * Client Profile ID
         */
        private Long clientProfileId;

        /**
         * 序号
         */
        private Integer sequenceNo;

        /**
         * step名称
         */
        private String stepName;

        /**
         * 模型ID
         */
        private Long modelId;

        /**
         * 系统提示词
         */
        private String systemPrompt;

        /**
         * 启用Tools
         */
        private Boolean enableTools;

        /**
         * 允许的工具Key列表JSON
         */
        private String allowedToolKeysJson;

        /**
         * 创建时间
         */
        private LocalDateTime createdAt;

        /**
         * 更新时间
         */
        private LocalDateTime updatedAt;
    }
}
