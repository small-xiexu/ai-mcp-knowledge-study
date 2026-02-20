package com.xbk.knowledge.api.dto.advisor;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * Advisor 保存请求（新增/更新）。
 *
 * @author sxie
 */
@Data
public class AdvisorSaveRequest {

    private Long id;

    @NotBlank(message = "advisorCode 不能为空")
    private String advisorCode;

    @NotBlank(message = "advisorName 不能为空")
    private String advisorName;

    @NotBlank(message = "advisorType 不能为空")
    private String advisorType;

    private Boolean enabled;

    /**
     * JSON 字符串（可空）。
     */
    private String configJson;
}

