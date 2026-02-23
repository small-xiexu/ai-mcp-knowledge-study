package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.rag.RagGitAnalyzeRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskIdRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskQueryRequest;
import com.xbk.knowledge.api.dto.rag.RagTaskResponse;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * RAG 知识库服务接口
 * 定义 RAG 知识库任务与文件处理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IRagService {

    /**
     * 查询知识库标签列表。
     *
     * @return 返回 String 列表数据。
     */
    Result<List<String>> listTags();

    /**
     * 删除知识库标签。
     *
     * @param ragTag 知识库标签
     * @return 返回标签删除结果。
     */
    Result<Boolean> deleteTag(String ragTag);

    /**
     * 统计标签数据量。
     *
     * @param ragTag 知识库标签
     * @return 返回数量统计结果。
     */
    Result<Long> countTag(String ragTag);

    /**
     * 同步上传知识库文件。
     *
     * @param ragTag 知识库标签
     * @param files 文件列表
     * @return 返回文件上传结果。
     */
    Result<Boolean> uploadFile(String ragTag, List<MultipartFile> files);

    /**
     * 异步上传知识库文件。
     *
     * @param ragTag 知识库标签
     * @param files 文件列表
     * @return 返回异步任务 ID。
     */
    Result<String> uploadFileAsync(String ragTag, List<MultipartFile> files);

    /**
     * 分析 Git 仓库并创建任务。
     *
     * @param request Git 仓库分析参数。
     * @return 返回分析任务 ID。
     */
    Result<String> analyzeGitRepository(RagGitAnalyzeRequest request);

    /**
     * 查询任务进度。
     *
     * @param request 任务查询参数。
     * @return 返回 RagTaskResponse 数据。
     */
    Result<RagTaskResponse> queryTask(RagTaskIdRequest request);

    /**
     * 取消任务。
     *
     * @param request 任务取消参数。
     * @return 取消结果
     */
    Result<Boolean> cancelTask(RagTaskIdRequest request);

    /**
     * 重试任务。
     *
     * @param request 任务重试参数。
     * @return 返回重试任务 ID。
     */
    Result<String> retryTask(RagTaskIdRequest request);

    /**
     * 分页查询任务列表。
     *
     * @param request 任务分页查询条件。
     * @return 返回 RagTaskResponse 分页数据。
     */
    Result<PageResult<RagTaskResponse>> listTasks(RagTaskQueryRequest request);
}
