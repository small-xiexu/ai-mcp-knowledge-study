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

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * Client 编码
     */
    @NotBlank(message = "clientCode 不能为空")
    private String clientCode;

    /**
     * Client 名称
     */
    @NotBlank(message = "clientName 不能为空")
    private String clientName;

    /**
     * 描述
     */
    private String description;

    /**
     * ENABLED/DISABLED。
     */
    private String status;

    /**
     * steps
     */
    private List<ClientProfileStepItem> steps;

    /**
     * Client Profile 步骤项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientProfileStepItem {

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
    }
}
