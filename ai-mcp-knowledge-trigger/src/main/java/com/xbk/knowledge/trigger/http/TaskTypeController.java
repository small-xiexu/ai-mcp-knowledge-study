package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.task.TaskTypeCodeRequest;
import com.xbk.knowledge.api.dto.task.TaskTypeQueryRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.task.TaskTypeRequest;
import com.xbk.knowledge.api.dto.task.TaskTypeResponse;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypePageQuery;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.application.service.app.TaskTypeAppService;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

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
        int offset = request.getOffset();
        Integer pageSize = request.getPageSize();
        TaskTypePageQuery query = new TaskTypePageQuery(
                offset,
                pageSize
        );
        PageResult<TaskType> pageResult = taskTypeAppService.queryTaskTypePage(query);

        PageResult<TaskTypeResponse> result = PageResultConverter.convert(pageResult, this::convertToResponse);

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
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        TaskType taskType = taskTypeAppService.queryTaskTypeById(idQuery);

        // 转换为响应 DTO
        TaskTypeResponse response = convertToResponse(taskType);
        return Result.success(response);
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
        String code = request.getCode();
        TaskTypeCodeQuery taskTypeCodeQuery = new TaskTypeCodeQuery(code);
        TaskType taskType = taskTypeAppService.queryTaskTypeByCode(taskTypeCodeQuery);

        // 转换为响应 DTO
        TaskTypeResponse response = convertToResponse(taskType);
        return Result.success(response);
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
        TaskTypeResponse response = convertToResponse(savedTaskType);
        return Result.success("任务类型创建成功", response);
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
        Long id = request.getId();
        taskType.setId(id);

        // 调用应用服务更新
        TaskType updatedTaskType = taskTypeAppService.updateTaskType(taskType);

        // 转换为响应 DTO
        TaskTypeResponse response = convertToResponse(updatedTaskType);
        return Result.success("任务类型更新成功", response);
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
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        taskTypeAppService.deleteTaskType(idQuery);

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
        Long preferredModelId = taskType.getPreferredModelId();
        if (preferredModelId != null) {
            try {
                IdQuery idQuery = new IdQuery(preferredModelId);
                ModelConfig modelConfig = modelConfigAppService.queryModelConfigById(idQuery);
                preferredModelName = modelConfig.getModelName();
            } catch (NotFoundException e) {
                log.warn("任务类型首选模型不存在，modelId: {}", preferredModelId, e);
            }
        }

        Long taskTypeId = taskType.getId();
        String taskName = taskType.getTaskName();
        String taskCode = taskType.getTaskCode();
        String description = taskType.getDescription();
        String fallbackModelIds = taskType.getFallbackModelIds();
        LocalDateTime createdAt = taskType.getCreatedAt();
        LocalDateTime updatedAt = taskType.getUpdatedAt();
        return TaskTypeResponse.builder()
                .id(taskTypeId)
                .taskName(taskName)
                .taskCode(taskCode)
                .description(description)
                .preferredModelId(preferredModelId)
                .preferredModelName(preferredModelName)
                .fallbackModelIds(fallbackModelIds)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    /**
     * 从请求 DTO 构建领域实体
     *
     * @param request 请求 DTO
     * @return 领域实体
     */
    private TaskType buildTaskTypeFromRequest(TaskTypeRequest request) {
        String taskName = request.getTaskName();
        String taskCode = request.getTaskCode();
        String description = request.getDescription();
        Long preferredModelId = request.getPreferredModelId();
        String fallbackModelIds = request.getFallbackModelIds();
        return TaskType.builder()
                .taskName(taskName)
                .taskCode(taskCode)
                .description(description)
                .preferredModelId(preferredModelId)
                .fallbackModelIds(fallbackModelIds)
                .build();
    }
}
