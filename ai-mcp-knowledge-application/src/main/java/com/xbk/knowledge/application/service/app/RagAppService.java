package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.RagTask;
import com.xbk.knowledge.types.common.PageResult;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * RAG 应用服务接口
 * 负责知识库管理与任务编排
 *
 * 职责：应用层用例接口，用于封装调用入口
 * @author xiexu
 */
public interface RagAppService {

    /**
     * 查询知识库标签列表
     *
     * @return 标签列表
     */
    List<String> listRagTags();

    /**
     * 删除知识库标签
     *
     * @param ragTag 标签
     * @return 是否成功
     */
    boolean deleteRagTag(String ragTag);

    /**
     * 统计标签向量数量
     *
     * @param ragTag 标签
     * @return 数量
     */
    long countByRagTag(String ragTag);

    /**
     * 上传知识库文件
     *
     * @param ragTag 标签
     * @param files  文件
     * @return 是否成功
     */
    boolean uploadFiles(String ragTag, List<MultipartFile> files);

    /**
     * 提交 Git 仓库分析任务
     *
     * @param repoUrl 仓库地址
     * @param userName 用户名
     * @param token 访问令牌
     * @param ragTag 标签
     * @return 任务ID
     */
    String analyzeGitRepository(String repoUrl, String userName, String token, String ragTag);

    /**
     * 查询任务进度
     *
     * @param taskId 任务ID
     * @return 任务
     */
    RagTask queryTask(String taskId);

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTask(String taskId);

    /**
     * 查询任务列表
     *
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<RagTask> queryTaskPage(int offset, int pageSize);
}
