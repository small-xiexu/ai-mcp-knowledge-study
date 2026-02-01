package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.rag.RagGitAnalyzeRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskIdRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskQueryRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskResponse;
import com.xbk.knowledge.application.service.app.RagAppService;
import com.xbk.knowledge.domain.model.entity.RagTask;
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
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagAppService ragAppService;

    /**
     * 查询知识库标签列表
     */
    @PostMapping("/tags")
    public Result<List<String>> listTags() {
        List<String> tags = ragAppService.listRagTags();
        return Result.success(tags);
    }

    /**
     * 删除知识库标签
     */
    @PostMapping("/delete")
    public Result<Boolean> deleteTag(@RequestParam("ragTag") String ragTag) {
        boolean success = ragAppService.deleteRagTag(ragTag);
        return Result.success(success);
    }

    /**
     * 统计标签向量数量
     */
    @PostMapping("/count")
    public Result<Long> countTag(@RequestParam("ragTag") String ragTag) {
        long count = ragAppService.countByRagTag(ragTag);
        return Result.success(count);
    }

    /**
     * 上传知识库文件
     */
    @PostMapping(value = "/upload", headers = "content-type=multipart/form-data")
    public Result<Boolean> uploadFile(@RequestParam("ragTag") String ragTag,
                                      @RequestParam("file") List<MultipartFile> files) {
        boolean success = ragAppService.uploadFiles(ragTag, files);
        return Result.success(success);
    }

    /**
     * 分析 Git 仓库
     */
    @PostMapping("/analyze")
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
     */
    @PostMapping("/task/progress")
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
     */
    @PostMapping("/task/cancel")
    public Result<Boolean> cancelTask(@Valid @RequestBody RagTaskIdRequest request) {
        boolean success = ragAppService.cancelTask(request.getTaskId());
        return Result.success(success);
    }

    /**
     * 查询任务列表
     */
    @PostMapping("/task/list")
    public Result<PageResult<RagTaskResponse>> listTasks(@Valid @RequestBody RagTaskQueryRequest request) {
        int offset = request.getOffset();
        int pageSize = request.getPageSize();
        PageResult<RagTask> page = ragAppService.queryTaskPage(offset, pageSize);
        PageResult<RagTaskResponse> result = PageResultConverter.convert(page, this::toResponse);
        return Result.success(result);
    }

    private RagTaskResponse toResponse(RagTask task) {
        return RagTaskResponse.builder()
                .taskId(task.getTaskId())
                .type(task.getType())
                .status(task.getStatus())
                .progress(task.getProgress())
                .message(task.getMessage())
                .ragTag(task.getRagTag())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
