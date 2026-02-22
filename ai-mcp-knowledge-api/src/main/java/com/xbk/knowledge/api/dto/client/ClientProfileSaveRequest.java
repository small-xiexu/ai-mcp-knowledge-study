package com.xbk.knowledge.api.dto.client;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Client Profile 保存请求。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClientProfileSaveRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "clientCode 不能为空")
    private String clientCode;

    @NotBlank(message = "clientName 不能为空")
    private String clientName;

    private String description;

    /**
     * ENABLED/DISABLED。
     */
    private String status;

    private List<ClientProfileStepItem> steps;

    /**
     * Client Profile 步骤项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientProfileStepItem {

        private Integer sequenceNo;

        private String stepName;

        private Long modelId;

        private String systemPrompt;

        private Boolean enableTools;

        private String allowedToolKeysJson;
    }
}
