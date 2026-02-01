package com.xbk.knowledge.api.dto.ai;

import com.xbk.knowledge.types.enums.ModelSelectionStrategy;
import com.xbk.knowledge.types.enums.TaskTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

/**
 * AI 请求对象
 * 统一的 AI 模型调用请求参数
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {

    /**
     * 请求内容
     */
    private String content;

    /**
     * 任务类型编码（可选）
     * 取值来自任务类型配置表 ai_task_type.task_code，可通过 /api/task-types/list 查询
     * 如果指定，将根据任务类型自动选择模型
     *
     * @see com.xbk.knowledge.types.enums.TaskTypeEnum
     */
    @Pattern(regexp = TaskTypeEnum.TASK_TYPE_REGEX, message = "任务类型编码不合法")
    private String taskType;

    /**
     * 系统提示词（可选）
     */
    private String systemPrompt;

    /**
     * 模型参数（可选）
     * 例如：temperature、maxTokens 等
     */
    private Map<String, Object> parameters;

    /**
     * 模型选择策略（可选）
     * 如果不指定，将使用默认策略或根据任务类型选择
     */
    private ModelSelectionStrategy strategy;

    /**
     * 是否启用流式输出（可选）
     */
    private Boolean streaming;

    /**
     * 指定模型ID（可选）
     * 如果指定，将优先使用该模型
     */
    private Long modelId;

    /**
     * 知识库标签列表（可选）
     * 用于 RAG 检索过滤
     */
    private List<String> ragTags;
}
