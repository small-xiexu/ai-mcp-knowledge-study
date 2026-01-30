package com.xbk.knowledge.api.dto.ai;

import com.xbk.knowledge.types.common.BaseRequest;
import com.xbk.knowledge.types.enums.TaskTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Pattern;
/**
 * 推荐模型查询请求
 * 根据任务类型返回推荐模型
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelRecommendRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 任务类型编码（可选）
     * 取值来自任务类型配置表 ai_task_type.task_code，可通过 /api/task-types/list 查询
     *
     * @see com.xbk.knowledge.types.enums.TaskTypeEnum
     */
    @Pattern(regexp = TaskTypeEnum.TASK_TYPE_REGEX, message = "任务类型编码不合法")
    private String taskType;
}
