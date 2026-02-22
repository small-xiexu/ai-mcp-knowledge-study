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
     * @return 列表结果
     */
    Result<List<String>> listTags();

    /**
     * 删除知识库标签。
     *
     * @param ragTag 知识库标签
     * @return 处理结果
     */
    Result<Boolean> deleteTag(String ragTag);

    /**
     * 统计标签数据量。
     *
     * @param ragTag 知识库标签
     * @return 统计结果
     */
    Result<Long> countTag(String ragTag);

    /**
     * 同步上传知识库文件。
     *
     * @param ragTag 知识库标签
     * @param files 文件列表
     * @return 处理结果
     */
    Result<Boolean> uploadFile(String ragTag, List<MultipartFile> files);

    /**
     * 异步上传知识库文件。
     *
     * @param ragTag 知识库标签
     * @param files 文件列表
     * @return 任务结果
     */
    Result<String> uploadFileAsync(String ragTag, List<MultipartFile> files);

    /**
     * 分析 Git 仓库并创建任务。
     *
     * @param request 请求参数
     * @return 任务结果
     */
    Result<String> analyzeGitRepository(RagGitAnalyzeRequest request);

    /**
     * 查询任务进度。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<RagTaskResponse> queryTask(RagTaskIdRequest request);

    /**
     * 取消任务。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Boolean> cancelTask(RagTaskIdRequest request);

    /**
     * 重试任务。
     *
     * @param request 请求参数
     * @return 任务结果
     */
    Result<String> retryTask(RagTaskIdRequest request);

    /**
     * 分页查询任务列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<RagTaskResponse>> listTasks(RagTaskQueryRequest request);
}
