package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.types.common.PageRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.TaskTypeRequest;
import com.xbk.knowledge.api.dto.TaskTypeResponse;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.repository.TaskTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务类型管理 Controller
 * 提供任务类型的增删改查接口
 *
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/task-types")
@RequiredArgsConstructor
public class TaskTypeController {

    private final TaskTypeRepository taskTypeRepository;
    private final ModelConfigRepository modelConfigRepository;

    /**
     * 查询所有任务类型（分页）
     *
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<TaskTypeResponse>> listTaskTypes(PageRequest pageRequest) {
        log.info("查询任务类型列表，pageNum: {}, pageSize: {}", pageRequest.getPageNum(), pageRequest.getPageSize());

        // 验证并修正分页参数
        pageRequest.validate();

        // 构建 Spring Data 分页请求
        org.springframework.data.domain.PageRequest springPageRequest = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPageNum() - 1,  // Spring Data 页码从 0 开始
                pageRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 查询分页数据
        Page<TaskType> page = taskTypeRepository.findAll(springPageRequest);

        // 转换为响应 DTO
        List<TaskTypeResponse> records = page.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<TaskTypeResponse> pageResult = PageResult.of(
                records,
                page.getTotalElements(),
                pageRequest.getPageNum(),
                pageRequest.getPageSize()
        );

        return Result.success(pageResult);
    }

    /**
     * 根据 ID 查询任务类型
     *
     * @param id 任务类型 ID
     * @return 任务类型
     */
    @GetMapping("/{id}")
    public Result<TaskTypeResponse> getTaskType(@PathVariable Long id) {
        log.info("查询任务类型，id: {}", id);

        TaskType taskType = taskTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("任务类型不存在，id: " + id));

        return Result.success(convertToResponse(taskType));
    }

    /**
     * 根据编码查询任务类型
     *
     * @param code 任务类型编码
     * @return 任务类型
     */
    @GetMapping("/code/{code}")
    public Result<TaskTypeResponse> getTaskTypeByCode(@PathVariable String code) {
        log.info("根据编码查询任务类型，code: {}", code);

        TaskType taskType = taskTypeRepository.findByTaskCode(code)
                .orElseThrow(() -> new NotFoundException("任务类型不存在，code: " + code));

        return Result.success(convertToResponse(taskType));
    }

    /**
     * 创建任务类型
     *
     * @param request 任务类型请求
     * @return 创建的任务类型
     */
    @PostMapping
    public Result<TaskTypeResponse> createTaskType(@Valid @RequestBody TaskTypeRequest request) {
        log.info("创建任务类型，taskName: {}, taskCode: {}", request.getTaskName(), request.getTaskCode());

        // 检查任务编码是否已存在
        if (taskTypeRepository.findByTaskCode(request.getTaskCode()).isPresent()) {
            throw new IllegalArgumentException("任务编码已存在：" + request.getTaskCode());
        }

        // 检查首选模型是否存在
        if (!modelConfigRepository.existsById(request.getPreferredModelId())) {
            throw new NotFoundException("首选模型不存在，id: " + request.getPreferredModelId());
        }

        // 构建任务类型实体
        TaskType taskType = TaskType.builder()
                .taskName(request.getTaskName())
                .taskCode(request.getTaskCode())
                .description(request.getDescription())
                .preferredModelId(request.getPreferredModelId())
                .fallbackModelIds(request.getFallbackModelIds())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 保存到数据库
        TaskType savedTaskType = taskTypeRepository.save(taskType);

        log.info("任务类型创建成功，id: {}", savedTaskType.getId());
        return Result.success("任务类型创建成功", convertToResponse(savedTaskType));
    }

    /**
     * 更新任务类型
     *
     * @param id      任务类型 ID
     * @param request 任务类型请求
     * @return 更新后的任务类型
     */
    @PutMapping("/{id}")
    public Result<TaskTypeResponse> updateTaskType(@PathVariable Long id,
                                                     @Valid @RequestBody TaskTypeRequest request) {
        log.info("更新任务类型，id: {}, taskName: {}", id, request.getTaskName());

        // 查询现有任务类型
        TaskType taskType = taskTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("任务类型不存在，id: " + id));

        // 检查任务编码是否与其他任务类型冲突
        taskTypeRepository.findByTaskCode(request.getTaskCode())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new IllegalArgumentException("任务编码已存在：" + request.getTaskCode());
                    }
                });

        // 检查首选模型是否存在
        if (!modelConfigRepository.existsById(request.getPreferredModelId())) {
            throw new NotFoundException("首选模型不存在，id: " + request.getPreferredModelId());
        }

        // 更新字段
        taskType.setTaskName(request.getTaskName());
        taskType.setTaskCode(request.getTaskCode());
        taskType.setDescription(request.getDescription());
        taskType.setPreferredModelId(request.getPreferredModelId());
        taskType.setFallbackModelIds(request.getFallbackModelIds());
        taskType.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        TaskType updatedTaskType = taskTypeRepository.save(taskType);

        log.info("任务类型更新成功，id: {}", id);
        return Result.success("任务类型更新成功", convertToResponse(updatedTaskType));
    }

    /**
     * 删除任务类型
     *
     * @param id 任务类型 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTaskType(@PathVariable Long id) {
        log.info("删除任务类型，id: {}", id);

        // 检查任务类型是否存在
        if (!taskTypeRepository.existsById(id)) {
            throw new NotFoundException("任务类型不存在，id: " + id);
        }

        // 删除任务类型
        taskTypeRepository.deleteById(id);

        log.info("任务类型删除成功，id: {}", id);
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
        String preferredModelName = modelConfigRepository.findById(taskType.getPreferredModelId())
                .map(ModelConfig::getModelName)
                .orElse(null);

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
}
