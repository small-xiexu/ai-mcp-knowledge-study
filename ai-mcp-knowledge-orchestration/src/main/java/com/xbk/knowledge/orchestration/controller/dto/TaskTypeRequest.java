package com.xbk.knowledge.orchestration.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 任务类型请求 DTO
 * 用于创建和更新任务类型
 *
 * @author xiexu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTypeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    /**
     * 任务编码（唯一）
     */
    @NotBlank(message = "任务编码不能为空")
    private String taskCode;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 首选模型ID
     */
    @NotNull(message = "首选模型ID不能为空")
    private Long preferredModelId;

    /**
     * 备用模型ID列表（逗号分隔）
     */
    private String fallbackModelIds;
}
