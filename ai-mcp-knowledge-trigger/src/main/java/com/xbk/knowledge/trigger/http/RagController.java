package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IRagService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.rag.RagGitAnalyzeRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskIdRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskQueryRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskResponse;
import com.xbk.knowledge.application.service.app.RagAppService;
import com.xbk.knowledge.domain.rag.model.entity.RagTask;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageQueryExecutor;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

/**
 * RAG 知识库管理 Controller
 *
 * 职责：HTTP 接口适配，用于转发应用层能力
 * @author sxie
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class RagController implements IRagService {

    /**
     * RAG 应用服务。
     */
    private final RagAppService ragAppService;

    /**
     * 查询知识库标签列表。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Controller 调用 `ragAppService.listRagTags` 查询标签集合。
     * 3. 应用层汇总可用标签并去重返回。
     * 4. Controller 接收结果并封装响应。
     * 5. 统一返回 `Result.success(tags)`。
     * 
     * @return 标签列表
     */
    @PostMapping("/tags")
    @SaCheckPermission("agent:read")
    @Override
    public Result<List<String>> listTags() {
        List<String> tags = ragAppService.listRagTags();
        return Result.success(tags);
    }

    /**
     * 删除知识库标签及其关联向量数据。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 绑定请求参数 `ragTag`。
     * 3. Controller 调用 `ragAppService.deleteRagTag` 执行删除。
     * 4. 应用层清理标签对应文档与向量索引。
     * 5. 返回删除结果布尔值。
     * 
     * @param ragTag RAG 标签。
     * @return 删除结果
     */
    @PostMapping("/delete")
    @SaCheckPermission("agent:write")
    @Override
    public Result<Boolean> deleteTag(@RequestParam("ragTag") String ragTag) {
        boolean success = ragAppService.deleteRagTag(ragTag);
        return Result.success(success);
    }

    /**
     * 统计指定标签下的向量数量。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 绑定请求参数 `ragTag`。
     * 3. Controller 调用 `ragAppService.countByRagTag` 查询数量。
     * 4. 应用层从向量库或索引层返回统计值。
     * 5. 返回数量结果。
     * 
     * @param ragTag RAG 标签。
     * @return 向量数量
     */
    @PostMapping("/count")
    @SaCheckPermission("agent:read")
    @Override
    public Result<Long> countTag(@RequestParam("ragTag") String ragTag) {
        long count = ragAppService.countByRagTag(ragTag);
        return Result.success(count);
    }

    /**
     * 同步上传知识库文件。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 绑定 multipart 参数（`ragTag` + `file[]`）。
     * 3. Controller 调用 `ragAppService.uploadFiles` 执行同步导入。
     * 4. 应用层完成解析、切片、向量化和入库。
     * 5. 返回上传成功布尔值。
     * 
     * @param ragTag RAG 标签。
     * @param files 上传文件列表。
     * @return 上传结果
     */
    @PostMapping(value = "/upload", headers = "content-type=multipart/form-data")
    @SaCheckPermission("agent:write")
    @Override
    public Result<Boolean> uploadFile(@RequestParam("ragTag") String ragTag,
                                      @RequestParam("file") List<MultipartFile> files) {
        boolean success = ragAppService.uploadFiles(ragTag, files);
        return Result.success(success);
    }

    /**
     * 异步上传知识库文件。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 绑定 multipart 参数（`ragTag` + `file[]`）。
     * 3. Controller 调用 `ragAppService.uploadFilesAsync` 创建任务。
     * 4. 应用层写入任务记录并异步执行导入流程。
     * 5. 返回“任务已创建”和任务 ID。
     * 
     * @param ragTag RAG 标签。
     * @param files 上传文件列表。
     * @return 异步任务 ID
     */
    @PostMapping(value = "/upload/async", headers = "content-type=multipart/form-data")
    @SaCheckPermission("agent:write")
    @Override
    public Result<String> uploadFileAsync(@RequestParam("ragTag") String ragTag,
                                          @RequestParam("file") List<MultipartFile> files) {
        String taskId = ragAppService.uploadFilesAsync(ragTag, files);
        return Result.success("任务已创建", taskId);
    }

    /**
     * 分析 Git 仓库并创建知识入库任务。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 提取仓库参数并调用 `analyzeGitRepository`。
     * 4. 应用层拉取仓库、分析文件并提交异步向量化任务。
     * 5. 返回“任务已提交”和任务 ID。
     * 
     * @param request Git 分析请求
     * @return 异步任务 ID
     */
    @PostMapping("/analyze")
    @SaCheckPermission("agent:write")
    @Override
    public Result<String> analyzeGitRepository(@Valid @RequestBody RagGitAnalyzeRequest request) {
        String taskId = ragAppService.analyzeGitRepository(
                request.getRepoUrl(),
                request.getUserName(),
                request.getToken(),
                request.getRagTag()
        );
        return Result.success("任务已提交", taskId);
    }

    /**
     * 查询异步任务进度。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `ragAppService.queryTask` 查询任务实体。
     * 4. 任务存在时转换为 `RagTaskResponse`，不存在返回 null。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request 任务 ID 请求
     * @return 任务状态详情
     */
    @PostMapping("/task/progress")
    @SaCheckPermission("agent:read")
    @Override
    public Result<RagTaskResponse> queryTask(@Valid @RequestBody RagTaskIdRequest request) {
        RagTask task = ragAppService.queryTask(request.getTaskId());
        if (task == null) {
            return Result.success(null);
        }
        RagTaskResponse response = toResponse(task);
        return Result.success(response);
    }

    /**
     * 取消异步任务。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `ragAppService.cancelTask` 发起取消。
     * 4. 应用层更新任务状态并尝试中断执行。
     * 5. 返回取消是否成功。
     * 
     * @param request 任务 ID 请求
     * @return 取消结果
     */
    @PostMapping("/task/cancel")
    @SaCheckPermission("agent:write")
    @Override
    public Result<Boolean> cancelTask(@Valid @RequestBody RagTaskIdRequest request) {
        boolean success = ragAppService.cancelTask(request.getTaskId());
        return Result.success(success);
    }

    /**
     * 重试指定任务。
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `ragAppService.retryTask` 创建重试任务。
     * 4. 应用层复制原任务关键参数并重新排队执行。
     * 5. 返回“重试任务已创建”和新任务 ID。
     * 
     * @param request 任务 ID 请求
     * @return 新任务 ID
     */
    @PostMapping("/task/retry")
    @SaCheckPermission("agent:write")
    @Override
    public Result<String> retryTask(@Valid @RequestBody RagTaskIdRequest request) {
        String newTaskId = ragAppService.retryTask(request.getTaskId());
        return Result.success("重试任务已创建", newTaskId);
    }

    /**
     * 分页查询任务列表。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`），并执行 `request.validate()`。
     * 3. Controller 调用 `ragAppService.queryTaskPage` 查询分页任务。
     * 4. 将 `RagTask` 分页结果转换为 `RagTaskResponse` 分页结构。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request 分页请求
     * @return 任务列表分页结果
     */
    @PostMapping("/task/list")
    @SaCheckPermission("agent:read")
    @Override
    public Result<PageResult<RagTaskResponse>> listTasks(@Valid @RequestBody RagTaskQueryRequest request) {
        return PageQueryExecutor.execute(
                request,
                ragAppService::queryTaskPage,
                this::toResponse
        );
    }

    private RagTaskResponse toResponse(RagTask task) {
        // 统一 DTO 映射入口，屏蔽领域实体细节
        return RagTaskResponse.builder()
                .taskId(task.getTaskId())
                .type(task.getType())
                .status(task.getStatus())
                .progress(task.getProgress())
                .message(task.getMessage())
                .ragTag(task.getRagTag())
                .errorDetails(task.getErrorDetails())
                .retryCount(task.getRetryCount())
                .parentTaskId(task.getParentTaskId())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
