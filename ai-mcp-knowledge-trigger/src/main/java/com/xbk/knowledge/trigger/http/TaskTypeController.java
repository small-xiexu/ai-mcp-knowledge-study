package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
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
     * 为什么：任务类型可能扩展，分页避免一次性加载过多数据
     * 入参：分页查询请求
     * 出参：分页结果
     */
    @PostMapping("/list")
    @SaCheckPermission("workflow:read")
    public Result<PageResult<TaskTypeResponse>> listTaskTypes(@Valid @RequestBody TaskTypeQueryRequest request) {
        /*
         * 目的：将分页参数转换为领域查询对象
         */
        int offset = request.getOffset();
        Integer pageSize = request.getPageSize();
        TaskTypePageQuery query = new TaskTypePageQuery(
                offset,
                pageSize
        );
        PageResult<TaskType> pageResult = taskTypeAppService.queryTaskTypePage(query);

        /*
         * 目的：统一分页转换逻辑，避免前端协议分散
         */
        PageResult<TaskTypeResponse> result = PageResultConverter.convert(pageResult, this::convertToResponse);

        return Result.success(result);
    }

    /**
     * 根据 ID 查询任务类型
     *
     * 为什么：前端详情页需要单条记录
     * 入参：ID 查询请求
     * 出参：任务类型详情
     */
    @PostMapping("/get")
    @SaCheckPermission("workflow:read")
    public Result<TaskTypeResponse> getTaskType(@Valid @RequestBody IdRequest request) {
        /*
         * 目的：通过应用层读取任务类型
         */
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        TaskType taskType = taskTypeAppService.queryTaskTypeById(idQuery);

        /*
         * 目的：输出层只暴露必要字段
         */
        TaskTypeResponse response = convertToResponse(taskType);
        return Result.success(response);
    }

    /**
     * 根据编码查询任务类型
     *
     * 为什么：任务执行时通常使用编码定位类型
     * 入参：任务类型代码请求
     * 出参：任务类型详情
     */
    @PostMapping("/get-by-code")
    @SaCheckPermission("workflow:read")
    public Result<TaskTypeResponse> getTaskTypeByCode(@Valid @RequestBody TaskTypeCodeRequest request) {
        /*
         * 目的：按编码查询更适合内部路由
         */
        String code = request.getCode();
        TaskTypeCodeQuery taskTypeCodeQuery = new TaskTypeCodeQuery(code);
        TaskType taskType = taskTypeAppService.queryTaskTypeByCode(taskTypeCodeQuery);

        /*
         * 目的：输出层只暴露必要字段
         */
        TaskTypeResponse response = convertToResponse(taskType);
        return Result.success(response);
    }

    /**
     * 创建任务类型
     *
     * 为什么：统一由应用层校验并持久化
     * 入参：任务类型请求
     * 出参：创建的任务类型
     */
    @PostMapping("/create")
    @SaCheckPermission("workflow:write")
    public Result<TaskTypeResponse> createTaskType(@Valid @RequestBody TaskTypeRequest request) {
        /*
         * 目的：构建领域对象，隔离接口层 DTO
         */
        TaskType taskType = buildTaskTypeFromRequest(request);

        /*
         * 目的：交由应用层执行创建逻辑
         */
        TaskType savedTaskType = taskTypeAppService.createTaskType(taskType);

        /*
         * 目的：输出层只返回必要字段
         */
        TaskTypeResponse response = convertToResponse(savedTaskType);
        return Result.success("任务类型创建成功", response);
    }

    /**
     * 更新任务类型
     *
     * 为什么：保持任务类型变更入口统一，便于审计
     * 入参：任务类型请求（包含 ID）
     * 出参：更新后的任务类型
     */
    @PostMapping("/update")
    @SaCheckPermission("workflow:write")
    public Result<TaskTypeResponse> updateTaskType(@Valid @RequestBody TaskTypeRequest request) {
        /*
         * 目的：构建完整领域对象，保证字段映射一致
         */
        TaskType taskType = buildTaskTypeFromRequest(request);
        Long id = request.getId();
        taskType.setId(id);

        /*
         * 目的：交由应用层处理更新逻辑
         */
        TaskType updatedTaskType = taskTypeAppService.updateTaskType(taskType);

        /*
         * 目的：输出层只返回必要字段
         */
        TaskTypeResponse response = convertToResponse(updatedTaskType);
        return Result.success("任务类型更新成功", response);
    }

    /**
     * 删除任务类型
     *
     * 为什么：清理不再使用的类型，避免误选
     * 入参：ID 查询请求
     * 出参：删除结果
     */
    @PostMapping("/delete")
    @SaCheckPermission("workflow:write")
    public Result<Void> deleteTaskType(@Valid @RequestBody IdRequest request) {
        /*
         * 目的：交由应用层完成删除与校验
         */
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        taskTypeAppService.deleteTaskType(idQuery);

        return Result.success();
    }

    /**
     * 转换为响应 DTO
     *
     * 为什么：输出层补充首选模型名称，提升前端展示体验
     * 入参：任务类型实体
     * 出参：响应 DTO
     */
    private TaskTypeResponse convertToResponse(TaskType taskType) {
        /*
         * 目的：补充模型名称以便前端直接展示
         * 约束：模型不存在时降级为 null，不阻断主流程
         */
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
     * 为什么：保持领域对象构建逻辑集中，便于统一校验
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
