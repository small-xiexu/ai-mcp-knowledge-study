package com.xbk.knowledge.api.dto.metrics;

import com.xbk.knowledge.types.common.BaseRequest;
import com.xbk.knowledge.types.enums.TaskTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * 监控统计查询请求
 * 用于查询 AI 调用的监控统计数据
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MetricsQueryRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 模型 ID（可选）
     * 不指定则查询所有模型
     */
    private Long modelId;

    /**
     * 任务类型编码（可选）
     * 取值来自任务类型配置表 ai_task_type.task_code，可通过 /api/task-types/list 查询
     * 不指定则查询所有任务类型
     *
     * @see com.xbk.knowledge.types.enums.TaskTypeEnum
     */
    @Pattern(regexp = TaskTypeEnum.TASK_TYPE_REGEX, message = "任务类型编码不合法")
    private String taskType;

    /**
     * 开始时间（必填）
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间（必填）
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
