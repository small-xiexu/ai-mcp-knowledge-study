package com.xbk.knowledge.api.dto.task;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 任务类型请求 DTO
 * 用于创建和更新任务类型
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskTypeRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 任务类型 ID（更新时必填，创建时不填）
     */
    private Long id;

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
