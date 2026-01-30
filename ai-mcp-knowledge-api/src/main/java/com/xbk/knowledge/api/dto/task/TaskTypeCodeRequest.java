package com.xbk.knowledge.api.dto.task;

import com.xbk.knowledge.types.common.BaseRequest;
import com.xbk.knowledge.types.enums.TaskTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 任务类型代码查询请求
 * 用于根据任务代码查询任务类型
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskTypeCodeRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 任务类型代码（必填）
     */
    @NotBlank(message = "任务类型代码不能为空")
    @Pattern(regexp = TaskTypeEnum.TASK_TYPE_REGEX, message = "任务类型代码不合法")
    private String code;
}
