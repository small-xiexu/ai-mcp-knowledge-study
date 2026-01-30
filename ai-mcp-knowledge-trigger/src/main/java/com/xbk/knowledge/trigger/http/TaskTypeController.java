package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.task.TaskTypeCodeRequest;
import com.xbk.knowledge.api.dto.task.TaskTypeQueryRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.task.TaskTypeRequest;
import com.xbk.knowledge.api.dto.task.TaskTypeResponse;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.IdQuery;
import com.xbk.knowledge.domain.model.vo.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.TaskTypePageQuery;
import com.xbk.knowledge.application.service.ModelConfigAppService;
import com.xbk.knowledge.application.service.TaskTypeAppService;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务类型管理 Controller
 * 负责接收 HTTP 请求，调用应用服务，转换响应
 *
 * 职责：HTTP 接口适配，用于转发应用层能力
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/task-types")
@RequiredArgsConstructor
public class TaskTypeController {

    private final TaskTypeAppService taskTypeAppService;
    private final ModelConfigAppService modelConfigAppService;

    /**
     * 查询所有任务类型（分页）
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @PostMapping("/list")
    public Result<PageResult<TaskTypeResponse>> listTaskTypes(@Valid @RequestBody TaskTypeQueryRequest request) {
        // 调用应用服务查询
        TaskTypePageQuery query = new TaskTypePageQuery(
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<TaskType> pageResult = taskTypeAppService.queryTaskTypePage(query);

        // 转换为响应 DTO
        List<TaskTypeResponse> records = pageResult.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<TaskTypeResponse> result = PageResult.of(
                records,
                pageResult.getTotal(),
                pageResult.getPageNum(),
                pageResult.getPageSize()
        );

        return Result.success(result);
    }

    /**
     * 根据 ID 查询任务类型
     *
     * @param request ID 查询请求
     * @return 任务类型
     */
    @PostMapping("/get")
    public Result<TaskTypeResponse> getTaskType(@Valid @RequestBody IdRequest request) {
        // 调用应用服务查询
        TaskType taskType = taskTypeAppService.queryTaskTypeById(new IdQuery(request.getId()));

        // 转换为响应 DTO
        return Result.success(convertToResponse(taskType));
    }

    /**
     * 根据编码查询任务类型
     *
     * @param request 任务类型代码请求
     * @return 任务类型
     */
    @PostMapping("/get-by-code")
    public Result<TaskTypeResponse> getTaskTypeByCode(@Valid @RequestBody TaskTypeCodeRequest request) {
        // 调用应用服务查询
        TaskType taskType = taskTypeAppService.queryTaskTypeByCode(new TaskTypeCodeQuery(request.getCode()));

        // 转换为响应 DTO
        return Result.success(convertToResponse(taskType));
    }

    /**
     * 创建任务类型
     *
     * @param request 任务类型请求
     * @return 创建的任务类型
     */
    @PostMapping("/create")
    public Result<TaskTypeResponse> createTaskType(@Valid @RequestBody TaskTypeRequest request) {
        // 构建领域实体
        TaskType taskType = buildTaskTypeFromRequest(request);

        // 调用应用服务创建
        TaskType savedTaskType = taskTypeAppService.createTaskType(taskType);

        // 转换为响应 DTO
        return Result.success("任务类型创建成功", convertToResponse(savedTaskType));
    }

    /**
     * 更新任务类型
     *
     * @param request 任务类型请求（包含 ID）
     * @return 更新后的任务类型
     */
    @PostMapping("/update")
    public Result<TaskTypeResponse> updateTaskType(@Valid @RequestBody TaskTypeRequest request) {
        // 构建领域实体
        TaskType taskType = buildTaskTypeFromRequest(request);
        taskType.setId(request.getId());

        // 调用应用服务更新
        TaskType updatedTaskType = taskTypeAppService.updateTaskType(taskType);

        // 转换为响应 DTO
        return Result.success("任务类型更新成功", convertToResponse(updatedTaskType));
    }

    /**
     * 删除任务类型
     *
     * @param request ID 查询请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public Result<Void> deleteTaskType(@Valid @RequestBody IdRequest request) {
        // 调用应用服务删除
        taskTypeAppService.deleteTaskType(new IdQuery(request.getId()));

        return Result.success();
    }

    /**
     * 转换为响应 DTO
     *
     * @param taskType 任务类型实体
     * @return 响应 DTO
     */
    private TaskTypeResponse convertToResponse(TaskType taskType) {
        // 查询首选模型名称
        String preferredModelName = null;
        if (taskType.getPreferredModelId() != null) {
            try {
                ModelConfig modelConfig = modelConfigAppService.queryModelConfigById(new IdQuery(taskType.getPreferredModelId()));
                preferredModelName = modelConfig.getModelName();
            } catch (NotFoundException e) {
                log.warn("任务类型首选模型不存在，modelId: {}", taskType.getPreferredModelId(), e);
            }
        }

        return TaskTypeResponse.builder()
                .id(taskType.getId())
                .taskName(taskType.getTaskName())
                .taskCode(taskType.getTaskCode())
                .description(taskType.getDescription())
                .preferredModelId(taskType.getPreferredModelId())
                .preferredModelName(preferredModelName)
                .fallbackModelIds(taskType.getFallbackModelIds())
                .createdAt(taskType.getCreatedAt())
                .updatedAt(taskType.getUpdatedAt())
                .build();
    }

    /**
     * 从请求 DTO 构建领域实体
     *
     * @param request 请求 DTO
     * @return 领域实体
     */
    private TaskType buildTaskTypeFromRequest(TaskTypeRequest request) {
        return TaskType.builder()
                .taskName(request.getTaskName())
                .taskCode(request.getTaskCode())
                .description(request.getDescription())
                .preferredModelId(request.getPreferredModelId())
                .fallbackModelIds(request.getFallbackModelIds())
                .build();
    }
}
