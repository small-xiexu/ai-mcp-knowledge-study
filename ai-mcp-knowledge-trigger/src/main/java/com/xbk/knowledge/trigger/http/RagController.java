package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.rag.RagGitAnalyzeRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskIdRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskQueryRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskResponse;
import com.xbk.knowledge.application.service.app.RagAppService;
import com.xbk.knowledge.domain.rag.model.entity.RagTask;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
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
public class RagController {

    private final RagAppService ragAppService;

    /**
     * 查询知识库标签列表
     *
     * 为什么：前端需要下拉选择已存在的标签
     * 入参：无
     * 出参：标签列表
     */
    @PostMapping("/tags")
    @SaCheckPermission("agent:read")
    public Result<List<String>> listTags() {
        List<String> tags = ragAppService.listRagTags();
        return Result.success(tags);
    }

    /**
     * 删除知识库标签
     *
     * 为什么：释放不再使用的知识库标签与向量资源
     * 入参：RAG 标签
     * 出参：删除结果
     */
    @PostMapping("/delete")
    @SaCheckPermission("agent:write")
    public Result<Boolean> deleteTag(@RequestParam("ragTag") String ragTag) {
        boolean success = ragAppService.deleteRagTag(ragTag);
        return Result.success(success);
    }

    /**
     * 统计标签向量数量
     *
     * 为什么：展示知识库规模，便于评估检索覆盖度
     * 入参：RAG 标签
     * 出参：向量数量
     */
    @PostMapping("/count")
    @SaCheckPermission("agent:read")
    public Result<Long> countTag(@RequestParam("ragTag") String ragTag) {
        long count = ragAppService.countByRagTag(ragTag);
        return Result.success(count);
    }

    /**
     * 上传知识库文件
     *
     * 为什么：允许同步导入文档，适合小文件或即时入库
     * 入参：RAG 标签 + 文件列表
     * 出参：上传结果
     */
    @PostMapping(value = "/upload", headers = "content-type=multipart/form-data")
    @SaCheckPermission("agent:write")
    public Result<Boolean> uploadFile(@RequestParam("ragTag") String ragTag,
                                      @RequestParam("file") List<MultipartFile> files) {
        boolean success = ragAppService.uploadFiles(ragTag, files);
        return Result.success(success);
    }

    /**
     * 异步上传知识库文件
     *
     * 为什么：大文件或批量导入需要异步任务，避免阻塞接口
     * 入参：RAG 标签 + 文件列表
     * 出参：异步任务 ID
     */
    @PostMapping(value = "/upload/async", headers = "content-type=multipart/form-data")
    @SaCheckPermission("agent:write")
    public Result<String> uploadFileAsync(@RequestParam("ragTag") String ragTag,
                                          @RequestParam("file") List<MultipartFile> files) {
        String taskId = ragAppService.uploadFilesAsync(ragTag, files);
        return Result.success("任务已创建", taskId);
    }

    /**
     * 分析 Git 仓库
     *
     * 为什么：支持直接从仓库构建知识库，减少手动上传
     * 入参：仓库地址、账号、Token、RAG 标签
     * 出参：异步任务 ID
     */
    @PostMapping("/analyze")
    @SaCheckPermission("agent:write")
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
     * 查询任务进度
     *
     * 为什么：异步任务需要轮询进度展示
     * 入参：任务 ID
     * 出参：任务状态详情
     */
    @PostMapping("/task/progress")
    @SaCheckPermission("agent:read")
    public Result<RagTaskResponse> queryTask(@Valid @RequestBody RagTaskIdRequest request) {
        RagTask task = ragAppService.queryTask(request.getTaskId());
        if (task == null) {
            return Result.success(null);
        }
        RagTaskResponse response = toResponse(task);
        return Result.success(response);
    }

    /**
     * 取消任务
     *
     * 为什么：长任务可被终止，节省资源
     * 入参：任务 ID
     * 出参：取消结果
     */
    @PostMapping("/task/cancel")
    @SaCheckPermission("agent:write")
    public Result<Boolean> cancelTask(@Valid @RequestBody RagTaskIdRequest request) {
        boolean success = ragAppService.cancelTask(request.getTaskId());
        return Result.success(success);
    }

    /**
     * 重试任务
     *
     * 为什么：失败任务可基于同一配置重新执行
     * 入参：任务 ID
     * 出参：新任务 ID
     */
    @PostMapping("/task/retry")
    @SaCheckPermission("agent:write")
    public Result<String> retryTask(@Valid @RequestBody RagTaskIdRequest request) {
        String newTaskId = ragAppService.retryTask(request.getTaskId());
        return Result.success("重试任务已创建", newTaskId);
    }

    /**
     * 查询任务列表
     *
     * 为什么：前端展示最近任务与状态，需分页以控制响应大小
     * 入参：分页请求
     * 出参：任务列表分页结果
     */
    @PostMapping("/task/list")
    @SaCheckPermission("agent:read")
    public Result<PageResult<RagTaskResponse>> listTasks(@Valid @RequestBody RagTaskQueryRequest request) {
        request.validate();
        int offset = request.getOffset();
        int pageSize = request.getPageSize();
        PageResult<RagTask> page = ragAppService.queryTaskPage(offset, pageSize);
        PageResult<RagTaskResponse> result = PageResultConverter.convert(page, this::toResponse);
        return Result.success(result);
    }

    private RagTaskResponse toResponse(RagTask task) {
        /*
         * 目的：统一 DTO 映射入口，屏蔽领域实体细节
 */
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
